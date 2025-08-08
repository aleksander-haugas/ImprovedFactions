

data class FactionScore(
    val factionName: String,
    var score: Long = 0,
    var members: Int = 0,
    var territory: Int = 0,
    var resources: Map<String, Int> = emptyMap(),
    val achievements: MutableSet<String> = mutableSetOf()
)