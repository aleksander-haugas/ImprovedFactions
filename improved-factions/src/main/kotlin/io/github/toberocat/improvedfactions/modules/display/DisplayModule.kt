package io.github.toberocat.improvedfactions.modules.display

import DisplayModuleListener
import DummyDisplayModuleHandle
import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.modules.base.BaseModule
import io.github.toberocat.improvedfactions.modules.display.config.DisplayModuleConfig
import io.github.toberocat.improvedfactions.modules.display.handles.DisplayModuleHandle
import io.github.toberocat.improvedfactions.modules.display.impl.DisplayModuleHandleImpl
import io.github.toberocat.improvedfactions.modules.protection.ProtectionModule
import org.bukkit.Bukkit

object DisplayModule : BaseModule {
    const val MODULE_NAME = "display"
    override val moduleName = MODULE_NAME
    override var isEnabled = false

    private var displayModuleHandle: DisplayModuleHandle = DummyDisplayModuleHandle()

    override fun onEnable(plugin: ImprovedFactionsPlugin) {
        try {
            isEnabled = true
            val handle = DisplayModuleHandleImpl(plugin, DisplayModuleConfig())
            displayModuleHandle = handle

            // Registrar el listener
            plugin.server.pluginManager.registerEvents(DisplayModuleListener(handle), plugin)

            // Mensaje de debug
            plugin.logger.warning("[DisplayModule] Módulo habilitado correctamente")

            // Actualizar displays para jugadores online después de 1 segundo
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                plugin.logger.warning("[DisplayModule] Actualizando displays para ${Bukkit.getOnlinePlayers().size} jugadores")
                Bukkit.getOnlinePlayers().forEach { player ->
                    try {
                        handle.createBelowNameDisplay(player)
                    } catch (e: Exception) {
                        plugin.logger.severe("[DisplayModule] Error al crear display para ${player.name}: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }, 20L)

        } catch (e: Exception) {
            plugin.logger.severe("[DisplayModule] Error al habilitar el módulo: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadConfig(plugin: ImprovedFactionsPlugin): DisplayModuleConfig {
        val config = plugin.config
        // Implementar la carga de configuración aquí
        return DisplayModuleConfig()
    }

    override fun onLoadDatabase(plugin: ImprovedFactionsPlugin) {
        // No necesitamos base de datos por ahora
    }

    fun displayPair() = DisplayModule.MODULE_NAME to this
}