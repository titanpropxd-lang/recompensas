package com.aetheria.rewards.cosmetics;

import com.aetheria.rewards.util.Rarity;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class CosmeticDef {
    public enum Category {
        AURAS("Auras", org.bukkit.Material.BLAZE_POWDER), SOMBREROS("Sombreros", org.bukkit.Material.GOLDEN_HELMET),
        MASCOTAS("Mascotas", org.bukkit.Material.LEAD), ALAS_CAPAS("Alas y Capas", org.bukkit.Material.ELYTRA),
        EFECTOS("Efectos", org.bukkit.Material.FIREWORK_STAR), OTROS("Otros", org.bukkit.Material.AMETHYST_SHARD);
        public final String label; public final org.bukkit.Material icon;
        Category(String l, org.bukkit.Material m) { label = l; icon = m; }
    }
    public static class Spec {
        public String tipo, forma; public int cantidad; public double radio, altura, velocidad, tamano;
        public String color, color2;
    }

    public String id, name, desc, model, color, follow;
    public Category category;
    public String slot;
    public Rarity rarity;
    public boolean premium;
    public boolean published = true;
    public String event;        // null | febrero | adviento
    public int cmd;
    public double ox, oy, oz, yaw, scale;
    public final List<Spec> specs = new ArrayList<>();

    public static CosmeticDef load(String id, ConfigurationSection s) {
        CosmeticDef d = new CosmeticDef();
        d.id = id;
        d.name = s.getString("nombre", id);
        d.desc = s.getString("desc", "");
        try { d.category = Category.valueOf(s.getString("categoria", "OTROS")); } catch (Exception e) { d.category = Category.OTROS; }
        d.slot = s.getString("slot", "AURA").toUpperCase();
        d.rarity = Rarity.parse(s.getString("rareza"), Rarity.COMMON);
        d.premium = "PREMIUM".equalsIgnoreCase(s.getString("tier", "PREMIUM"));
        d.model = s.getString("model");
        d.color = s.getString("color", "#b070ff");
        d.follow = s.getString("sigue", "head");
        d.event = s.getString("evento");
        d.published = s.getBoolean("publicado", true);
        d.cmd = s.getInt("cmd", 0);
        List<Double> off = s.getDoubleList("offset");
        d.ox = off.size() > 0 ? off.get(0) : 0; d.oy = off.size() > 1 ? off.get(1) : 0; d.oz = off.size() > 2 ? off.get(2) : 0;
        d.yaw = s.getDouble("yaw", 0);
        d.scale = s.getDouble("scale", 1.0);
        List<?> ps = s.getList("particulas");
        if (ps != null) for (Object o : ps) {
            if (!(o instanceof java.util.Map<?, ?> raw)) continue;
            @SuppressWarnings("unchecked") java.util.Map<String, Object> m = (java.util.Map<String, Object>) raw;
            Spec sp = new Spec();
            sp.tipo = String.valueOf(m.getOrDefault("tipo", "END_ROD"));
            sp.forma = String.valueOf(m.getOrDefault("forma", "SPIRAL"));
            sp.cantidad = num(m.get("cantidad"), 1); sp.radio = dbl(m.get("radio"), 0.8); sp.altura = dbl(m.get("altura"), 1.8);
            sp.velocidad = dbl(m.get("velocidad"), 0); sp.tamano = dbl(m.get("tamano"), 1.0);
            sp.color = m.get("color") == null ? null : String.valueOf(m.get("color"));
            sp.color2 = m.get("color2") == null ? null : String.valueOf(m.get("color2"));
            d.specs.add(sp);
        }
        return d;
    }
    private static int num(Object o, int def) { return o instanceof Number n ? n.intValue() : def; }
    private static double dbl(Object o, double def) { return o instanceof Number n ? n.doubleValue() : def; }

    public boolean hasModel() { return model != null && !model.isEmpty(); }
}
