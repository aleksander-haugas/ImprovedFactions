package io.github.toberocat.improvedfactions.modules.display.impl
import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.modules.display.config.DisplayModuleConfig
import io.github.toberocat.improvedfactions.modules.display.handles.DisplayModuleHandle
import io.github.toberocat.improvedfactions.user.factionUser
import org.bukkit.entity.Player
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.attribute.Attribute

class DisplayModuleHandleImpl(
    private val plugin: ImprovedFactionsPlugin,
    private val config: DisplayModuleConfig
) : DisplayModuleHandle {

    private val scoreboard = Bukkit.getScoreboardManager()?.mainScoreboard
        ?: throw IllegalStateException("Failed to get scoreboard")

    override fun createBelowNameDisplay(player: Player) {
        try {
            val faction = player.factionUser().faction()

            // Crear o obtener el objetivo de salud
            var objective = scoreboard.getObjective("heart")
            if (objective == null) {
                objective = scoreboard.registerNewObjective("heart", "health", "❤")
                objective.displaySlot = DisplaySlot.BELOW_NAME
            }

            // Configurar el equipo para el prefijo de facción
            val teamName = "f_${player.uniqueId}"
            val team = scoreboard.getTeam(teamName) ?: scoreboard.registerNewTeam(teamName)

            if (faction != null) {
                team.prefix = "§7[${faction.name}] §r"
            } else {
                team.prefix = ""
            }

            // Añadir el jugador al equipo
            if (!team.hasEntry(player.name)) {
                team.addEntry(player.name)
            }

            // Actualizar el scoreboard del jugador
            player.scoreboard = scoreboard

        } catch (e: Exception) {
            plugin.logger.severe("[DisplayModule] Error al crear display: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun updateBelowNameDisplay(player: Player) {
        try {
            val health = player.health
            val maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
            val healthPercentage = (health / maxHealth) * 100

            // Obtener o crear el objetivo
            var objective = scoreboard.getObjective("heart")
            if (objective == null) {
                objective = scoreboard.registerNewObjective("heart", "health", getHealthDisplay(healthPercentage))
                objective.displaySlot = DisplaySlot.BELOW_NAME
            } else {
                objective.displayName = getHealthDisplay(healthPercentage)
            }

            // Actualizar el equipo si existe
            val teamName = "f_${player.uniqueId}"
            val team = scoreboard.getTeam(teamName)
            if (team != null) {
                val faction = player.factionUser().faction()
                team.prefix = if (faction != null) "§7[${faction.name}] §r" else ""
            }

        } catch (e: Exception) {
            plugin.logger.severe("[DisplayModule] Error al actualizar display: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun getHealthDisplay(percentage: Double): String {
        return when {
            percentage >= 70 -> "§a❤" // Verde para vida alta
            percentage >= 30 -> "§6❤" // Naranja/Dorado para vida media
            else -> "§c❤"            // Rojo para vida baja
        }
    }

    override fun removeBelowNameDisplay(player: Player) {
        try {
            val teamName = "f_${player.uniqueId}"
            val team = scoreboard.getTeam(teamName)
            team?.removeEntry(player.name)
            team?.unregister()
        } catch (e: Exception) {
            plugin.logger.severe("[DisplayModule] Error al remover display: ${e.message}")
        }
    }

    // Implementación de los métodos abstractos faltantes
    override fun formatChat(player: Player, message: String): Component {
        val faction = player.factionUser().faction()
        return Component.text()
            .append(Component.text(if (faction != null) "§7[${faction.name}] " else ""))
            .append(Component.text("${player.name}: §f$message"))
            .build()
    }

    override fun getHoverText(player: Player, viewer: Player): Component {
        return Component.text("Hover text") // Implementación básica
    }

    override fun updateTabList(player: Player) {
        // Implementación básica
    }
}