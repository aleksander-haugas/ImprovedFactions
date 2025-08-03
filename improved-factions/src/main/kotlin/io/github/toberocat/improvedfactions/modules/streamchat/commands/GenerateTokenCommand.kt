package io.github.toberocat.improvedfactions.modules.streamchat.commands

import io.github.toberocat.improvedfactions.modules.streamchat.server.StreamChatServer
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GenerateTokenCommand(private val chatServer: StreamChatServer) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Este comando solo puede ser usado por jugadores")
            return true
        }

        if (!sender.hasPermission("factions.streamchat.token")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando")
            return true
        }

        val token = chatServer.generateUserToken(sender.uniqueId.toString())

        // Crear el mensaje con componentes clickeables
        sender.sendMessage("§a¡Token generado con éxito!")

        // Crear el componente del token
        val tokenComponent = TextComponent("§eToken: §f$token")
        tokenComponent.clickEvent = ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, token)
        tokenComponent.hoverEvent = HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            ComponentBuilder("§7Click para copiar el token").create()
        )

        sender.spigot().sendMessage(tokenComponent)
        sender.sendMessage("""
            §7Este token expirará en 24 horas.
            §7Mantenlo seguro y no lo compartas con nadie.
        """.trimIndent())

        return true
    }
}