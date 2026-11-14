package com.aetheria.rewards.util;

public enum Rarity {
    COMMON("§7", "Común"), RARE("§9", "Raro"), EPIC("§5", "Épico"), LEGENDARY("§6", "Legendario"), MYTHIC("§d", "Mítico");
    public final String color; public final String label;
    Rarity(String c, String l) { color = c; label = l; }
    public String tag() { return color + "§l" + label.toUpperCase(); }
    public static Rarity parse(String s, Rarity def) {
        if (s == null) return def;
        try { return valueOf(s.toUpperCase()); } catch (IllegalArgumentException e) { return def; }
    }
}
