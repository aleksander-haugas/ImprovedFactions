
package io.github.toberocat.improvedfactions.modules.protection.lockdown

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object FactionLockdownViolations : IntIdTable("faction_lockdown_violations") {
    val lockdownId = reference("lockdown_id", FactionLockdowns)
    val violationType = enumeration("violation_type", ViolationType::class)
    val timestamp = datetime("timestamp")
    val details = varchar("details", 255)
}

class FactionLockdownViolation(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<FactionLockdownViolation>(FactionLockdownViolations)

    var lockdown by FactionLockdown referencedOn FactionLockdownViolations.lockdownId
    var violationType by FactionLockdownViolations.violationType
    var timestamp by FactionLockdownViolations.timestamp
    var details by FactionLockdownViolations.details
}

enum class ViolationType {
    OFFENSIVE_ACTION,
    UNDER_SIEGE,
    PVP_ENGAGEMENT
}