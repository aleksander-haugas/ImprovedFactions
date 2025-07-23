
package io.github.toberocat.improvedfactions.modules.protection.lockdown

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.database.DatabaseManager.loggedTransaction
import io.github.toberocat.improvedfactions.factions.Faction
import io.github.toberocat.improvedfactions.modules.protection.config.ProtectionModuleConfig
import io.github.toberocat.improvedfactions.user.factionUser
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class LockdownBossBar(
    private val plugin: ImprovedFactionsPlugin,
    private val config: ProtectionModuleConfig
) {
    private var bar: BossBar? = null
    private var taskId: Int = -1
    private var startTime: Long = 0
    private val duration: Long = config.lockdownSupervisionPeriod
    private var activeFactionId: String? = null

    private val playerJoinListener = object : Listener {
        @EventHandler
        fun onPlayerJoin(event: PlayerJoinEvent) {
            val bar = bar ?: return
            val factionId = activeFactionId ?: return

            loggedTransaction {
                val playerFaction = event.player.factionUser().faction()
                if (playerFaction?.id?.toString() == factionId) {
                    plugin.adventure.player(event.player).showBossBar(bar)
                }
            }
        }
    }

    init {
        plugin.server.pluginManager.registerEvents(playerJoinListener, plugin)
    }

    fun showBar(faction: Faction) {
        cleanup()

        bar = BossBar.bossBar(
            Component.text("Iniciando auditoría..."),
            0f,
            BossBar.Color.BLUE,
            BossBar.Overlay.PROGRESS
        )

        startTime = System.currentTimeMillis() / 1000
        activeFactionId = faction.id.toString()

        // Mostrar barra a todos los miembros
        loggedTransaction {
            faction.members().forEach { member ->
                member.player()?.let { player ->
                    plugin.adventure.player(player).showBossBar(bar!!)
                }
            }
        }

        // Iniciar actualización
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, {
            updateBar()
        }, 0L, 20L)
    }

    private fun updateBar() {
        val bar = this.bar ?: return
        val currentTime = System.currentTimeMillis() / 1000
        val elapsed = currentTime - startTime
        val remaining = duration - elapsed

        if (remaining <= 0) {
            bar.progress(1f)
            bar.name(Component.text("¡Auditoría completada!", NamedTextColor.GREEN))
            cleanup()
            return
        }

        val progress = elapsed.toFloat() / duration
        bar.progress(progress)

        // Formatear tiempo restante
        val minutes = remaining / 60
        val seconds = remaining % 60
        bar.name(Component.text("Auditoría en curso: ${minutes}m ${seconds}s", NamedTextColor.BLUE))
    }

    fun cleanup() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId)
            taskId = -1
        }

        bar?.let { currentBar ->
            Bukkit.getOnlinePlayers().forEach { player ->
                plugin.adventure.player(player).hideBossBar(currentBar)
            }
            bar = null
        }

        activeFactionId = null
    }
}