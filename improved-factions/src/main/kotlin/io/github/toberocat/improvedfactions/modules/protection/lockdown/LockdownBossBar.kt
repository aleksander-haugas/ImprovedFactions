
package io.github.toberocat.improvedfactions.modules.protection.lockdown

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.database.DatabaseManager.loggedTransaction
import io.github.toberocat.improvedfactions.factions.Faction
import io.github.toberocat.improvedfactions.modules.protection.config.ProtectionModuleConfig
import io.github.toberocat.improvedfactions.user.FactionUser
import io.github.toberocat.improvedfactions.user.factionUser
import kotlinx.datetime.*
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.jetbrains.exposed.sql.and
import java.util.concurrent.ConcurrentHashMap

class LockdownBossBar(
    private val plugin: ImprovedFactionsPlugin,
    private val config: ProtectionModuleConfig
) {
    private val activeBars = ConcurrentHashMap<String, BossBar>()
    private var taskId: Int = -1

    init {
        startUpdateTask()
    }

    private fun startUpdateTask() {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, {
            updateBars()
        }, 0L, 20L) // Actualizar cada segundo
    }

    fun showBar(faction: Faction) {
        loggedTransaction {
            val lockdown = FactionLockdown.find {
                FactionLockdowns.factId eq faction.id.toString()
            }.firstOrNull() ?: return@loggedTransaction

            val bar = createBar(lockdown)
            activeBars[faction.id.toString()] = bar

            // Mostrar la barra a todos los miembros de la facción que estén online
            Bukkit.getOnlinePlayers()
                .filter { player ->
                    player.factionUser().faction()?.id == faction.id
                }
                .forEach { player ->
                    plugin.adventure.player(player).showBossBar(bar)
                }
        }
    }

    private fun createBar(lockdown: FactionLockdown): BossBar {
        return BossBar.bossBar(
            Component.text("Auditoría en curso..."),
            1.0f,
            BossBar.Color.BLUE,
            BossBar.Overlay.PROGRESS
        )
    }

    private fun updateBars() {
        loggedTransaction {
            val iterator = activeBars.iterator()
            while (iterator.hasNext()) {
                val (factionId, bar) = iterator.next()
                val lockdown = FactionLockdown.find {
                    FactionLockdowns.factId eq factionId
                }.firstOrNull()

                if (lockdown == null) {
                    removeBar(factionId)
                    iterator.remove()
                    continue
                }

                updateBarProgress(lockdown, bar)
            }
        }
    }

    private fun updateBarProgress(lockdown: FactionLockdown, bar: BossBar) {
        val now = Clock.System.now()
        val supervisionEnd = lockdown.supervisionStartTime
            .toInstant(TimeZone.currentSystemDefault())
            .plus(config.lockdownSupervisionPeriod, DateTimeUnit.SECOND)

        val totalDuration = config.lockdownSupervisionPeriod.toFloat()
        val remaining = (supervisionEnd - now).inWholeSeconds

        if (remaining <= 0) {
            bar.name(Component.text("¡Auditoría completada!", NamedTextColor.GREEN))
            bar.progress(1.0f)
            // Programar la eliminación de la barra después de 5 segundos
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                removeBar(lockdown.factId)
            }, 100L) // 100 ticks = 5 segundos
            return
        }

        val progress = 1.0f - (remaining / totalDuration)
        bar.progress(progress)

        val minutes = remaining / 60
        val seconds = remaining % 60
        bar.name(Component.text(
            "Auditoría en curso: ${minutes}m ${seconds}s",
            NamedTextColor.BLUE
        ))
    }

    private fun removeBar(factionId: String) {
        val bar = activeBars[factionId] ?: return

        // Obtener la facción y ocultar la barra para todos sus miembros
        loggedTransaction {
            val faction = Faction.findById(factionId.toInt())
            if (faction != null) {
                Bukkit.getOnlinePlayers()
                    .filter { player ->
                        player.factionUser().faction()?.id == faction.id
                    }
                    .forEach { player ->
                        plugin.adventure.player(player).hideBossBar(bar)
                    }
            }
        }

        activeBars.remove(factionId)
    }

    fun cleanup() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId)
        }

        // Limpiar todas las barras activas
        activeBars.keys.forEach { factionId ->
            removeBar(factionId)
        }
        activeBars.clear()
    }
}