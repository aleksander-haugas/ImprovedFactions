# Available Methods

Use the `ImprovedFactionsAPI` object to interact with factions:

## ImprovedFactionsAPI accessors

### getFaction(String name)

Used to grab a faction by name if it exists. Might be null

```kotlin
val faction = ImprovedFactionsAPI.getFaction("Knights")
```

### getFaction(int id)

Used to grab a faction by its id if it exists. Might be null

```kotlin
val faction = ImprovedFactionsAPI.getFaction(12)
```

## getFactions()

Used to grab all factions regsitered on this server

```kotlin
val factions = ImprovedFactionsAPI.getFactions()
```

## createFaction(UUID ownerId, String name)

You can create factions programmatically:

```kotlin
ImprovedFactionsAPI.createFaction(player.uniqueId, "MyFaction")
```

This will fire the faction create event