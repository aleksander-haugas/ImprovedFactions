package io.github.toberocat.improvedfactions.modules.bluemap

import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.gson.MarkerGson
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.bluecolored.bluemap.api.markers.POIMarker
import de.bluecolored.bluemap.api.markers.ShapeMarker
import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.modules.base.BaseModule
import org.bukkit.Bukkit
import java.io.FileReader
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer


class BluemapModule : BaseModule {
    override val moduleName = "bluemap"
    override var isEnabled = false

    private val markerSetId = "improvedfactions_claims"
    private val markerSetLabel = "Factions"
    private val markerMap = ConcurrentHashMap<String, ShapeMarker>()

    override fun shouldEnable(plugin: ImprovedFactionsPlugin): Boolean {
        // Define your module's enabling conditions here.
        // This could be based on configuration, server properties, etc.
        val shouldEnable = super.shouldEnable(plugin)
        if (!shouldEnable) {
            return false
        }

        if (Bukkit.getPluginManager().isPluginEnabled("bluemap")) {
            return true
        }

        warn("Bluemap module is enabled but Bluemap is not installed. Disabling Bluemap module.")
        return false
    }

    // Testing bluemap markers
    override fun onEnable(plugin: ImprovedFactionsPlugin) {
        BlueMapAPI.onEnable(Consumer { api: BlueMapAPI ->
            val marker = POIMarker.builder()
                .label("My Marker")
                .position(20.0, 65.0, -23.0)
                .maxDistance(1000.0)
                .build()

            val markerSet = MarkerSet.builder()
                .label("Factions")
                .build()

            markerSet.markers["my-marker-id"] = marker

            // 🔥 Aquí es donde faltaba: agregar el marker set al mapa
            api.maps.forEach { map ->
                map.markerSets[markerSetId] = markerSet
            }

            plugin.logger.info("BlueMap markers added to all maps.")
        })
    }

    companion object {
        fun bluemapPair() = "bluemap" to BluemapModule()
    }

}