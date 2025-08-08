package io.github.toberocat.improvedfactions.modules.bluemap.impl

import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.markers.HtmlMarker
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.bluecolored.bluemap.api.markers.POIMarker
import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.claims.FactionClaim
import io.github.toberocat.improvedfactions.claims.FactionClaims
import io.github.toberocat.improvedfactions.factions.Faction
import io.github.toberocat.improvedfactions.modules.home.HomeModule.getHome
import io.github.toberocat.improvedfactions.modules.relations.RelationsModule.allies
import io.github.toberocat.improvedfactions.modules.relations.RelationsModule.enemies
import io.github.toberocat.improvedfactions.utils.toOfflinePlayer
import org.bukkit.Location
import org.jetbrains.exposed.sql.transactions.transaction

class BluemapModuleHandleImpl(private val plugin: ImprovedFactionsPlugin) {

    fun registerMarkers() {
        BlueMapAPI.onEnable { api ->
            val factionInfoSet = MarkerSet.builder().label("Factions").build()
            val factionClaimSet = MarkerSet.builder().label("Faction Claims").build()
            val locationSet = MarkerSet.builder().label("Server Locations").build()

            transaction {
                val allFactions = Faction.all().toList()
                val factionRanks = rankFactions(allFactions)

                allFactions.forEach { faction ->
                    // Get home sections
                    val home: Location? = faction.getHome()
                    if (home != null) {
                        val marker = createFactionMarker(faction, home, factionRanks[faction.id.value] ?: "?")
                        factionInfoSet.markers["home-${faction.id.value}"] = marker
                    } else {
                        plugin.logger.info("Facción '${faction.name}' no tiene home. Se omite.")
                    }
                }
            }

            // Register default server markers i remove this later and put in config
            registerStaticLocations(locationSet)

            // Creates a different markers
            api.maps.forEach { map ->
                map.markerSets["factions"] = factionInfoSet             // Display information markers
                map.markerSets["factions-claims"] = factionClaimSet     // Claimed lands markers
                map.markerSets["server-points"] = locationSet           // Server locations POI markers
            }

            plugin.logger.info("BlueMap claim areas and server markers loaded.")

            // List claimed chunks for each faction
            claimedFactionchunks()
        }
    }

    private fun claimedFactionchunks() {
        val factionClaimSet = MarkerSet.builder().label("Faction Claims").build()
        //transaction {
        //    val allFactions = Faction.all().toList()
        //
        //    for (faction in allFactions) {
         //       plugin.logger.info("Facción: ${faction.name}")
        //
         //       if (faction.name.equals("LegionDelNexo", ignoreCase = true)) {
        //            val claims = FactionClaim.find { FactionClaims.factionId eq faction.id.value }.toList()
        //            plugin.logger.info(" - Claims de LegionDelNexo:")
        //            claims.forEach { claim ->
        //                plugin.logger.info("   - Chunk X: ${claim.chunkX}, Z: ${claim.chunkZ}, World: ${claim.world}")
        //            }
        //        }
         //   }
        //}
    }

    private fun rankFactions(factions: List<Faction>): Map<Int, Int> {
        return factions.sortedByDescending { it.accumulatedPower }
            .mapIndexed { index, f -> f.id.value to (index + 1) }
            .toMap()
    }

    // This function adds html home markers and info
    private fun createFactionMarker(faction: Faction, home: Location, rank: Any): HtmlMarker {
        val x = home.x
        val y = home.y
        val z = home.z
        val world = home.world?.name

        val ownerName = faction.owner.toOfflinePlayer().name ?: "Desconocido"
        val membersCount = faction.members().count().toString()
        val claimsCount = FactionClaim.find { FactionClaims.factionId eq faction.id.value }.count()
        val power = faction.accumulatedPower.toString()
        val joinType = faction.factionJoinType.name.lowercase()
        val enemiesf = faction.enemies().count()
        val alliesf = faction.allies().count()

        val html = """
            <div style="text-align:left;padding:8px;border-radius:10px;background:#222;color:#fff;font-family:sans-serif;font-size:12px">
                <h3 style="margin-top:0;text-align:center">${faction.name}</h3>
                <hr style="border:0;height:1px;background:#444">
                <p><strong>Owner:</strong> $ownerName</p>
                <p><strong>Miembros:</strong> $membersCount</p>
                <p><strong>Power:</strong> $power</p>
                <p><strong>Claims:</strong> $claimsCount</p>
                <p><strong>Join Type:</strong> $joinType</p>
                <p><strong>Enemies:</strong> $enemiesf / <strong>Allies:</strong> $alliesf</p>
                <p><strong>Ranking global:</strong> #$rank</p>
            </div>
        """.trimIndent()

        plugin.logger.info("Marcador HTML enriquecido añadido para '${faction.name}' en $x, $y, $z (World: $world)")

        return HtmlMarker.builder()
            .label(faction.name)
            .position(x, y, z)
            .anchor(0, 0)
            .html(html)
            .maxDistance(1000.0)
            .build()
    }

    private fun registerStaticLocations(locationSet: MarkerSet) {
        val locations = listOf(
            "Premium Shops" to Triple(-1414.0, 70.0, 231.0),
            "Daily Rewards" to Triple(-1449.0, 71.0, 187.0),
            "Market Spawn" to Triple(-1474.0, 72.0, 293.0),
            "Flower Shop" to Triple(-1491.0, 72.0, 187.0),
            "Treasure Shop" to Triple(-1507.0, 69.0, 183.0),
            "Farmers Shop" to Triple(-1539.0, 64.0, 182.0),
            "Librarian" to Triple(-1467.0, 76.0, 175.0),
            "Buy & Sell" to Triple(-1469.0, 77.0, 207.0),
            "Arcane Pottery" to Triple(-1472.0, 78.0, 192.0),
            "Weapon Smith" to Triple(-1487.0, 82.0, 197.0),
            "Alchemist" to Triple(-1436.0, 76.0, 273.0),
            "Craftsman" to Triple(-1477.0, 75.0, 277.0),
            "Armourer" to Triple(-1471.0, 75.0, 277.0),
            "Villager recruitment" to Triple(-1598.0, 66.0, 216.0),
            "Villager gathering area" to Triple(-1620.0, 68.0, 194.0),
            "Monthly Rewards" to Triple(-1448.0, 77.0, 173.0),
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
    }
}
