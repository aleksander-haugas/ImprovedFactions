
package io.github.toberocat.improvedfactions.modules.protection.lockdown

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.database.DatabaseManager.loggedTransaction
import io.github.toberocat.improvedfactions.factions.Faction
import io.github.toberocat.improvedfactions.modules.protection.config.ProtectionModuleConfig
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.and

class LockdownManager(
    private val plugin: ImprovedFactionsPlugin,
    private val config: ProtectionModuleConfig
) {
    private val bossBar = LockdownBossBar(plugin, config)

    fun startSupervision(faction: Faction) {
        loggedTransaction {
            // Primero limpiar cualquier lockdown existente y sus violaciones
            val existingLockdown = FactionLockdown.find {
                FactionLockdowns.factId eq faction.id.toString()
            }.firstOrNull()

            if (existingLockdown != null) {
                // Eliminar violaciones anteriores
                FactionLockdownViolation.find {
                    FactionLockdownViolations.lockdownId eq existingLockdown.id
                }.forEach { it.delete() }

                // Actualizar el lockdown existente
                existingLockdown.supervisionStartTime = getCurrentDateTime()
                existingLockdown.startTime = getCurrentDateTime()
                existingLockdown.endTime = getCurrentDateTime()
            } else {
                createNewLockdown(faction)
            }

            // Reiniciar el bossbar
            bossBar.cleanup() // Limpiar barra anterior si existe
            bossBar.showBar(faction)
        }
    }

    fun activateLockdown(faction: Faction, duration: Long): Boolean {
        if (!config.lockdownAllowedDurations.contains(duration)) return false

        return loggedTransaction {
            val lockdown = FactionLockdown.find {
                FactionLockdowns.factId eq faction.id.toString()
            }.firstOrNull() ?: return@loggedTransaction false

            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

            // Verificar que el período de supervisión haya terminado y no haya violaciones
            val supervisionEndTime = lockdown.supervisionStartTime
                .toInstant(TimeZone.currentSystemDefault())
                .plus(config.lockdownSupervisionPeriod, DateTimeUnit.SECOND)
                .toLocalDateTime(TimeZone.currentSystemDefault())

            if (now <= supervisionEndTime || hasViolations(faction)) {
                return@loggedTransaction false
            }

            lockdown.startTime = now
            lockdown.endTime = now.toInstant(TimeZone.currentSystemDefault())
                .plus(duration, DateTimeUnit.SECOND)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            true
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

    //
    private fun hasViolations(faction: Faction): Boolean {
        return loggedTransaction {
            val lockdown = FactionLockdown.find {
                FactionLockdowns.factId eq faction.id.toString()
            }.firstOrNull() ?: return@loggedTransaction true

            // Verificar violaciones solo durante el período de supervisión
            val supervisionStart = lockdown.supervisionStartTime
            val supervisionEnd = supervisionStart.toInstant(TimeZone.currentSystemDefault())
                .plus(config.lockdownSupervisionPeriod, DateTimeUnit.SECOND)
                .toLocalDateTime(TimeZone.currentSystemDefault())

            FactionLockdownViolation.find {
                FactionLockdownViolations.lockdownId eq lockdown.id and
                        (FactionLockdownViolations.timestamp greaterEq supervisionStart) and
                        (FactionLockdownViolations.timestamp lessEq supervisionEnd)
            }.any()
        }
    }

    private fun getCurrentDateTime() = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())

    private fun createNewLockdown(faction: Faction) {
        val now = getCurrentDateTime()
        FactionLockdown.new {
            factId = faction.id.toString()
            supervisionStartTime = now
            startTime = now
            endTime = now
        }
    }
}
