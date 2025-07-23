package io.github.toberocat.improvedfactions.modules.protection.handlers

import io.github.toberocat.improvedfactions.modules.protection.config.ProtectionModuleConfig
import io.github.toberocat.improvedfactions.claims.FactionClaim
import io.github.toberocat.improvedfactions.database.DatabaseManager.loggedTransaction
import io.github.toberocat.improvedfactions.modules.protection.lockdown.FactionLockdown
import io.github.toberocat.improvedfactions.modules.protection.lockdown.FactionLockdowns
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bukkit.entity.Creeper
import org.bukkit.entity.Fireball
import org.bukkit.entity.TNTPrimed
import org.bukkit.entity.Wither
import org.bukkit.entity.minecart.ExplosiveMinecart
import org.bukkit.event.entity.EntityExplodeEvent
import org.jetbrains.exposed.sql.and

class ExplosionHandler(private val config: ProtectionModuleConfig) {
    fun shouldCancelExplosion(event: EntityExplodeEvent, claim: FactionClaim?): Boolean {
        // Si no hay claim, permitir la explosión
        if (claim == null) return false

        val faction = claim.faction() ?: return false

        // Verificar si la facción tiene protección ACTIVA (no en supervisión)
        val hasActiveProtection = loggedTransaction {
            FactionLockdown.find {
                (FactionLockdowns.factId eq faction.id.value.toString()) and
                        (FactionLockdowns.startTime less Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())) and
                        (FactionLockdowns.endTime greater Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())) and
                        (FactionLockdowns.startTime greater FactionLockdowns.supervisionStartTime)
            }.any()
        }

        // Si no tiene protección activa, permitir todas las explosiones
        if (!hasActiveProtection) return false

        // Si tiene protección activa, cancelar TNT, Creepers y TNT Minecarts
        return when (event.entity) {
            is TNTPrimed, is Creeper, is ExplosiveMinecart -> true
            else -> false
        }
    }

    fun getExplosionType(event: EntityExplodeEvent): String {
        return when (event.entity) {
            is Creeper -> "Creeper"
            is TNTPrimed -> "TNT"
            is Wither -> "Wither"
            is Fireball -> "Fireball"
            is ExplosiveMinecart -> "Minecart con TNT"
            else -> "Unknown"
        }
    }
}