
package io.github.toberocat.improvedfactions.modules.protection.listener

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.modules.protection.config.ProtectionModuleConfig
import io.github.toberocat.improvedfactions.modules.protection.notification.NotificationManager
import io.github.toberocat.improvedfactions.claims.FactionClaim
import io.github.toberocat.improvedfactions.claims.FactionClaims
import io.github.toberocat.improvedfactions.database.DatabaseManager.loggedTransaction
import io.github.toberocat.improvedfactions.factions.Faction
import io.github.toberocat.improvedfactions.modules.protection.ProtectionModule
import io.github.toberocat.improvedfactions.modules.protection.handlers.ExplosionHandler
import io.github.toberocat.improvedfactions.modules.protection.lockdown.FactionLockdown
import io.github.toberocat.improvedfactions.modules.protection.lockdown.FactionLockdowns
import io.github.toberocat.improvedfactions.modules.protection.lockdown.ViolationType
import io.github.toberocat.improvedfactions.user.factionUser
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bukkit.Chunk
import org.bukkit.entity.Creeper
import org.bukkit.entity.EnderCrystal
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.entity.minecart.ExplosiveMinecart
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExplodeEvent
import org.jetbrains.exposed.sql.and

class ExplosionProtectionListener(
    private val plugin: ImprovedFactionsPlugin,
    config: ProtectionModuleConfig
) : Listener {
    private val explosionHandler = ExplosionHandler(config)

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        // Verificar si es TNT, Creeper, TNT Minecart o Cristal del End
        if (event.entity !is TNTPrimed &&
            event.entity !is Creeper &&
            event.entity !is ExplosiveMinecart &&
            event.entity !is EnderCrystal
        ) return

        val sourcePlayer = if (event.entity is TNTPrimed) {
            val tnt = event.entity as TNTPrimed
            if (tnt.source is Player) tnt.source as Player else null
        } else null

        val sourceFaction = sourcePlayer?.let { getFactionOfPlayer(it) }

        loggedTransaction {
            // Obtener la facción del territorio donde explotó
            val targetClaim = getFactionClaim(event.location.chunk)
            val targetFaction = targetClaim?.faction()

            // Si no hay claims o facciones involucradas, permitir la explosión
            if (targetFaction == null) {
                return@loggedTransaction
            }

            // Sí es TNT dentro de la propia facción, solo verificar protección
            if (event.entity is TNTPrimed && sourceFaction?.id?.value == targetFaction.id.value) {
                handleExplosion(event, targetClaim, targetFaction)
                return@loggedTransaction
            }

            // Para TNT, registrar violaciones si viene de otra facción
            if (event.entity is TNTPrimed && sourceFaction != null && sourceFaction.id.value != targetFaction.id.value) {
                val sourceLockdown = findActiveLockdown(sourceFaction)
                val targetLockdown = findActiveLockdown(targetFaction)

                if (sourceLockdown != null) {
                    handleAttackerViolation(sourceFaction, targetFaction, event)
                }

                if (targetLockdown != null) {
                    handleDefenderViolation(targetFaction, sourceFaction, event)
                }
            }

            // Manejar la explosión (se cancela si hay protección activa)
            handleExplosion(event, targetClaim, targetFaction)
        }
    }

    private fun handleAttackerViolation(sourceFaction: Faction, targetFaction: Faction, event: EntityExplodeEvent) {
        val lockdown = findActiveLockdown(sourceFaction)
        if (lockdown != null) {
            val details = "Ataque a territorio de ${targetFaction.name} en X:${event.location.blockX}, Y:${event.location.blockY}, Z:${event.location.blockZ}"
            ProtectionModule.lockdownManager.registerViolation(
                lockdown,
                ViolationType.OFFENSIVE_ACTION,
                details
            )
            NotificationManager.notifyLockdownViolation(sourceFaction, details)
        }
    }

    private fun handleDefenderViolation(targetFaction: Faction, sourceFaction: Faction, event: EntityExplodeEvent) {
        val lockdown = findActiveLockdown(targetFaction)
        if (lockdown != null) {
            val details = "Territorio atacado por ${sourceFaction.name} en X:${event.location.blockX}, Y:${event.location.blockY}, Z:${event.location.blockZ}"
            ProtectionModule.lockdownManager.registerViolation(
                lockdown,
                ViolationType.UNDER_SIEGE,
                details
            )
            NotificationManager.notifyLockdownViolation(targetFaction, details)
        }
    }

    private fun handleExplosion(event: EntityExplodeEvent, claim: FactionClaim?, faction: Faction?) {
        if (explosionHandler.shouldCancelExplosion(event, claim)) {
            event.blockList().clear()
            if (faction != null) {
                NotificationManager.notifyExplosionBlocked(
                    faction,
                    explosionHandler.getExplosionType(event),
                    event.location
                )
            }
        }
    }

    private fun getFactionOfPlayer(player: Player): Faction? {
        return loggedTransaction {
            player.factionUser().faction()
        }
    }

    private fun findActiveLockdown(faction: Faction): FactionLockdown? {
        return FactionLockdown.find {
            (FactionLockdowns.factId eq faction.id.value.toString()) and
                    (FactionLockdowns.supervisionStartTime lessEq Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault()))
        }.firstOrNull()
    }

    private fun handleLockdownViolation(event: EntityExplodeEvent, faction: Faction?) {
        if (faction == null || event.entity !is TNTPrimed) return

        val lockdown = findActiveLockdown(faction)
        if (lockdown != null) {
            val details = createExplosionDetails(event)
            ProtectionModule.lockdownManager.registerViolation(
                lockdown,
                ViolationType.OFFENSIVE_ACTION,
                details
            )
            NotificationManager.notifyLockdownViolation(faction, details)
        }
    }

    private fun handleCrossClaimExplosion(event: EntityExplodeEvent, sourceFaction: Faction?) {
        if (event.entity !is TNTPrimed) return

        event.blockList().forEach { block ->
            val blockClaim = getFactionClaim(block.chunk)
            val targetFaction = blockClaim?.faction()

            if (targetFaction != null && targetFaction != sourceFaction) {
                val targetLockdown = findActiveLockdown(targetFaction)
                if (targetLockdown != null) {
                    val details = "TNT Explosion affecting territory at X:${block.x}, Y:${block.y}, Z:${block.z}"
                    ProtectionModule.lockdownManager.registerViolation(
                        targetLockdown,
                        ViolationType.OFFENSIVE_ACTION,
                        details
                    )
                }
            }
        }
    }

    private fun createExplosionDetails(event: EntityExplodeEvent): String {
        return StringBuilder().apply {
            append("TNT Explosion ")
            append("at X:${event.location.blockX}, Y:${event.location.blockY}, Z:${event.location.blockZ} ")
            append("in territory")
        }.toString()
    }

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