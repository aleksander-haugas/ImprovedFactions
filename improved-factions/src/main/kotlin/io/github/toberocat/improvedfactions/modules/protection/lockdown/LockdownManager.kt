
package io.github.toberocat.improvedfactions.modules.protection.lockdown

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.database.DatabaseManager.loggedTransaction
import io.github.toberocat.improvedfactions.factions.Faction
import io.github.toberocat.improvedfactions.modules.protection.config.ProtectionModuleConfig
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.and

object FactionLockdowns : IntIdTable("faction_lockdowns") {
    val factId = varchar("faction_id", 36).index()
    val startTime = datetime("start_time")
    val endTime = datetime("end_time")
    val supervisionStartTime = datetime("supervision_start_time")
}

class FactionLockdown(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<FactionLockdown>(FactionLockdowns)

    var factId by FactionLockdowns.factId
    var startTime by FactionLockdowns.startTime
    var endTime by FactionLockdowns.endTime
    var supervisionStartTime by FactionLockdowns.supervisionStartTime
}

object FactionLockdownViolations : IntIdTable("faction_lockdown_violations") {
    val lockdownId = reference("lockdown_id", FactionLockdowns)
    val violationType = enumeration("violation_type", ViolationType::class)
    val timestamp = datetime("timestamp")
    val details = varchar("details", 255)
}

class FactionLockdownViolation(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<FactionLockdownViolation>(FactionLockdownViolations)

    var lockdown by FactionLockdown referencedOn FactionLockdownViolations.lockdownId
    var violationType by FactionLockdownViolations.violationType
    var timestamp by FactionLockdownViolations.timestamp
    var details by FactionLockdownViolations.details
}

enum class ViolationType {
    OFFENSIVE_ACTION,
    UNDER_SIEGE,
    PVP_ENGAGEMENT
}


class LockdownManager(private val plugin: ImprovedFactionsPlugin,
                      private val config: ProtectionModuleConfig
) {
    private val bossBar = LockdownBossBar(plugin, config)

    fun startSupervision(faction: Faction) {
        loggedTransaction {
            // Primero, verificar si ya existe un lockdown activo
            val existingLockdown = FactionLockdown.find {
                FactionLockdowns.factId eq faction.id.toString()
            }.firstOrNull()

            if (existingLockdown != null) {
                // Si existe, actualizamos el tiempo de supervisión
                existingLockdown.supervisionStartTime = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
            } else {
                // Si no existe, creamos uno nuevo
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

                FactionLockdown.new {
                    factId = faction.id.toString()
                    supervisionStartTime = now
                    startTime = now      // Cambiado: iniciamos con el tiempo actual
                    endTime = now        // Cambiado: iniciamos con el tiempo actual
                }
            }

            // Mostrar el BossBar después de iniciar la supervisión
            bossBar.showBar(faction)
        }
    }

    fun canActivateLockdown(faction: Faction): Boolean {
        return loggedTransaction {
            val lockdown = FactionLockdown.find {
                FactionLockdowns.factId eq faction.id.toString()
            }.firstOrNull() ?: return@loggedTransaction false

            val supervisionTime = lockdown.supervisionStartTime
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

            // Calculamos cuando debería terminar el período de supervisión
            val supervisionEndTime = supervisionTime.toInstant(TimeZone.currentSystemDefault())
                .plus(config.lockdownSupervisionPeriod, DateTimeUnit.SECOND)
                .toLocalDateTime(TimeZone.currentSystemDefault())

            // Verificamos que:
            // 1. El período de supervisión haya terminado
            // 2. No haya violaciones registradas
            // 3. El lockdown no esté activo (endTime debe estar en el futuro)
            now > supervisionEndTime &&
                    !hasViolations(faction) &&
                    now > lockdown.endTime // Cambiado: verifica que el lockdown anterior haya terminado
        }
    }

    // Función auxiliar para verificar si una facción está bajo asedio
    private fun isUnderSiege(faction: Faction): Boolean {
        // Implementar verificación de asedio usando ClaimSiegeManager
        return false // TODO: Implementar lógica real
    }

    // Función para registrar una violación
    fun registerViolation(lockdown: FactionLockdown, type: ViolationType, details: String) {
        FactionLockdownViolation.new {
            this.lockdown = lockdown
            this.violationType = type
            this.timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            this.details = details
        }
    }

    fun activateLockdown(faction: Faction, duration: Long): Boolean {
        if (!canActivateLockdown(faction)) return false
        if (!config.lockdownAllowedDurations.contains(duration)) return false

        loggedTransaction {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val lockdown = FactionLockdown.find {
                FactionLockdowns.factId eq faction.id.toString()
            }.firstOrNull() ?: return@loggedTransaction false

            lockdown.startTime = now
            lockdown.endTime = now.toInstant(TimeZone.currentSystemDefault())
                .plus(duration, DateTimeUnit.SECOND)
                .toLocalDateTime(TimeZone.currentSystemDefault())
        }
        return true
    }

    fun onSiegeStart(faction: Faction) {
        loggedTransaction {
            val lockdown = FactionLockdown.find {
                FactionLockdowns.factId eq faction.id.toString() and
                        (FactionLockdowns.endTime greater Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
            }.firstOrNull() ?: return@loggedTransaction

            registerViolation(
                lockdown,
                ViolationType.UNDER_SIEGE,
                "Faction under siege"
            )
        }
    }

    //
    private fun hasViolations(faction: Faction): Boolean {
        return loggedTransaction {
            val lockdown = FactionLockdown.find {
                FactionLockdowns.factId eq faction.id.toString()
            }.firstOrNull() ?: return@loggedTransaction true

            // Verificar si hay asedios activos
            val underSiege = isUnderSiege(faction)
            if (underSiege) {
                registerViolation(lockdown, ViolationType.UNDER_SIEGE, "Faction is under siege")
                return@loggedTransaction true
            }

            // Verificar violaciones registradas durante el período de supervisión
            val supervisionStart = lockdown.supervisionStartTime
            FactionLockdownViolation.find {
                FactionLockdownViolations.lockdownId eq lockdown.id and
                        (FactionLockdownViolations.timestamp greaterEq supervisionStart)
            }.any()
        }
    }
    // Añadir método para limpiar recursos
    fun cleanup() {
        bossBar.cleanup()
    }

}