package io.github.toberocat.improvedfactions.modules.bluemap.config

import org.bukkit.configuration.file.FileConfiguration

data class BluemapColorConfig(
    val color: Int,
    val opacity: Double
)

data class BluemapModuleConfig(
    var claimColors: Map<String, BluemapColorConfig> = emptyMap(),
    var allowFactionInfoWindowCustomization: Boolean = false,
    var markerSetId: String = "factions",
    var markerSetDisplayName: String = "Factions",
    var markerSetPriority: Int = 10,
    var markerSetHiddenByDefault: Boolean = false,
    var showZones: Boolean = false,
    var showHomes: Boolean = true,
    var colorFactionClaims: Boolean = true
) {
    private val configPath = "factions.bluemap"

    fun reload(config: FileConfiguration) {
        allowFactionInfoWindowCustomization = config.getBoolean("$configPath.allow-faction-info-window-customization", allowFactionInfoWindowCustomization)
        markerSetId = config.getString("$configPath.marker-set.id") ?: markerSetId
        markerSetDisplayName = config.getString("$configPath.marker-set.display-name") ?: markerSetDisplayName
        markerSetPriority = config.getInt("$configPath.marker-set.layer-priority", markerSetPriority)
        markerSetHiddenByDefault = config.getBoolean("$configPath.marker-set.hidden-by-default", markerSetHiddenByDefault)
        showZones = config.getBoolean("$configPath.show-zones", showZones)
        showHomes = config.getBoolean("$configPath.show-homes", showHomes)
        colorFactionClaims = config.getBoolean("$configPath.color-faction-claims", colorFactionClaims)
        config.getConfigurationSection("$configPath.claim-colors")?.let { section ->
            claimColors = section.getKeys(false).associateWith {
                BluemapColorConfig(
                    section.getString("$it.color")?.toInt(16) ?: 0,
                    section.getDouble("$it.opacity")
                )
            }
        }
    }
}