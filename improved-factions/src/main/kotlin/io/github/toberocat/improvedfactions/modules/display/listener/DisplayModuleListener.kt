import io.github.toberocat.improvedfactions.modules.display.impl.DisplayModuleHandleImpl
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.entity.Player
import io.github.toberocat.improvedfactions.database.DatabaseManager.loggedTransaction

class DisplayModuleListener(private val handle: DisplayModuleHandleImpl) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        loggedTransaction {
            handle.createBelowNameDisplay(event.player)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        handle.removeBelowNameDisplay(event.player)
    }

    @EventHandler
    fun onPlayerDamage(event: EntityDamageEvent) {
        if (event.entity is Player) {
            loggedTransaction {
                handle.updateBelowNameDisplay(event.entity as Player)
            }
        }
    }

    @EventHandler
    fun onPlayerHeal(event: EntityRegainHealthEvent) {
        if (event.entity is Player) {
            loggedTransaction {
                handle.updateBelowNameDisplay(event.entity as Player)
            }
        }
    }

    @EventHandler
    fun onPotionEffect(event: EntityPotionEffectEvent) {
        if (event.entity is Player) {
            loggedTransaction {
                handle.updateBelowNameDisplay(event.entity as Player)
            }
        }
    }
}