
package io.github.toberocat.improvedfactions.modules.protection.lockdown

import io.github.toberocat.improvedfactions.database.DatabaseManager.loggedTransaction
import io.github.toberocat.improvedfactions.factions.Faction
import io.github.toberocat.improvedfactions.modules.protection.config.ProtectionModuleConfig
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import kotlinx.datetime.*

object FactionLockdowns : IntIdTable("faction_lockdowns") {  // Cambiar el nombre de la tabla
    val factId = varchar("faction_id", 36).index()  // Cambiar de nuevo a faction_id para claridad
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

class LockdownManager(private val config: ProtectionModuleConfig) {
    fun startSupervision(faction: Faction) {
        loggedTransaction {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            FactionLockdown.new {
                factId = faction.id.toString()
                supervisionStartTime = now
                startTime = now
                endTime = now
            }
        }
    }

    fun canActivateLockdown(faction: Faction): Boolean {
        return loggedTransaction {
            val lockdown = FactionLockdown.find {
                FactionLockdowns.factId eq faction.id.toString()
            }.firstOrNull() ?: return@loggedTransaction false

            val supervisionTime = lockdown.supervisionStartTime
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val supervisionEndTime = supervisionTime.toInstant(TimeZone.currentSystemDefault())
                .plus(config.lockdownSupervisionPeriod, DateTimeUnit.SECOND)
                .toLocalDateTime(TimeZone.currentSystemDefault())

            now > supervisionEndTime && !hasViolations(faction)
        }
    }

    private fun hasViolations(faction: Faction): Boolean {
        // TODO: Implementar la lógica para verificar violaciones durante el período de supervisión
        return false
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
}