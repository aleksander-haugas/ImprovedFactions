package io.github.toberocat.improvedfactions.modules

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin.Companion.instance
import io.github.toberocat.improvedfactions.modules.base.BaseModule
import io.github.toberocat.improvedfactions.modules.bluemap.BluemapModule
import io.github.toberocat.improvedfactions.modules.chat.ChatModule
import io.github.toberocat.improvedfactions.modules.claimparticle.ClaimParticleModule
import io.github.toberocat.improvedfactions.modules.display.DisplayModule
import io.github.toberocat.improvedfactions.modules.dynmap.DynmapModule
import io.github.toberocat.improvedfactions.modules.gui.GuiModule
import io.github.toberocat.improvedfactions.modules.home.HomeModule
import io.github.toberocat.improvedfactions.modules.power.PowerRaidsModule
import io.github.toberocat.improvedfactions.modules.protection.ProtectionModule
import io.github.toberocat.improvedfactions.modules.relations.RelationsModule
import io.github.toberocat.improvedfactions.modules.streamchat.StreamChatModule
import io.github.toberocat.improvedfactions.modules.wilderness.WildernessModule
import io.github.toberocat.toberocore.command.CommandExecutor
import org.bukkit.OfflinePlayer

class ModuleManager(private val plugin: ImprovedFactionsPlugin) {
    val activeModules: Map<String, BaseModule>
    val modules = mutableMapOf(
        PowerRaidsModule.powerRaidsPair(),
        DynmapModule.dynmapPair(),
        WildernessModule.wildernessPair(),
        HomeModule.homePair(),
        ChatModule.chatPair(),
        GuiModule.guiPair(),
        ClaimParticleModule.claimParticlesPair(),
        RelationsModule.relationsModulePair(),
        ProtectionModule.protectionPair(),
        DisplayModule.displayPair(),
        StreamChatModule.streamChatPair(),
        BluemapModule.bluemapPair()
    )

    init {
        activeModules = modules.filter { it.value.shouldEnable(instance) }
    }

    fun addModuleCommands(executor: CommandExecutor) {
        activeModules.forEach { (_, module) -> module.addCommands(plugin, executor) }
    }

    fun reloadModuleConfigs() = modules.values.forEach { it.reloadConfig(plugin) }

    fun registerModules() = activeModules.forEach { (name, module) ->
        module.onEnable(plugin)
        module.isEnabled = true
        plugin.logger.info("Loaded module $name")
        module.reloadConfig(plugin)
    }

    fun initializeModuleDatabase() = activeModules.forEach { (_, module) -> module.onLoadDatabase(plugin) }

    fun loadPapiPlaceholders(placeholders: HashMap<String, (player: OfflinePlayer) -> String?>) =
        activeModules.forEach { (_, module) -> module.onPapiPlaceholder(placeholders) }

    inline fun <reified T> getModule(moduleName: String) = modules[moduleName] as? T ?: throw IllegalStateException()
}

