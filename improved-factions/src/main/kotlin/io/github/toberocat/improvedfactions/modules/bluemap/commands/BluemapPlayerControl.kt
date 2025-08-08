package io.github.toberocat.improvedfactions.modules.bluemap.commands

import de.bluecolored.bluemap.api.BlueMapAPI
import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.*

class BluemapPlayerControl(private val plugin: ImprovedFactionsPlugin) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val api = BlueMapAPI.getInstance().orElse(null)
        if (api == null) {
            sender.sendMessage("${ChatColor.RED}BlueMap API no está disponible.")
            return true
        }

        val isPlayer = sender is Player

        when {
            isPlayer && args.isEmpty() -> {
                val player = sender
                val uuid = player.uniqueId
                val visible = api.webApp.getPlayerVisibility(uuid)
                api.webApp.setPlayerVisibility(uuid, !visible)
                player.sendMessage("Ahora eres ${if (!visible) ChatColor.AQUA else ChatColor.GOLD}${if (!visible) "visible" else "invisible"}${ChatColor.RESET} en el mapa.")
                return true
            }

            isPlayer && args.size == 1 -> {
                val player = sender
                val uuid = player.uniqueId
                when (args[0].lowercase(Locale.ROOT)) {
                    "show" -> {
                        api.webApp.setPlayerVisibility(uuid, true)
                        player.sendMessage("Ahora eres ${ChatColor.AQUA}visible${ChatColor.RESET} en el mapa.")
                        return true
                    }
                    "hide" -> {
                        api.webApp.setPlayerVisibility(uuid, false)
                        player.sendMessage("Ahora eres ${ChatColor.GOLD}invisible${ChatColor.RESET} en el mapa.")
                        return true
                    }
                }
            }

            !isPlayer && args.isEmpty() -> {
                sender.sendMessage("${ChatColor.RED}Solo los jugadores pueden usar este comando sin argumentos.")
                return true
            }
        }

        if (!othersAllowed(sender)) {
            sender.sendMessage("${ChatColor.RED}No tienes permiso para cambiar la visibilidad de otros jugadores.")
            return true
        }

        val targetName = args.last()
        val targets = Bukkit.selectEntities(sender, targetName).filterIsInstance<Player>()

        if (targets.isEmpty()) {
            sender.sendMessage("${ChatColor.YELLOW}Jugador \"$targetName\" no encontrado.")
            return true
        }

        val action = if (args.size > 1) args[0].lowercase(Locale.ROOT) else "toggle"
        for (target in targets) {
            val uuid = target.uniqueId
            when (action) {
                "show" -> {
                    api.webApp.setPlayerVisibility(uuid, true)
                    sender.sendMessage("${target.displayName} ahora es ${ChatColor.AQUA}visible${ChatColor.RESET} en el mapa.")
                }
                "hide" -> {
                    api.webApp.setPlayerVisibility(uuid, false)
                    sender.sendMessage("${target.displayName} ahora es ${ChatColor.GOLD}invisible${ChatColor.RESET} en el mapa.")
                }
                "toggle" -> {
                    val visible = api.webApp.getPlayerVisibility(uuid)
                    api.webApp.setPlayerVisibility(uuid, !visible)
                    sender.sendMessage("${target.displayName} ahora es ${if (!visible) ChatColor.AQUA else ChatColor.GOLD}${if (!visible) "visible" else "invisible"}${ChatColor.RESET} en el mapa.")
                }
            }
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.size == 1) {
            completions += listOf("show", "hide")
            if (othersAllowed(sender)) {
                completions += Bukkit.getOnlinePlayers().map { it.name }
            }
        }

        if (othersAllowed(sender) && args.size == 2) {
            completions += Bukkit.getOnlinePlayers().map { it.name }
            completions += listOf("@a", "@p", "@r", "@s")
        }

        return completions
    }

    private fun othersAllowed(sender: CommandSender): Boolean {
        return sender.hasPermission("bluemapplayercontrol.others") || sender.isOp
    }
}
