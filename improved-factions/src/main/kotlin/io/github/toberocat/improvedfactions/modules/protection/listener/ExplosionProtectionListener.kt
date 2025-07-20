package io.github.toberocat.improvedfactions.modules.protection.listener

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.modules.protection.config.ProtectionModuleConfig
import io.github.toberocat.improvedfactions.claims.FactionClaim
import io.github.toberocat.improvedfactions.claims.FactionClaims
import io.github.toberocat.improvedfactions.database.DatabaseManager.loggedTransaction
import org.bukkit.ChatColor
import org.bukkit.Chunk
import org.bukkit.entity.Creeper
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExplodeEvent
import org.jetbrains.exposed.sql.and

class ExplosionProtectionListener(private val plugin: ImprovedFactionsPlugin, private val config: ProtectionModuleConfig) : Listener {

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        loggedTransaction {
            val location = event.location
            val chunk = location.chunk

            // Determinar el tipo de explosión
            val isCreeper = event.entity is Creeper
            val isTNT = event.entity is TNTPrimed

            // Obtener el claim del chunk actual
            val claim = getFactionClaim(chunk)

            // Verificar si está en territorio reclamado o zona de guerra
            val isInClaim = claim?.isClaimed() == true
            val isInWarzone = claim?.zoneType == "warzone"

            // Decidir si se debe cancelar la explosión
            val shouldCancel = when {
                isCreeper && isInClaim && config.preventCreeperGriefingInClaims -> true
                isCreeper && isInWarzone && config.preventCreeperGriefingInWarzone -> true
                isTNT && isInClaim && config.preventTntGriefingInClaims -> true
                isTNT && isInWarzone && config.preventTntGriefingInWarzone -> true
                else -> false
            }

            if (shouldCancel) {
                // Cancelar la explosión
                event.blockList().clear()

                val faction = claim?.faction()
                if (faction != null) {
                    // Crear mensaje para notificación
                    val explosionType = if (isCreeper) "Creeper" else "TNT"
                    val message = StringBuilder().apply {
                        append("${ChatColor.RED}[Explosion Blocked] ")
                        append("${ChatColor.YELLOW}Type: $explosionType ")
                        append("${ChatColor.GRAY}at X: ${location.blockX}, Y: ${location.blockY}, Z: ${location.blockZ} ")
                        append("${ChatColor.GRAY}in ${location.world?.name}")
                    }.toString()

                    // Notificar solo a owners y admins de la facción
                    faction.members().forEach { member ->
                        if (member.uniqueId == faction.owner || member.hasPermission("factions.admin")) {
                            member.player()?.sendMessage(message)
                        }
                    }
                }
            } else {
                // Registrar la explosión permitida
                val explosionType = when (event.entity) {
                    is Creeper -> "Creeper"
                    is TNTPrimed -> "TNT"
                    else -> event.entity.type.name
                }

                val emessage = StringBuilder().apply {
                    append("${ChatColor.RED}[Explosion Allowed] ")
                    append("${ChatColor.YELLOW}Type: $explosionType ")
                    append("${ChatColor.GRAY}at X: ${location.blockX}, Y: ${location.blockY}, Z: ${location.blockZ} ")
                    append("${ChatColor.GRAY}in ${location.world?.name}")
                }.toString()

                // Notificar a jugadores cercanos
                val radius = 50.0
                location.world?.players?.forEach { player ->
                    if (player.location.distance(location) <= radius) {
                        player.sendMessage(emessage)
                    }
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