package com.nosoyyo0717.glyphmatrixwidget

import com.nosoyyo0717.glyphmatrixwidget.designs.minecraft.MINECRAFT_COMPATIBILITY_TAGS
import com.nosoyyo0717.glyphmatrixwidget.designs.minecraft.getMinecraftMatrixDotDesign
import com.nosoyyo0717.glyphmatrixwidget.designs.hollowknight.HOLLOW_KNIGHT_COMPATIBILITY_TAGS
import com.nosoyyo0717.glyphmatrixwidget.designs.hollowknight.getHollowKnightMatrixDotDesign

object GameData {
    val ALL_GAMES = mapOf(
        "Minecraft" to mapOf(
            "Mob Face" to listOf("Creeper", "Skeleton", "Enderman", "Ghastling", "Creaking", "Carved Snow Golem", "Wither"),
            "Food" to listOf("Carrot", "Potato", "Cake", "Bread", "Chorus Fruit", "Cooked Chicken", "Cooked Porkchop", "Cookie", "Golden Apple", "Melon Slice", "Pumpkin Pie"),
            "Armor and Tools" to listOf("Elytra", "Broken Elytra", "Iron Axe", "Iron Pickaxe", "Iron Shovel", "Iron Sword", "Iron Spear", "Fishing Rod", "Carrot on a Stick", "Warped Fungus on a Stick", "Iron Helmet", "Iron Chestplate", "Iron Leggings", "Iron Boots", "Iron Horse Armor", "Iron Nautilus Armor", "Trident", "Mace", "Firework Loaded Crossbow"),
            "Items" to listOf("Wheat Seed", "Sugarcane", "Turtle Egg", "White Candle", "Pale Oak Boat", "Spyglass", "Name Tag", "Book and Quill", "Map", "Water Bucket", "Milk Bucket", "Powder Snow Bucket", "Bucket of Axolotl", "Totem of Undying", "White Bundle", "Ender Pearl", "Eye of Ender", "Wind Charge", "Ominous Bottle", "Potion of Invisibility", "Splash Potion of Invisibility", "Bone", "Diamond", "Emerald", "Nether Star", "Firework Rocket", "Ghast Tear", "Honeycomb", "Iron Ingot", "Phantom Membrane", "Pink Petals", "Snowball", "Wheat"),
            "Music Discs" to listOf("Music Disc Strad", "Music Disc Tears", "Music Disc Lava Chicken"),
            "Blocks" to listOf("Firefly Bush", "Campfire", "Lantern", "White Bed", "Pale Oak Sign", "Oak Door", "Spruce Door", "Birch Door", "Jungle Door", "Acacia Door", "Dark Oak Door", "Mangrove Door", "Cherry Door", "Pale Oak Door", "Bamboo Door", "Crimson Door", "Warped Door", "Iron Door", "Copper Door", "Bell")
        ),
        "Hollow Knight" to mapOf(
            "Game Title" to listOf("Title - 1", "Title - 2"),
            "Hollow Knight" to listOf("The Knight - 1", "The Knight - 2", "The Knight - 3", "The Knight - 4",)
        )
    )

    fun getAllMobNames(): List<String> {
        return ALL_GAMES.values.flatMap { it.values.flatten() }
    }

    fun isMobValid(mobName: String): Boolean {
        return getAllMobNames().contains(mobName)
    }

    fun getMasterTags(mobName: String): List<String> {
        return MINECRAFT_COMPATIBILITY_TAGS[mobName]
            ?: HOLLOW_KNIGHT_COMPATIBILITY_TAGS[mobName]
            ?: listOf("3", "4aPro")
    }

    fun getMasterMatrixDesign(mobName: String, deviceModel: String): IntArray {
        val mcMatrix = getMinecraftMatrixDotDesign(mobName, deviceModel)

        if (!mcMatrix.all { it == 0 }) {
            return mcMatrix
        }

        return getHollowKnightMatrixDotDesign(mobName, deviceModel)
    }
}