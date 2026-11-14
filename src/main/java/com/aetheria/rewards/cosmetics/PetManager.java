package com.aetheria.rewards.cosmetics;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.model.ModelHandle;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Mascotas: modelo de BetterModel que sigue con suavizado (sin entidades reales, sin IA, sin pathfinding). */
public class PetManager {
    private final AetheriaRewards plugin;
    private static final class State { double x, y, z, yaw; boolean init; }
    private final Map<UUID, State> states = new HashMap<>();

    public PetManager(AetheriaRewards plugin) { this.plugin = plugin; }

    /** Devuelve la ubicacion actual de la mascota (suavizada). */
    public Location follow(Player p, CosmeticDef def, long tick) {
        State s = states.computeIfAbsent(p.getUniqueId(), k -> new State());
        Location pl = p.getLocation();
        double yawRad = Math.toRadians(pl.getYaw());
        double fx = -Math.sin(yawRad), fz = Math.cos(yawRad), rx = -Math.cos(yawRad), rz = -Math.sin(yawRad);
        double tx = pl.getX() + rx * def.ox - fx * def.oz;
        double tz = pl.getZ() + rz * def.ox - fz * def.oz;
        double ty = pl.getY() + def.oy + Math.sin(tick * 0.08) * 0.08;
        if (!s.init || Math.abs(s.x - tx) > 12 || Math.abs(s.z - tz) > 12 || Math.abs(s.y - ty) > 8) {
            s.x = tx; s.y = ty; s.z = tz; s.yaw = pl.getYaw(); s.init = true;
        } else {
            double k = 0.28;
            double dx = tx - s.x, dz = tz - s.z;
            s.x += dx * k; s.y += (ty - s.y) * 0.35; s.z += dz * k;
            if (dx * dx + dz * dz > 0.02) {
                double target = Math.toDegrees(Math.atan2(-dx, dz));
                double diff = ((target - s.yaw + 540) % 360) - 180;
                s.yaw += diff * 0.35;
            } else {
                double diff = ((pl.getYaw() - s.yaw + 540) % 360) - 180;
                s.yaw += diff * 0.1;
            }
        }
        Location out = new Location(pl.getWorld(), s.x, s.y, s.z, (float) (s.yaw + def.yaw), 0f);
        return out;
    }

    public void forget(UUID id) { states.remove(id); }
}
