package io.github.toberocat.improvedfactions.modules.protection.listener

import io.github.toberocat.improvedfactions.claims.FactionClaim
import io.github.toberocat.improvedfactions.claims.getFactionClaim
import io.github.toberocat.improvedfactions.database.DatabaseManager.loggedTransaction
import io.github.toberocat.improvedfactions.factions.Faction
import io.github.toberocat.improvedfactions.modules.protection.lockdown.FactionLockdown
import io.github.toberocat.improvedfactions.modules.protection.lockdown.FactionLockdowns
import io.github.toberocat.improvedfactions.modules.protection.lockdown.LockdownManager
import io.github.toberocat.improvedfactions.modules.protection.lockdown.ViolationType
import io.github.toberocat.improvedfactions.user.factionUser
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.jetbrains.exposed.sql.and
import kotlinx.datetime.*
import org.bukkit.ChatColor

class FactionProtectionAuditor(private val lockdownManager: LockdownManager) : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPvP(event: EntityDamageByEntityEvent) {
        // Obtener el atacante (incluso si usa proyectiles)
        val attacker = when (val damager = event.getDamager()) {
            is Player -> damager
            is Projectile -> damager.shooter as? Player
            else -> null
        } ?: return

        // Obtener la víctima si es un jugador
        val victim = event.getEntity() as? Player ?: return

        // Verificar si pertenecen a facciones diferentes
        loggedTransaction {
            val attackerFaction = attacker.factionUser().faction() ?: return@loggedTransaction
            val victimFaction = victim.factionUser().faction() ?: return@loggedTransaction

            if (attackerFaction.id != victimFaction.id) {
                registerPvPViolation(attackerFaction, attacker, victim)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onExplosion(event: EntityExplodeEvent) {
        loggedTransaction {
            // Obtener todos los chunks afectados por la explosión
            val affectedChunks = mutableSetOf<Pair<FactionClaim, Faction>>()

            // Verificar el chunk donde ocurrió la explosión
            val explosionChunk = event.location.chunk
            val explosionClaim = explosionChunk.getFactionClaim()
            if (explosionClaim != null) {
                val faction = Faction.findById(explosionClaim.factionId)
                if (faction != null) {
                    affectedChunks.add(explosionClaim to faction)
                }
            }

            // Verificar los chunks de los bloques afectados
            event.blockList().forEach { block ->
                val blockClaim = block.chunk.getFactionClaim()
                if (blockClaim != null) {
                    val faction = Faction.findById(blockClaim.factionId)
                    if (faction != null) {
                        affectedChunks.add(blockClaim to faction)
                    }
                }
            }

            // Procesar cada facción afectada
            affectedChunks.forEach { (claim, faction) ->
                // Buscar lockdown activo para la facción
                val lockdown = FactionLockdown.find {
                    (FactionLockdowns.factId eq faction.id.value.toString()) and
                            (FactionLockdowns.startTime eq FactionLockdowns.endTime) and
                            (FactionLockdowns.endTime greater Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
                }.firstOrNull()

                if (lockdown != null) {
                    // Registrar la violación
                    val location = event.location
                    val details = StringBuilder().apply {
                        append("Explosion (${event.entityType}) ")
                        append("at X:${location.blockX}, Y:${location.blockY}, Z:${location.blockZ} ")
                        if (claim.factionId == faction.id.value) {
                            append("in own territory")
                        } else {
                            append("affecting territory")
                        }
                    }.toString()

                    lockdownManager.registerViolation(
                        lockdown,
                        ViolationType.OFFENSIVE_ACTION,
                        details
                    )

                    // Notificar a los miembros de la facción
                    faction.members().forEach { member ->
                        member.player()?.sendMessage("${ChatColor.RED}¡Violación de lockdown detectada! $details")
                    }
                }
            }
        }
    }

    private fun registerPvPViolation(attackerFaction: Faction, attacker: Player, victim: Player) {
        val lockdown = FactionLockdown.find {
            FactionLockdowns.factId eq attackerFaction.id.toString() and
                    (FactionLockdowns.endTime greater Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
        }.firstOrNull() ?: return

        lockdownManager.registerViolation(
            lockdown,
            ViolationType.PVP_ENGAGEMENT,
            "PvP: ${attacker.name} attacked ${victim.name}"
        )
    }

    private fun registerOffensiveAction(faction: Faction, details: String) {
        val lockdown = FactionLockdown.find {
            FactionLockdowns.factId eq faction.id.toString() and
                    (FactionLockdowns.endTime greater Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
        }.firstOrNull() ?: return

        lockdownManager.registerViolation(
            lockdown,
            ViolationType.OFFENSIVE_ACTION,
            details
        )
    }
}