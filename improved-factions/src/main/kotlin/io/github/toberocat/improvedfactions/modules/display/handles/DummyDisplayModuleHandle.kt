import io.github.toberocat.improvedfactions.modules.display.handles.DisplayModuleHandle
import org.bukkit.entity.Player
import net.kyori.adventure.text.Component

class DummyDisplayModuleHandle : DisplayModuleHandle {
    override fun createBelowNameDisplay(player: Player) = Unit
    override fun removeBelowNameDisplay(player: Player) = Unit
    override fun updateBelowNameDisplay(player: Player) = Unit
    override fun formatChat(player: Player, message: String): Component = Component.empty()
    override fun getHoverText(player: Player, viewer: Player): Component = Component.empty()
    override fun updateTabList(player: Player) = Unit
}