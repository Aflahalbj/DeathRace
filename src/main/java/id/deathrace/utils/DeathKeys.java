package id.deathrace.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class DeathKeys {

    public static final Map<String, String> DEATH_DISPLAY = new LinkedHashMap<>();

    static {
        DEATH_DISPLAY.put("CACTUS",             "🌵 Kaktus");
        DEATH_DISPLAY.put("SWEET_BERRY",        "🍓 Sweet Berry");
        DEATH_DISPLAY.put("DROWNING",           "💧 Drowning");
        DEATH_DISPLAY.put("ELYTRA",             "🪂 Elytra");
        DEATH_DISPLAY.put("TNT",                "💣 TNT/Ledakan");
        DEATH_DISPLAY.put("CREEPER",            "💚 Creeper");
        DEATH_DISPLAY.put("BED",                "🛏️ Bed");
        DEATH_DISPLAY.put("FALL",               "🌠 Fall");
        DEATH_DISPLAY.put("ANVIL",              "⚒️ Anvil");
        DEATH_DISPLAY.put("DRIPSTONE",          "🗡️ Dripstone");
        DEATH_DISPLAY.put("LAVA",               "🌋 Lava");
        DEATH_DISPLAY.put("FIRE",               "🔥 Fire");
        DEATH_DISPLAY.put("CAMPFIRE",           "🏕️ Campfire");
        DEATH_DISPLAY.put("FIREWORK",           "🎆 Firework");
        DEATH_DISPLAY.put("LIGHTNING",          "⚡ Lightning");
        DEATH_DISPLAY.put("MAGMA",              "🟥 Magma Block");
        DEATH_DISPLAY.put("MAGIC",              "🧪 Magic/Potion");
        DEATH_DISPLAY.put("FREEZE",             "❄️ Powder Snow");
        DEATH_DISPLAY.put("ENDER_PEARL",        "🔮 Ender Pearl");
        DEATH_DISPLAY.put("FIRE_CHARGE",        "🧨 Fire Charge");
        DEATH_DISPLAY.put("ARROW",              "🏹 Arrow");
        DEATH_DISPLAY.put("TRIDENT",            "🔱 Trident");
        DEATH_DISPLAY.put("HUNGER",             "🍖 Hunger");
        DEATH_DISPLAY.put("SUFFOCATION",        "😤 Suffocated");
        DEATH_DISPLAY.put("WORLD_BORDER",       "🌍 World Border");
        DEATH_DISPLAY.put("THORNS",             "🛡️ Thorns");
        DEATH_DISPLAY.put("VOID",               "🕳️ Void");
        DEATH_DISPLAY.put("END_CRYSTAL",        "💎 End Crystal");
        DEATH_DISPLAY.put("RESPAWN_ANCHOR",     "⚓ Respawn Anchor");
        DEATH_DISPLAY.put("BEE",                "🐝 Bee");
        DEATH_DISPLAY.put("ENDERMAN",           "👽 Enderman");
        DEATH_DISPLAY.put("CAVE_SPIDER",        "🕷️ Cave Spider");
        DEATH_DISPLAY.put("SPIDER",             "🕸️ Spider");
        DEATH_DISPLAY.put("IRON_GOLEM",         "🌹 Iron Golem");
        DEATH_DISPLAY.put("POLAR_BEAR",         "🐻 Polar Bear");
        DEATH_DISPLAY.put("WOLF",               "🐺 Wolf");
        DEATH_DISPLAY.put("PANDA",              "🐼 Panda");
        DEATH_DISPLAY.put("LLAMA",              "🦙 Llama");
        DEATH_DISPLAY.put("TRADER_LLAMA",       "🦙 Trader Llama");
        DEATH_DISPLAY.put("PIGLIN",             "🐷 Piglin");
        DEATH_DISPLAY.put("PUFFERFISH",         "🐡 Pufferfish");
        DEATH_DISPLAY.put("BLAZE",              "🔥 Blaze");
        DEATH_DISPLAY.put("BOGGED",             "🦴 Bogged");
        DEATH_DISPLAY.put("BREEZE",             "🌪️ Breeze");
        DEATH_DISPLAY.put("DROWNED",            "🤽‍♂️ Drowned");

        DEATH_DISPLAY.put("ELDER_GUARDIAN",     "👁️ Elder Guardian");

        DEATH_DISPLAY.put("ENDERMITE",          "🪲 Endermite");
        DEATH_DISPLAY.put("EVOKER",             "📖 Evoker");
        DEATH_DISPLAY.put("GHAST",              "👻 Ghast");
        DEATH_DISPLAY.put("GUARDIAN",           "🐟 Guardian");
        DEATH_DISPLAY.put("HOGLIN",             "🐗 Hoglin");
        DEATH_DISPLAY.put("HUSK",               "🏜️ Husk");
        DEATH_DISPLAY.put("MAGMA_CUBE",         "🟥 Magma Cube");
        DEATH_DISPLAY.put("PARCHED",            "🥵 Parched");

        DEATH_DISPLAY.put("PIGLIN_BRUTE",       "🪓 Piglin Brute");
        DEATH_DISPLAY.put("PILLAGER",           "🏹 Pillager");
        DEATH_DISPLAY.put("RAVAGER",            "🐂 Ravager");

        DEATH_DISPLAY.put("SILVERFISH",         "🐛 Silverfish");
        DEATH_DISPLAY.put("SKELETON",           "💀 Skeleton");
        DEATH_DISPLAY.put("SLIME",              "🟢 Slime");
        DEATH_DISPLAY.put("STRAY",              "❄️ Stray");
        DEATH_DISPLAY.put("VEX",                "👻 Vex");
        DEATH_DISPLAY.put("VINDICATOR",         "🪓 Vindicator");
        DEATH_DISPLAY.put("WARDEN",             "🔊 Warden");
        DEATH_DISPLAY.put("WITCH",              "🧪 Witch");

        DEATH_DISPLAY.put("WITHER_SKELETON",    "☠️ Wither Skeleton");
        DEATH_DISPLAY.put("ZOGLIN",             "🧟🐗 Zoglin");
        DEATH_DISPLAY.put("ZOMBIE",             "🧟 Zombie");
        DEATH_DISPLAY.put("ZOMBIE_VILLAGER",    "🧟 Villager Zombie");
        DEATH_DISPLAY.put("ZOMBIFIED_PIGLIN",   "🐷 Zombified Piglin");
        DEATH_DISPLAY.put("ENDER_DRAGON",       "🐉 Ender Dragon");
        DEATH_DISPLAY.put("SHULKER",            "🐚 Shulker");
        DEATH_DISPLAY.put("PHANTOM",            "👻 Phantom");
        DEATH_DISPLAY.put("WITHER",             "☠️ Wither");
    }

    public static String getDisplay(String key) {
        return DEATH_DISPLAY.getOrDefault(key, key);
    }
}