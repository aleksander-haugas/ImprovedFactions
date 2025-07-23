
package io.github.toberocat.improvedfactions.modules.protection.listener

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.claims.FactionClaim
import io.github.toberocat.improvedfactions.claims.FactionClaims
import io.github.toberocat.improvedfactions.database.DatabaseManager.loggedTransaction
import io.github.toberocat.improvedfactions.modules.protection.config.ProtectionModuleConfig
import io.github.toberocat.improvedfactions.modules.protection.lockdown.FactionLockdown
import io.github.toberocat.improvedfactions.modules.protection.lockdown.FactionLockdowns
import io.github.toberocat.improvedfactions.modules.protection.notification.NotificationManager
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bukkit.Chunk
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.jetbrains.exposed.sql.and

class FireProtectionListener(
    private val plugin: ImprovedFactionsPlugin,
    private val config: ProtectionModuleConfig
) : Listener {
    private val notificationManager = NotificationManager()

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockBurn(event: BlockBurnEvent) {
        val block = event.block
        if (shouldPreventFire(block)) {
            event.isCancelled = true
            notifyFireProtection(block, "quemadura")
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onFireSpread(event: BlockSpreadEvent) {
        if (event.source.type.name != "FIRE") return

        val targetBlock = event.block
        if (shouldPreventFire(targetBlock)) {
            event.isCancelled = true
            notifyFireProtection(targetBlock, "propagación")
        }
    }

    private fun shouldPreventFire(block: Block): Boolean {
        return loggedTransaction {
            val claim = getFactionClaim(block.chunk) ?: return@loggedTransaction false
            val faction = claim.faction() ?: return@loggedTransaction false

            // Verificar si la facción tiene protección activa
            FactionLockdown.find {
                (FactionLockdowns.factId eq faction.id.value.toString()) and
                        (FactionLockdowns.startTime less Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())) and
                        (FactionLockdowns.endTime greater Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())) and
                        (FactionLockdowns.startTime greater FactionLockdowns.supervisionStartTime)
            }.any()
        }
    }

    private fun getFactionClaim(chunk: Chunk): FactionClaim? {
        return loggedTransaction {
            FactionClaim.find {
                (FactionClaims.world eq chunk.world.name) and
                        (FactionClaims.chunkX eq chunk.x) and
                        (FactionClaims.chunkZ eq chunk.z)
            }.firstOrNull()
        }
    }

    private fun notifyFireProtection(block: Block, type: String) {
        loggedTransaction {
            val claim = getFactionClaim(block.chunk) ?: return@loggedTransaction
            val faction = claim.faction() ?: return@loggedTransaction

            notificationManager.notifyFactionMembers(
                faction,
                "Fuego",
                "X:${block.x}, Y:${block.y}, Z:${block.z}",
                "Se previno la $type de fuego"
            )
        }
    }
}