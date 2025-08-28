package io.github.toberocat.improvedfactions.permissions

import org.bukkit.Material

object Permissions {
    val knownPermissions: MutableMap<String, PermissionHolder> = mutableMapOf()

    // General permissions (Always available)
    val VIEW_POWER = "view-power".registerAsPermission(Material.BEACON)
    val MANAGE_CLAIMS = "manage-claims".registerAsPermission(Material.DIRT)
    val SEND_INVITES = "send-invites".registerAsPermission(Material.BIRCH_SIGN)
    val SET_ICON = "set-icon".registerAsPermission(Material.WHITE_BANNER)
    val RENAME_FACTION = "rename-faction".registerAsPermission(Material.NAME_TAG)
    val MANAGE_PERMISSIONS = "manage-permissions".registerAsPermission(Material.REDSTONE)
    val KICK_PLAYER = "kick-player".registerAsPermission(Material.WOODEN_SWORD)
    val MANAGE_BANS = "manage-bans".registerAsPermission(Material.NETHERITE_AXE)
    val TRANSFER_OWNERSHIP = "transfer-ownership".registerAsPermission(Material.BARRIER)
    val SET_HOME = "set-home".registerAsPermission(Material.RED_BED)
    val HOME = "teleport-home".registerAsPermission(Material.WHITE_BED)
    val SET_JOIN_TYPE = "set-join-type".registerAsPermission(Material.IRON_DOOR)
    val MANAGE_RELATION = "manage-relation".registerAsPermission(Material.IRON_SWORD)
    val PARTICLE_EFFECTS = "particle-effects".registerAsPermission(Material.FIREWORK_ROCKET)

    // Lockdown permissions (If lockdown is installed)
    val FIRE_NOTIFY = "fire-notify".registerAsPermission(Material.SANDSTONE)
    val EXPLOSION_NOTIFY = "explosion-notify".registerAsPermission(Material.TNT)
    val PVP_NOTIFY = "pvp-notify".registerAsPermission(Material.DIAMOND_SWORD)
    val MOB_KILL_NOTIFY = "mob-kill-notify".registerAsPermission(Material.BONE)
    val ANIMAL_KILL_NOTIFY = "animal-kill-notify".registerAsPermission(Material.COOKED_BEEF)
    val IN_ATTACK_NOTIFY = "in-attack-notify".registerAsPermission(Material.GOLDEN_AXE)
    val OUT_ATTACK_NOTIFY = "out-attack-notify".registerAsPermission(Material.IRON_AXE)
    val LOCKDOWN_ACTIVATE = "lockdown-activate".registerAsPermission(Material.OBSIDIAN)
    val LOCKDOWN_AUDIT = "lockdown-audit".registerAsPermission(Material.CRYING_OBSIDIAN)

    // Streamchat permissions (If streamchat is installed)
    val STREAMCHAT_TOKENS = "streamchat-tokens".registerAsPermission(Material.PAPER)

    // Bluemap permissions (If bluemap is installed)
    val BLUEMAP_VIEW_CLAIMS = "bluemap-view-claims".registerAsPermission(Material.MAP)
    val BLUEMAP_HIDE_PLAYER = "bluemap-hide-player".registerAsPermission(Material.MAP)

    // Economy permissions (If economy is installed)
    val ECONOMY_WITHDRAW = "economy-withdraw".registerAsPermission(Material.GOLD_INGOT)
    val ECONOMY_DEPOSIT = "economy-deposit".registerAsPermission(Material.IRON_INGOT)
    val ECONOMY_BALANCE = "economy-balance".registerAsPermission(Material.EMERALD)
    val ECONOMY_PAY = "economy-pay".registerAsPermission(Material.NETHERITE_INGOT)
    val ECONOMY_FACTION_BALANCE = "economy-faction-balance".registerAsPermission(Material.DIAMOND)
    val ECONOMY_FACTION_PAY = "economy-faction-pay".registerAsPermission(Material.DIAMOND_SWORD)
    val ECONOMY_FACTION_DEPOSIT = "economy-faction-deposit".registerAsPermission(Material.DIAMOND_BLOCK)
    val ECONOMY_FACTION_WITHDRAW = "economy-faction-withdraw".registerAsPermission(Material.DIAMOND_ORE)

    // RPG role play permissions (If rpg role play is installed)
    val RPG_CHARACTER_CREATE = "rpg-character-create".registerAsPermission(Material.WRITABLE_BOOK)
    val RPG_CHARACTER_DELETE = "rpg-character-delete".registerAsPermission(Material.BOOK)
    val RPG_CHARACTER_LIST = "rpg-character-list".registerAsPermission(Material.BOOKSHELF)
    val RPG_CHARACTER_SELECT = "rpg-character-select".registerAsPermission(Material.ENCHANTED_BOOK)
    val RPG_CHARACTER_INFO = "rpg-character-info".registerAsPermission(Material.BOOK)
    
    // Character permissions (Requires rpg role play)
    val RPG_CHARACTER_NPC_TALK = "rpg-character-npc-talk".registerAsPermission(Material.VILLAGER_SPAWN_EGG)
    val RPG_CHARACTER_NPC_TRADE = "rpg-character-npc-trade".registerAsPermission(Material.EMERALD)
    val RPG_CHARACTER_NPC_ATTACK = "rpg-character-npc-attack".registerAsPermission(Material.IRON_SWORD)
    val RPG_CHARACTER_NPC_FOLLOW = "rpg-character-npc-follow".registerAsPermission(Material.LEAD)
    val RPG_CHARACTER_NPC_STAND_STILL = "rpg-character-npc-stand-still".registerAsPermission(Material.ARMOR_STAND)
    val RPG_CHARACTER_NPC_MOVE = "rpg-character-npc-move".registerAsPermission(Material.DIAMOND_BOOTS)
    val RPG_CHARACTER_NPC_TELEPORT = "rpg-character-npc-teleport".registerAsPermission(Material.ENDER_PEARL)
    val RPG_CHARACTER_NPC_NAME = "rpg-character-npc-name".registerAsPermission(Material.NAME_TAG)
    val RPG_CHARACTER_NPC_DEFEND = "rpg-character-npc-defend".registerAsPermission(Material.BARRIER)
    val RPG_CHARACTER_NPC_SUMMON = "rpg-character-npc-summon".registerAsPermission(Material.SPAWNER)
    val RPG_CHARACTER_NPC_EQUIP = "rpg-character-npc-equip".registerAsPermission(Material.CHEST)
    val RPG_CHARACTER_NPC_INVENTORY = "rpg-character-npc-inventory".registerAsPermission(Material.CHEST_MINECART)
    val RPG_CHARACTER_NPC_AI = "rpg-character-npc-ai".registerAsPermission(Material.REDSTONE_TORCH)
    val RPG_CHARACTER_NPC_QUEST = "rpg-character-npc-quest".registerAsPermission(Material.WRITABLE_BOOK)

    fun String.registerAsPermission(material: Material): String {
        knownPermissions[this] = PermissionHolder(material)
        return this
    }
}

data class PermissionHolder(val material: Material)