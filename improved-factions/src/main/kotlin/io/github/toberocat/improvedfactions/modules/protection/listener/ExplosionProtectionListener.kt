package io.github.toberocat.improvedfactions.modules.protection.listener

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.modules.protection.config.ProtectionModuleConfig
import io.github.toberocat.improvedfactions.claims.FactionClaim
import io.github.toberocat.improvedfactions.claims.FactionClaims
import io.github.toberocat.improvedfactions.database.DatabaseManager.loggedTransaction
import io.github.toberocat.improvedfactions.modules.protection.ProtectionModule
import io.github.toberocat.improvedfactions.modules.protection.lockdown.FactionLockdown
import io.github.toberocat.improvedfactions.modules.protection.lockdown.FactionLockdowns
import io.github.toberocat.improvedfactions.modules.protection.lockdown.ViolationType
import io.github.toberocat.improvedfactions.user.factionUser
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.bukkit.ChatColor
import org.bukkit.Chunk
import org.bukkit.entity.Creeper
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.jetbrains.exposed.sql.and

class ExplosionProtectionListener(private val plugin: ImprovedFactionsPlugin, private val config: ProtectionModuleConfig) : Listener {


    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        loggedTransaction {
            val location = event.location
            val chunk = location.chunk
            val isCreeper = event.entity is Creeper
            val isTNT = event.entity is TNTPrimed

            val claim = getFactionClaim(chunk)
            val faction = claim?.faction()

            if (faction != null) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val lockdown = FactionLockdown.find {
                    FactionLockdowns.factId eq faction.id.value.toString()
                }.firstOrNull()

                if (lockdown != null) {
                    // Verificar si está en período de supervisión
                    val supervisionEnd = lockdown.supervisionStartTime
                        .toInstant(TimeZone.currentSystemDefault())
                        .plus(config.lockdownSupervisionPeriod, DateTimeUnit.SECOND)
                        .toLocalDateTime(TimeZone.currentSystemDefault())

                    if (now <= supervisionEnd && now >= lockdown.supervisionStartTime) {
                        // Solo registrar si está en supervisión y es TNT
                        if (isTNT) {
                            val details = "TNT Explosion in territory at X:${location.blockX}, Y:${location.blockY}, Z:${location.blockZ}"
                            ProtectionModule.lockdownManager.registerViolation(
                                lockdown,
                                ViolationType.OFFENSIVE_ACTION,
                                details
                            )

                            faction.members().forEach { member ->
                                member.player()?.sendMessage("${ChatColor.RED}¡Violación de lockdown detectada! $details")
                            }
                        }
                    }
                }
            }

            // Manejar la cancelación de explosiones
            val isInClaim = claim?.isClaimed() == true
            val isInWarzone = claim?.zoneType == "warzone"

            val shouldCancel = when {
                isCreeper && isInClaim && config.preventCreeperGriefingInClaims -> true
                isCreeper && isInWarzone && config.preventCreeperGriefingInWarzone -> true
                isTNT && isInClaim && config.preventTntGriefingInClaims -> true
                isTNT && isInWarzone && config.preventTntGriefingInWarzone -> true
                else -> false
            }

            if (shouldCancel) {
                event.blockList().clear()
                faction?.let {
                    val explosionType = if (isCreeper) "Creeper" else "TNT"
                    val message = "${ChatColor.RED}[Explosion Blocked] ${ChatColor.YELLOW}Type: $explosionType"
                
                    // Notificar solo a owners y admins
                    it.members().forEach { member ->
                        if (member.uniqueId == it.owner || member.hasPermission("factions.admin")) {
                            member.player()?.sendMessage(message)
                        }
                    }
                }
            }
        }
    }

    fun onPlayerDamageByPlayer(event: EntityDamageByEntityEvent) {
        if (event.entity !is Player || event.damager !is Player) return

        val victim = event.entity as Player
        val attacker = event.damager as Player

        loggedTransaction {
            val victimFaction = victim.factionUser().faction() ?: return@loggedTransaction
            val attackerFaction = attacker.factionUser().faction()

            // Verificar lockdown de la facción víctima
            val lockdown = FactionLockdown.find {
                (FactionLockdowns.factId eq victimFaction.id.value.toString()) and
                        (FactionLockdowns.endTime greater Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault()))
            }.firstOrNull()

            if (lockdown != null) {
                val details = "PvP engagement: ${attacker.name} attacked ${victim.name}"
                ProtectionModule.lockdownManager.registerViolation(
                    lockdown,
                    ViolationType.PVP_ENGAGEMENT,
                    details
                )

                victimFaction.members().forEach { member ->
                    member.player()?.sendMessage("${ChatColor.RED}¡Violación de lockdown detectada! $details")
                }
            }
        }
    }

    /**
     * Finds a faction claim associated with a chunk.
     *
     * @param chunk The chunk to find the claim for
     * @return The faction claim for the chunk if it exists, null otherwise
     */
    private fun getFactionClaim(chunk: Chunk): FactionClaim? {
        return try {
            loggedTransaction {
                FactionClaim.find {
                    (FactionClaims.world eq chunk.world.name) and
                            (FactionClaims.chunkX eq chunk.x) and
                            (FactionClaims.chunkZ eq chunk.z)
                }.firstOrNull()
            }
        } catch (exception: Exception) {
            plugin.logger.warning("""
            Failed to retrieve faction claim for chunk:
            World: ${chunk.world.name}
            X: ${chunk.x}
            Z: ${chunk.z}
            Exception type: ${exception.javaClass.simpleName}
            Error message: ${exception.message}
            Stack trace: ${exception.stackTrace.take(3).joinToString("\n")}
        """.trimIndent())
            null
        }
    }
}