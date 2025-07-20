package io.github.toberocat.improvedfactions.modules.protection

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.modules.base.BaseModule
import io.github.toberocat.improvedfactions.modules.protection.config.ProtectionModuleConfig
import io.github.toberocat.improvedfactions.modules.protection.listener.ExplosionProtectionListener
import io.github.toberocat.toberocore.command.CommandExecutor

object ProtectionModule : BaseModule {

    const val MODULE_NAME = "protection"
    override val moduleName = MODULE_NAME
    override var isEnabled = false // Controlado por el config.yml principal de ImprovedFactions

    private val config = ProtectionModuleConfig() // Se inicializará en onEnable
    private lateinit var explosionListener: ExplosionProtectionListener // Se inicializará en onEnable

    override fun onEnable(plugin: ImprovedFactionsPlugin) {
        // 1. Cargar la configuración del módulo
        config.reload(plugin.config) // Carga las opciones desde el config.yml principal

        // 2. Crear y registrar el listener de eventos
        explosionListener = ExplosionProtectionListener(plugin, config)
        plugin.server.pluginManager.registerEvents(explosionListener, plugin)

        // Marcar el módulo como habilitado
        isEnabled = false
        plugin.logger.info("ProtectionModule enabled.")
    }

    override fun reloadConfig(plugin: ImprovedFactionsPlugin) {
        // Recargar la configuración del módulo cuando el plugin principal recarga su configuración
        config.reload(plugin.config)
        plugin.logger.info("ProtectionModule config reloaded.")
    }

    override fun addCommands(plugin: ImprovedFactionsPlugin, executor: CommandExecutor) {
        // No hay comandos específicos para este módulo de protección en este caso.
        // Si los hubiera, se añadirían aquí.
    }

    fun protectionPair() = MODULE_NAME to this
}