package io.github.toberocat.improvedfactions.modules.protection

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.database.DatabaseManager
import io.github.toberocat.improvedfactions.modules.base.BaseModule
import io.github.toberocat.improvedfactions.modules.protection.config.ProtectionModuleConfig
import io.github.toberocat.improvedfactions.modules.protection.listener.ExplosionProtectionListener
import io.github.toberocat.improvedfactions.modules.protection.lockdown.LockdownManager
import io.github.toberocat.improvedfactions.modules.protection.commands.LockdownCommand
import io.github.toberocat.improvedfactions.modules.protection.listener.FireProtectionListener
import io.github.toberocat.improvedfactions.modules.protection.lockdown.FactionLockdownViolations
import io.github.toberocat.improvedfactions.modules.protection.lockdown.FactionLockdowns
import io.github.toberocat.toberocore.command.CommandExecutor

// Empezamos el módulo de proteccion con temporizadores y anti griefing
object ProtectionModule : BaseModule {
    const val MODULE_NAME = "protection"
    override val moduleName = MODULE_NAME
    override var isEnabled = false

    private val config = ProtectionModuleConfig()
    private lateinit var explosionListener: ExplosionProtectionListener
    private lateinit var fireListener: FireProtectionListener
    lateinit var lockdownManager: LockdownManager
        private set

    override fun onLoadDatabase(plugin: ImprovedFactionsPlugin) {
        DatabaseManager.createTables(FactionLockdowns, FactionLockdownViolations)
    }

    override fun onEnable(plugin: ImprovedFactionsPlugin) {
        config.reload(plugin.config)
        lockdownManager = LockdownManager(plugin, config)
        explosionListener = ExplosionProtectionListener(plugin, config)
        fireListener = FireProtectionListener(plugin, config)

        with(plugin.server.pluginManager) {
            registerEvents(explosionListener, plugin)
            registerEvents(fireListener, plugin)
        }
        plugin.logger.info("ProtectionModule enabled.")
    }


    override fun reloadConfig(plugin: ImprovedFactionsPlugin) {
        config.reload(plugin.config)
        plugin.logger.info("ProtectionModule config reloaded.")
    }

    // Agregamos comandos para reclamar la proteccion y hacer la auditoria
    override fun addCommands(plugin: ImprovedFactionsPlugin, executor: CommandExecutor) {
        executor.addChild(LockdownCommand(plugin, lockdownManager))
    }
    fun protectionPair() = MODULE_NAME to this
}