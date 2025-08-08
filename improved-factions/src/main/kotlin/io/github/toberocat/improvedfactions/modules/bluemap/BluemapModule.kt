package io.github.toberocat.improvedfactions.modules.bluemap

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.modules.base.BaseModule
import io.github.toberocat.improvedfactions.modules.bluemap.commands.BluemapPlayerControl
import io.github.toberocat.improvedfactions.modules.bluemap.handles.DummyFactionBluemapModuleHandles
import io.github.toberocat.improvedfactions.modules.bluemap.handles.FactionBluemapModuleHandle
import io.github.toberocat.improvedfactions.modules.bluemap.impl.BluemapModuleHandleImpl
import io.github.toberocat.toberocore.command.CommandExecutor
import org.bukkit.Bukkit

class BluemapModule : BaseModule {
    override val moduleName = MODULE_NAME
    override var isEnabled = false

    private var bluemapModuleHandle: FactionBluemapModuleHandle = DummyFactionBluemapModuleHandles()
    //private lateinit var executor: BluemapPlayerControl

    override fun shouldEnable(plugin: ImprovedFactionsPlugin): Boolean {
        if (!super.shouldEnable(plugin)) return false
        if (!Bukkit.getPluginManager().isPluginEnabled("BlueMap")) {
            warn("Bluemap module is enabled but BlueMap is not installed. Disabling Bluemap module.")
            return false
        }
        return true
    }

    override fun onEnable(plugin: ImprovedFactionsPlugin) {

        // Create world markers
        BluemapModuleHandleImpl(plugin).registerMarkers()

        // Creates players markers
        val bmpc = plugin.getCommand("bmpc")
        val executor = BluemapPlayerControl(plugin)

        if (bmpc != null) {
            bmpc.setExecutor(executor)
            bmpc.tabCompleter = executor
        } else {
            plugin.logger.warning("Comando /bmpc no encontrado en plugin.yml.")
        }
    }

    override fun addCommands(plugin: ImprovedFactionsPlugin, executor: CommandExecutor) {
        plugin.getCommand("bmp")?.setExecutor(BluemapPlayerControl(plugin))
    }

    companion object {
        const val MODULE_NAME = "bluemap"
        fun bluemapModule() =
            ImprovedFactionsPlugin.instance.moduleManager.getModule<BluemapModule>(MODULE_NAME)

        fun bluemapPair() = MODULE_NAME to BluemapModule()
    }
}
