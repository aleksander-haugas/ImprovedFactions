package io.github.toberocat.improvedfactions.modules.display.handles
import org.bukkit.entity.Player
import net.kyori.adventure.text.Component

interface DisplayModuleHandle {
    // Below name display
    fun createBelowNameDisplay(player: Player)
    fun removeBelowNameDisplay(player: Player)
    fun updateBelowNameDisplay(player: Player)

    // Chat formatting
    fun formatChat(player: Player, message: String): Component
    fun getHoverText(player: Player, viewer: Player): Component

    // TabList
    fun updateTabList(player: Player)
}