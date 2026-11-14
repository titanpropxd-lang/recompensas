package com.aetheria.rewards.cosmetics;

import com.aetheria.rewards.AetheriaRewards;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Dibuja auras, efectos, transformaciones y especiales a partir de las especificaciones de particulas del catalogo. */
public class AuraManager {
    private final AetheriaRewards plugin;
    private final Map<String, Particle> cache = new HashMap<>();

    public AuraManager(AetheriaRewards plugin) { this.plugin = plugin; }

    private Particle resolve(String name) {
        return cache.computeIfAbsent(name, n -> {
            try { return Particle.valueOf(n.toUpperCase()); } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Particula desconocida '" + n + "' en catalog.yml; se usa END_ROD.");
                return Particle.END_ROD;
            }
        });
    }

    private static Color color(String hex, Color def) {
        if (hex == null) return def;
        try { return Color.fromRGB(Integer.parseInt(hex.replace("#", ""), 16)); } catch (Exception e) { return def; }
    }

    private Object data(Particle p, CosmeticDef.Spec s) {
        Class<?> t = p.getDataType();
        if (t == Void.class) return null;
        if (t == Particle.DustOptions.class) return new Particle.DustOptions(color(s.color, Color.PURPLE), (float) Math.max(0.3, s.tamano));
        if (t == Particle.DustTransition.class)
            return new Particle.DustTransition(color(s.color, Color.PURPLE), color(s.color2, Color.FUCHSIA), (float) Math.max(0.3, s.tamano));
        return Void.class; // marcador: tipo no soportado
    }

    private void put(World w, Particle p, Object data, Spec2 s, double x, double y, double z) {
        w.spawnParticle(p, x, y, z, 1, 0, 0, 0, s.speed, data);
    }
    private record Spec2(double speed) {}

    /** Dibuja una especificacion. 'once' = efecto puntual (se pinta con todos sus puntos de una vez). */
    public void draw(Location base, CosmeticDef.Spec s, long t, double yawRad, boolean once) {
        World w = base.getWorld();
        if (w == null) return;
        Particle part = resolve(s.tipo);
        Object d = data(part, s);
        if (d == Void.class) { part = Particle.END_ROD; d = null; }
        Spec2 sp = new Spec2(s.velocidad);
        int n = Math.max(1, s.cantidad);
        double r = s.radio, h = s.altura, bx = base.getX(), by = base.getY(), bz = base.getZ();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        switch (s.forma.toUpperCase()) {
            case "SPIRAL" -> {
                for (int i = 0; i < n; i++) {
                    double a = t * 0.22 + i * (2 * Math.PI / n);
                    double y = ((t * 0.045 + i / (double) n) % 1.0) * h;
                    put(w, part, d, sp, bx + Math.cos(a) * r, by + y, bz + Math.sin(a) * r);
                }
            }
            case "RING" -> {
                for (int i = 0; i < n; i++) {
                    double a = (once ? 0 : t * 0.2) + i * (2 * Math.PI / n);
                    put(w, part, d, sp, bx + Math.cos(a) * r, by + 0.1 + (once ? 0 : h * 0.0), bz + Math.sin(a) * r);
                }
            }
            case "ORBIT" -> {
                for (int i = 0; i < n; i++) {
                    double a = t * 0.18 + i * (2 * Math.PI / n);
                    put(w, part, d, sp, bx + Math.cos(a) * r, by + h * 0.5 + Math.sin(t * 0.1 + i * 2) * 0.35, bz + Math.sin(a) * r);
                }
            }
            case "HALO" -> {
                for (int i = 0; i < n; i++) {
                    double a = t * 0.15 + i * (2 * Math.PI / n);
                    put(w, part, d, sp, bx + Math.cos(a) * r, by + h, bz + Math.sin(a) * r);
                }
            }
            case "RAIN" -> {
                for (int i = 0; i < n; i++) {
                    double a = rnd.nextDouble() * 2 * Math.PI, rr = Math.sqrt(rnd.nextDouble()) * r;
                    put(w, part, d, sp, bx + Math.cos(a) * rr, by + rnd.nextDouble() * h, bz + Math.sin(a) * rr);
                }
            }
            case "COLUMN" -> {
                for (int i = 0; i < n; i++)
                    put(w, part, d, sp, bx + (rnd.nextDouble() - 0.5) * r, by + h * i / n, bz + (rnd.nextDouble() - 0.5) * r);
            }
            case "BURST" -> {
                for (int i = 0; i < n; i++) {
                    double a = rnd.nextDouble() * 2 * Math.PI, b = (rnd.nextDouble() - 0.5) * Math.PI;
                    put(w, part, d, sp, bx + Math.cos(a) * Math.cos(b) * r, by + 1.0 + Math.sin(b) * h * 0.6, bz + Math.sin(a) * Math.cos(b) * r);
                }
            }
            case "TRAIL" -> {
                for (int i = 0; i < n; i++)
                    put(w, part, d, sp, bx + (rnd.nextDouble() - 0.5) * r, by + 0.1 + rnd.nextDouble() * h, bz + (rnd.nextDouble() - 0.5) * r);
            }
            case "WINGS" -> {
                // dos arcos detras del jugador siguiendo su yaw corporal
                double fx = -Math.sin(yawRad), fz = Math.cos(yawRad);          // adelante
                double rx = -Math.cos(yawRad), rz = -Math.sin(yawRad);         // derecha
                for (int side = -1; side <= 1; side += 2) {
                    for (int i = 0; i < 6; i++) {
                        double u = i / 5.0;
                        double lx = side * (0.25 + u * 0.95), ly = 1.55 + Math.sin(u * Math.PI) * 0.45 - u * 0.5, lz = 0.2 + u * 0.15;
                        put(w, part, d, sp, bx + rx * lx - fx * lz, by + ly, bz + rz * lx - fz * lz);
                    }
                }
            }
            default -> put(w, part, d, sp, bx, by + 1, bz);
        }
    }

    public void drawAll(CosmeticDef def, Location base, long t, double yawRad, boolean once) {
        for (CosmeticDef.Spec s : def.specs) draw(base, s, t, yawRad, once);
    }
}
