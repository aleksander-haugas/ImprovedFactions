package io.github.toberocat.improvedfactions.modules.bluemap.handles

import io.github.toberocat.improvedfactions.claims.FactionClaim
import io.github.toberocat.improvedfactions.claims.clustering.cluster.Cluster
import io.github.toberocat.improvedfactions.factions.Faction
import org.bukkit.Location

interface FactionBluemapModuleHandle {
    fun factionHomeChange(faction: Faction, homeLocation: Location)
    fun clusterChange(cluster: Cluster)
    fun clusterRemove(cluster: Cluster)
    fun removeClaim(position: FactionClaim)
}