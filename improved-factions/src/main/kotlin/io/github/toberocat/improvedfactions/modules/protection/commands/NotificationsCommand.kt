package io.github.toberocat.improvedfactions.modules.protection.commands

import io.github.toberocat.improvedfactions.modules.protection.notification.NotificationManager
import io.github.toberocat.improvedfactions.utils.command.CommandCategory
import io.github.toberocat.improvedfactions.utils.command.CommandMeta
import io.github.toberocat.improvedfactions.utils.options.InFactionOption
import io.github.toberocat.toberocore.command.PlayerSubCommand
import io.github.toberocat.toberocore.command.arguments.Argument
import io.github.toberocat.toberocore.command.options.Options
import org.bukkit.ChatColor
import org.bukkit.entity.Player

@CommandMeta(
    category = CommandCategory.MANAGE_CATEGORY,
    description = "protection.commands.notifications.description",
    module = "protection"
)
class NotificationsCommand : PlayerSubCommand("notifications") {

    override fun options(): Options = Options().apply {
        cmdOpt(InFactionOption(true))
    }

    override fun arguments(): Array<Argument<*>> = emptyArray()

    override fun handle(player: Player, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            player.sendMessage("${ChatColor.RED}Uso: /f notifications <fire>")
            return true
        }

        when (args[0].lowercase()) {
            "fire" -> NotificationManager.toggleFireNotifications(player)
            else -> {
                player.sendMessage("${ChatColor.RED}Tipo de notificación desconocido. Opciones disponibles: fire")
            }
        }
        return true
    }
}