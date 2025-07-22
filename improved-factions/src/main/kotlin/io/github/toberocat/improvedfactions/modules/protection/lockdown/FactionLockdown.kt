
package io.github.toberocat.improvedfactions.modules.protection.lockdown

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object FactionLockdowns : IntIdTable("faction_lockdowns") {
    val factId = varchar("faction_id", 36).index()
    val startTime = datetime("start_time")
    val endTime = datetime("end_time")
    val supervisionStartTime = datetime("supervision_start_time")
}

class FactionLockdown(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<FactionLockdown>(FactionLockdowns)

    var factId by FactionLockdowns.factId
    var startTime by FactionLockdowns.startTime
    var endTime by FactionLockdowns.endTime
    var supervisionStartTime by FactionLockdowns.supervisionStartTime
}