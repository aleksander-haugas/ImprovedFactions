# Getting Started with the API

**Improved Factions** provides a lightweight API that can be used by external plugins to interact with its core functionality.

> **Note:** If a specific feature isn’t available through the public API yet, you can still access most internals by retrieving the plugin instance via the server’s plugin manager. Almost all functionality is accessible this way.

The plugin is written in **Kotlin**, and it is recommended to use Kotlin when integrating. However, **Java** is also supported. Be aware that some method or class names may differ slightly due to how Kotlin compiles to Java bytecode. Still, the exposed API objects remain consistent across both languages.

## JitPack Integration

[![](https://jitpack.io/v/ToberoCat/ImprovedFactions.svg)](https://jitpack.io/#ToberoCat/ImprovedFactions)

The plugin is published on [JitPack](https://jitpack.io), making it easy to include in your Gradle project.

Add the following to your `build.gradle.kts`:

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.ToberoCat:ImprovedFactions:<version>")
}
```

Replace `<version>` with the desired release tag or commit hash (e.g., `2.3.0-dev`). You can find available versions on the [JitPack project page](https://jitpack.io/#ToberoCat/ImprovedFactions).

## Working with the underlying api

When you access any faction fucntions not wrapped by the api wrappers, you are almost certainly going to need a transaction for all actiosn that affect database modifications.

Transactions are handled by jetbrains exposed. Here is how one can access them:

```kotlin
val faction = ImprovedFactionsAPI.getFaction("Knights") // Transactions handled by the api wrapper
transaction {
    faction.join(player.uniqueId, 1) // This is a none api wrapped function, joining a player, therefore one needs to provide transaction
}
```

For java usage, one has to check jetbrains exposed documentation

### Further docs

<a href="/ImprovedFactions/api/index.html" target="_blank" rel="noopener noreferrer">Browse full API documentation</a>

### Get help

For support, visit the [Improved Factions Discord](https://discord.com/invite/VmSbFNZejz) or open an issue on [GitHub](https://github.com/ToberoCat/ImprovedFactions).
