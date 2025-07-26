package io.github.toberocat.improvedfactions.modules.display.config
import org.bukkit.configuration.serialization.ConfigurationSerializable

data class DisplayModuleConfig(
    var enabled: Boolean = true,
    var hologram: HologramConfig = HologramConfig(),
    var chat: ChatConfig = ChatConfig(),
    var tablist: TabListConfig = TabListConfig(),
    var effects: EffectsConfig = EffectsConfig()
) : ConfigurationSerializable {
    override fun serialize(): Map<String, Any> {
        return mapOf(
            "enabled" to enabled,
            "hologram" to hologram,
            "chat" to chat,
            "tablist" to tablist,
            "effects" to effects
        )
    }

    // Resto de las clases internas también necesitan implementar ConfigurationSerializable
    data class HologramConfig(
        var enabled: Boolean = true,
        var format: DisplayFormat = DisplayFormat(),
        var updateInterval: Int = 2
    ) : ConfigurationSerializable {
        override fun serialize(): Map<String, Any> {
            return mapOf(
                "enabled" to enabled,
                "format" to format,
                "update-interval" to updateInterval
            )
        }
    }

    data class DisplayFormat(
        var member: List<String> = listOf(
            "[{faction}] {player}",
            "❤ {health}/{maxhealth} - {effects}"
        ),
        var other: List<String> = listOf(
            "[{faction}] {player}",
            "❤ {health}/{maxhealth} - {effects}"
        )
    )


    data class ChatConfig(
        var format: DisplayFormat = DisplayFormat(
            member = listOf("[{faction}] [{rank}] {name}"),
            other = listOf("[{faction}] {name}")
        ),
        var hover: HoverFormat = HoverFormat()
    )


    data class HoverFormat(
        var member: List<String> = listOf(
            "━━━━━━━━━━━",
            "Rango: {rank}",
            "Vida: {health_bar}",
            "Efectos Activos:",
            "{effects_list}",
            "━━━━━━━━━━━"
        ),
        var other: List<String> = listOf(
            "━━━━━━━━━━━",
            "Facción: {faction}",
            "Poder: {power}/{max_power}",
            "━━━━━━━━━━━"
        )
    )


    data class TabListConfig(
        var enabled: Boolean = true,
        var format: DisplayFormat = DisplayFormat(
            member = listOf("[{faction}] [{rank}] {name}"),
            other = listOf("[{faction}] {name}")
        )
    )


    data class EffectsConfig(
        var symbols: Map<String, String> = mapOf(
            "SPEED" to "⚡",
            "STRENGTH" to "💪",
            "FIRE" to "🔥"
        ),
        var healthBar: HealthBarConfig = HealthBarConfig()
    )


    data class HealthBarConfig(
        var symbol: String = "■",
        var length: Int = 10,
        var colors: HealthBarColors = HealthBarColors()
    )


    data class HealthBarColors(
        var full: String = "<#00FF00>",
        var medium: String = "<#FFFF00>",
        var low: String = "<#FF0000>"
    )
}
