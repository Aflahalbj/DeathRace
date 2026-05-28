package id.deathrace.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class DeathKeys {

    public static final Map<String, String> DEATH_DISPLAY = new LinkedHashMap<>();

    static {
        DEATH_DISPLAY.put("CACTUS",         "🌵 Kaktus");
        DEATH_DISPLAY.put("SWEET_BERRY",    "🍓 Sweet Berry");
        DEATH_DISPLAY.put("DROWNING",       "💧 Drowned");
        DEATH_DISPLAY.put("ELYTRA",         "🪂 Elytra");
        DEATH_DISPLAY.put("TNT",            "💣 TNT/Ledakan");
        DEATH_DISPLAY.put("CREEPER",        "💚 Creeper");
        DEATH_DISPLAY.put("BED",            "🛏️ Bed");
        DEATH_DISPLAY.put("FALL",           "🌠 Fall");
        DEATH_DISPLAY.put("ANVIL",          "⚒️ Anvil");
        DEATH_DISPLAY.put("DRIPSTONE",      "🗡️ Dripstone");
        DEATH_DISPLAY.put("LAVA",           "🌋 Lava");
        DEATH_DISPLAY.put("FIRE",           "🔥 Fire");
        DEATH_DISPLAY.put("CAMPFIRE",       "🏕️ Campfire");
        DEATH_DISPLAY.put("FIREWORK",       "🎆 Firework");
        DEATH_DISPLAY.put("LIGHTNING",      "⚡ Lightning");
        DEATH_DISPLAY.put("MAGMA",          "🟥 Magma Block");
        DEATH_DISPLAY.put("MAGIC",          "🧪 Magic/Potion");
        DEATH_DISPLAY.put("FREEZE",         "❄️ Powder Snow");
        DEATH_DISPLAY.put("ENDER_PEARL",    "🔮 Ender Pearl");
        DEATH_DISPLAY.put("FIRE_CHARGE",    "🧨 Fire Charge");
        DEATH_DISPLAY.put("ARROW",          "🏹 Arrow");
        DEATH_DISPLAY.put("TRIDENT",        "🔱 Trident");
        DEATH_DISPLAY.put("HUNGER",         "🍖 Hunger");
        DEATH_DISPLAY.put("SUFFOCATION",    "😤 Suffocated");
        DEATH_DISPLAY.put("WORLD_BORDER",   "🌍 World Border");
        DEATH_DISPLAY.put("THORNS",         "🛡️ Thorns");
        DEATH_DISPLAY.put("VOID",           "🕳️ Void");
        DEATH_DISPLAY.put("END_CRYSTAL",    "💎 End Crystal");
        DEATH_DISPLAY.put("RESPAWN_ANCHOR", "⚓ Respawn Anchor");
    }

    public static String getDisplay(String key) {
        return DEATH_DISPLAY.getOrDefault(key, key);
    }
}