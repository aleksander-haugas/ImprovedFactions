package io.github.toberocat.improvedfactions.modules.streamchat

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.modules.base.BaseModule
import io.github.toberocat.improvedfactions.modules.streamchat.commands.GenerateTokenCommand
import io.github.toberocat.improvedfactions.modules.streamchat.server.StreamChatServer
import io.github.toberocat.toberocore.command.CommandExecutor

class StreamChatModule : BaseModule {
    override val moduleName = "streamchat"
    override var isEnabled = false

    private var server: StreamChatServer? = null

    override fun shouldEnable(plugin: ImprovedFactionsPlugin) =
        plugin.config.getBoolean("modules.streamchat", false)

    override fun onEnable(plugin: ImprovedFactionsPlugin) {
        server = StreamChatServer(plugin)
        server?.start()
    }

    override fun addCommands(plugin: ImprovedFactionsPlugin, executor: CommandExecutor) {
        val command = plugin.getCommand("chattoken")
        if (command != null) {
            command.setExecutor(GenerateTokenCommand(server!!))
            plugin.logger.info("Comando chattoken registrado correctamente")
        } else {
            plugin.logger.warning("No se pudo registrar el comando chattoken")
        }
    }

    override fun onLoadDatabase(plugin: ImprovedFactionsPlugin) {
        // Implementación vacía
    }

    override fun onPapiPlaceholder(placeholders: HashMap<String, (org.bukkit.OfflinePlayer) -> String?>) {
        // Implementación vacía
    }

    companion object {
        fun streamChatPair() = "streamchat" to StreamChatModule()
    }
}