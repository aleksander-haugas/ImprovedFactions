package io.github.toberocat.improvedfactions.modules.bluemap

import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.bluecolored.bluemap.api.markers.POIMarker
import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.claims.FactionClaim
import io.github.toberocat.improvedfactions.claims.FactionClaims
import io.github.toberocat.improvedfactions.factions.Faction
import io.github.toberocat.improvedfactions.modules.base.BaseModule
import org.bukkit.Bukkit
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.collections.set
import io.github.toberocat.improvedfactions.factions.Factions

class BluemapModule : BaseModule {
    override val moduleName = "bluemap"
    override var isEnabled = false

    override fun shouldEnable(plugin: ImprovedFactionsPlugin): Boolean {
        if (!super.shouldEnable(plugin)) return false
        if (!Bukkit.getPluginManager().isPluginEnabled("BlueMap")) {
            warn("Bluemap module is enabled but BlueMap is not installed. Disabling Bluemap module.")
            return false
        }
        return true
    }

    override fun onEnable(plugin: ImprovedFactionsPlugin) {
        BlueMapAPI.onEnable { api ->
            val markerSet = MarkerSet.builder().label("Faction Claims").build()
            val locationSet = MarkerSet.builder().label("Server Locations").build()

            // Log de claims por facción
            transaction {
                val targetFactionName = "LegionDelNexo"
                val faction = Faction.find { Factions.name eq targetFactionName }.firstOrNull()

                if (faction == null) {
                    plugin.logger.warning("No se encontró la facción '$targetFactionName'")
                    return@transaction
                }

                val claims = FactionClaim.find { FactionClaims.factionId eq faction.id.value }.toList()

                // Get all claims of the faction
                plugin.logger.info("=== Claims de la facción '${faction.name}' ===")
                claims.forEach { claim ->
                    val x = claim.chunkX
                    val z = claim.chunkZ
                    val world = claim.world
                    plugin.logger.info(" - Chunk X: $x, Z: $z, World: $world")
                }
            }

            api.maps.forEach { map ->
                map.markerSets["factions-claims"] = markerSet
            }

            // Server specific locations
            val locations = listOf(
                "Premium Shops" to Triple(-1414.0, 70.0, 231.0),
                "Daily Rewards" to Triple(-1449.0, 71.0, 187.0),
                "Market Spawn" to Triple(-1474.0, 72.0, 293.0),
                "Flower Shop" to Triple(-1491.0, 72.0, 187.0),
                "Treasure Shop" to Triple(-1507.0, 69.0, 183.0),
                "Farmers Shop" to Triple(-1539.0, 64.0, 182.0),
                "Librarian" to Triple(-1467.0, 76.0, 175.0),
                "Tavern" to Triple(-1494.0, 80.0, 258.0),
                "Magic Academy" to Triple(-1453.0, 93.0, 224.0),
                "Warehouse" to Triple(-1493.0, 66.0, 225.0),
                "Amethyst Deposit" to Triple(-1495.0, 63.0, 164.0),
                "Maritime Port" to Triple(-1561.0, 62.0, 134.0),
                "Chamber of Valor" to Triple(-1663.0, 82.0, -91.0),
                "Farmland Spawn" to Triple(-1621.0, 64.0, 217.0),
                "Nectar Gatherer" to Triple(-1450.0, 77.0, 262.0),
                "Temple Spawn" to Triple(-1669.0, 69.0, -59.0),
                "Factions Jobs" to Triple(-1474.0, 80.0, 311.0),
                "VIP Rewards" to Triple(-1410.0, 76.0, 238.0),
                "Faction Rewards" to Triple(-1362.0, 71.0, 264.0),
                "Nether Portal" to Triple(-1479.0, 71.0, 208.0),
                "Citadel's Park" to Triple(-1405.0, 71.0, 315.0)
            )

            locations.forEachIndexed { i, (label, pos) ->
                val marker = POIMarker.builder()
                    .label(label)
                    .position(pos.first, pos.second, pos.third)
                    .maxDistance(1000.0)
                    .build()
                locationSet.markers["location-$i"] = marker
            }

            api.maps.forEach { map ->
                map.markerSets["factions-claims"] = markerSet
                map.markerSets["server-points"] = locationSet
            }

            plugin.logger.info("BlueMap claim areas and server markers loaded.")
        }
    }

    companion object {
        fun bluemapPair() = "bluemap" to BluemapModule()
    }
}
