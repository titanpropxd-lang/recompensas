package com.aetheria.rewards.crate;

import com.aetheria.rewards.model.ModelHandle;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;

import java.util.UUID;

/** Un cofre colocado en el mundo (crate normal o cofre oculto de exploracion). */
public class Placement {
    public enum Type { CRATE, HIDDEN }
    public String id, crateId;
    public Type type = Type.CRATE;
    public String world; public double x, y, z; public float yaw;
    public boolean busy = false;
    // runtime
    public ModelHandle handle;
    public BlockDisplay fallback;
    public UUID hitbox, label;
    public int modelRetries = 0;

    public Location location() {
        org.bukkit.World w = org.bukkit.Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z, yaw, 0f);
    }
    public long chunkKey() { return (((long) ((int) Math.floor(x) >> 4)) << 32) ^ (((int) Math.floor(z) >> 4) & 0xFFFFFFFFL); }
    public Entity hitboxEntity() { return hitbox == null ? null : org.bukkit.Bukkit.getEntity(hitbox); }
}
