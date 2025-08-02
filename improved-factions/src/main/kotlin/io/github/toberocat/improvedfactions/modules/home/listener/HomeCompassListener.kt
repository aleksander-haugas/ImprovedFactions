
package io.github.toberocat.improvedfactions.modules.home.listener

import io.github.toberocat.improvedfactions.database.DatabaseManager.loggedTransaction
import io.github.toberocat.improvedfactions.modules.home.HomeModule
import io.github.toberocat.improvedfactions.modules.home.HomeModule.getHome
import io.github.toberocat.improvedfactions.user.factionUser
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.Color

class HomeCompassListener : Listener {
    private val particleShowingPlayers = mutableSetOf<Player>()
    private val homeLocations = mutableMapOf<Player, Location>()
    private val UPDATE_INTERVAL = 1L     // Actualizar cada tick
    private val PARTICLE_SPACING = 0.5   // Espaciado entre partículas principales
    private val ARROW_LENGTH = 4.0       // Longitud total de la línea
    private val ARROW_HEIGHT = 0.8       // Altura a nivel de la cintura

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player

        if (player.inventory.itemInMainHand.type == Material.COMPASS ||
            player.inventory.itemInOffHand.type == Material.COMPASS) {

            if (!particleShowingPlayers.contains(player)) {
                updateHomeLocation(player)
                startShowingParticles(player)
            }
        } else {
            particleShowingPlayers.remove(player)
            homeLocations.remove(player)
        }
    }

    private fun updateHomeLocation(player: Player) {
        loggedTransaction {
            val faction = player.factionUser().faction()
            val home = faction?.getHome()
            if (home != null) {
                homeLocations[player] = home
            }
        }
    }

    private fun startShowingParticles(player: Player) {
        particleShowingPlayers.add(player)

        object : BukkitRunnable() {
            private var updateCounter = 0

            override fun run() {
                if (!particleShowingPlayers.contains(player) ||
                    (player.inventory.itemInMainHand.type != Material.COMPASS &&
                            player.inventory.itemInOffHand.type != Material.COMPASS)) {
                    particleShowingPlayers.remove(player)
                    homeLocations.remove(player)
                    cancel()
                    return
                }

                if (updateCounter++ % 20 == 0) {
                    updateHomeLocation(player)
                }

                val home = homeLocations[player] ?: return
                val direction = home.toVector().subtract(player.location.toVector()).normalize()
                val startPos = player.location.add(0.0, ARROW_HEIGHT, 0.0)

                // Crear la línea de partículas alternando colores
                val particleCount = (ARROW_LENGTH / PARTICLE_SPACING).toInt()
                for (i in 0 until particleCount) {
                    val pos = startPos.clone().add(direction.clone().multiply(i * PARTICLE_SPACING))

                    if (i % 2 == 0) {
                        // Partícula verde brillante
                        player.world.spawnParticle(
                            Particle.REDSTONE,
                            pos,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.0f)
                        )
                    } else {
                        // Partícula verde más oscura
                        player.world.spawnParticle(
                            Particle.REDSTONE,
                            pos,
                            1,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            Particle.DustOptions(Color.fromRGB(0, 180, 0), 1.0f)
                        )
                    }
                }
            }
        }.runTaskTimer(HomeModule.plugin, 0L, UPDATE_INTERVAL)
    }
}