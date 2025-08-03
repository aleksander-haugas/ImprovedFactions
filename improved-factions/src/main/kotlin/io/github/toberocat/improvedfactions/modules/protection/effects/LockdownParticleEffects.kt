package io.github.toberocat.improvedfactions.modules.protection.effects

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.sin

class LockdownParticleEffects(private val plugin: ImprovedFactionsPlugin) {

    fun playSupervisionStartEffect(player: Player) {
        object : BukkitRunnable() {
            private var tick = 0
            private val maxTicks = 40 // 2 segundos a 20 ticks por segundo

            override fun run() {
                if (tick >= maxTicks) {
                    cancel()
                    return
                }

                // Crear un efecto de espiral ascendente
                val location = player.location.add(0.0, 1.0, 0.0)
                val radius = 1.0
                val y = tick * 0.1 // Altura ascendente

                for (angle in 0..360 step 30) {
                    val radian = Math.toRadians(angle.toDouble())
                    val x = radius * cos(radian)
                    val z = radius * sin(radian)

                    location.add(x, y, z)
                    player.world.spawnParticle(
                        Particle.SPELL_WITCH,
                        location,
                        1,
                        0.0, 0.0, 0.0,
                        0.0
                    )
                    location.subtract(x, y, z)
                }

                tick++
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }

    fun playLockdownActivateEffect(player: Player) {
        object : BukkitRunnable() {
            private var tick = 0
            private val maxTicks = 60 // 3 segundos

            override fun run() {
                if (tick >= maxTicks) {
                    cancel()
                    return
                }

                // Crear un domo de protección
                val location = player.location
                val radius = 3.0
                val particlesPerCircle = 20

                for (i in 0..particlesPerCircle) {
                    val angle = (i.toDouble() / particlesPerCircle) * Math.PI * 2
                    val height = sin((tick.toDouble() / maxTicks) * Math.PI) * radius

                    val x = cos(angle) * radius
                    val z = sin(angle) * radius

                    val particleLocation = location.clone().add(x, height, z)

                    // Crear partícula de redstone con color personalizado
                    val dustOptions = Particle.DustOptions(Color.fromRGB(75, 0, 130), 1.0f)
                    player.world.spawnParticle(
                        Particle.REDSTONE,
                        particleLocation,
                        1,
                        0.0, 0.0, 0.0,
                        0.0,
                        dustOptions
                    )
                }

                tick++
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }

    fun playProtectionActiveEffect(location: Location) {
        // Efecto sutil que se muestra en las esquinas del chunk cuando la protección está activa
        val chunk = location.chunk
        val corners = getChunkCorners(chunk)

        corners.forEach { corner ->
            corner.world?.spawnParticle(
                Particle.END_ROD,
                corner,
                1,
                0.0, 0.3, 0.0,
                0.0
            )
        }
    }

    private fun getChunkCorners(chunk: org.bukkit.Chunk): List<Location> {
        val world = chunk.world
        val x = chunk.x * 16
        val z = chunk.z * 16
        return listOf(
            Location(world, x.toDouble(), world.getHighestBlockYAt(x, z).toDouble(), z.toDouble()),
            Location(world, (x + 15).toDouble(), world.getHighestBlockYAt(x + 15, z).toDouble(), z.toDouble()),
            Location(world, x.toDouble(), world.getHighestBlockYAt(x, z + 15).toDouble(), (z + 15).toDouble()),
            Location(world, (x + 15).toDouble(), world.getHighestBlockYAt(x + 15, z + 15).toDouble(), (z + 15).toDouble())
        )
    }
}