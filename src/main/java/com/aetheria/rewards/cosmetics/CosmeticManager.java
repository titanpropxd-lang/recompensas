package com.aetheria.rewards.cosmetics;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.data.PlayerData;
import com.aetheria.rewards.model.ModelHandle;
import com.aetheria.rewards.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Catalogo, propiedad, equipamiento y motor visual de cosmeticos.
 * UNA sola tarea global (cada 2 ticks) procesa solo a los jugadores con cosmeticos activos:
 * no hay tareas por jugador ni entidades reales; los modelos son paquetes de BetterModel.
 */
public class CosmeticManager {
    public static final List<String> SLOTS = List.of("AURA", "HAT", "PET", "BACK", "RING", "ORBIT", "TRANSFORM", "SPECIAL",
            "WALK", "JUMP", "LAND", "KILL", "DAMAGE", "SPAWN", "VICTORY");
    public static final int PREVIEW_SECONDS = 25;

    private final AetheriaRewards plugin;
    private final Map<String, CosmeticDef> defs = new LinkedHashMap<>();
    private final AuraManager auras;
    private final PetManager pets;
    private final Set<UUID> active = new HashSet<>();
    private final Map<UUID, Map<String, ModelHandle>> handles = new HashMap<>();
    private final Map<UUID, Map<String, String>> previews = new HashMap<>();
    private final Map<UUID, Long> previewEnd = new HashMap<>();
    private final Map<UUID, double[]> lastPos = new HashMap<>();
    private final Map<UUID, double[]> bodyYaw = new HashMap<>();
    private final Map<UUID, Float> maxFall = new HashMap<>();
    private final Map<String, Long> cooldown = new HashMap<>();
    private long tick = 0;

    public CosmeticManager(AetheriaRewards plugin) {
        this.plugin = plugin;
        this.auras = new AuraManager(plugin);
        this.pets = new PetManager(plugin);
        load();
        Bukkit.getScheduler().runTaskTimer(plugin, this::run, 40L, 2L);
    }

    // ------------------------------------------------------------------ catalogo
    public final File catalogFile() { return new File(plugin.getDataFolder(), "catalog.yml"); }

    public void load() {
        defs.clear();
        File f = catalogFile();
        if (!f.exists()) plugin.saveResource("catalog.yml", false);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        var sec = y.getConfigurationSection("cosmetics");
        if (sec != null) for (String id : sec.getKeys(false)) defs.put(id, CosmeticDef.load(id, sec.getConfigurationSection(id)));
        // cosmeticos nuevos de actualizaciones del plugin
        try (var in = plugin.getResource("catalog.yml")) {
            if (in != null) {
                YamlConfiguration b = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
                var bs = b.getConfigurationSection("cosmetics");
                if (bs != null) for (String id : bs.getKeys(false)) defs.computeIfAbsent(id, k -> CosmeticDef.load(k, bs.getConfigurationSection(k)));
            }
        } catch (IOException ignored) {}
        plugin.getLogger().info(defs.size() + " cosméticos cargados (" + defs.values().stream().filter(d -> !d.premium).count() + " gratuitos).");
    }

    /** Guarda el ajuste de colocacion/estado de un cosmetico en catalog.yml. */
    public void saveTuning(CosmeticDef d) {
        File f = catalogFile();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        String p = "cosmetics." + d.id + ".";
        y.set(p + "offset", List.of(round(d.ox), round(d.oy), round(d.oz)));
        y.set(p + "yaw", d.yaw);
        y.set(p + "scale", round(d.scale));
        y.set(p + "publicado", d.published);
        try { y.save(f); } catch (IOException e) { plugin.getLogger().warning("No se pudo guardar catalog.yml: " + e.getMessage()); }
    }
    private static double round(double v) { return Math.round(v * 1000) / 1000.0; }

    public CosmeticDef get(String id) { return id == null ? null : defs.get(id); }
    public Collection<CosmeticDef> all() { return defs.values(); }
    public List<CosmeticDef> byCategory(CosmeticDef.Category c) {
        List<CosmeticDef> l = new ArrayList<>();
        for (CosmeticDef d : defs.values()) if (d.category == c) l.add(d);
        return l;
    }

    // ------------------------------------------------------------------ propiedad / equipo
    public boolean owned(PlayerData d, CosmeticDef def) { return !def.premium || d.unlocked.contains(def.id); }

    public void unlock(Player p, CosmeticDef def) {
        PlayerData d = plugin.data().get(p);
        if (d == null) return;
        d.unlocked.add(def.id);
        plugin.data().markDirty(d);
    }

    public ItemStack icon(CosmeticDef def, PlayerData d) {
        boolean own = d == null || owned(d, def);
        List<String> lore = new ArrayList<>();
        lore.add(def.rarity.tag() + " §8• §7" + def.category.label);
        lore.add(def.premium ? "§d✦ Premium" : "§a✔ Gratuito");
        if (def.event != null) lore.add(def.event.equals("febrero") ? "§d❤ Evento de Febrero" : "§b❄ Evento de Adviento");
        lore.add("");
        lore.add("§7" + def.desc);
        if (d != null) {
            lore.add("");
            if (!own) lore.add("§c🔒 Bloqueado §8- consíguelo en cofres, pase o eventos");
            else {
                boolean eq = def.id.equals(d.equipped.get(def.slot));
                lore.add(eq ? "§a✔ Equipado §8- clic para quitar" : "§e▶ Clic para equipar");
                lore.add("§e♥ Clic derecho: favorito §8" + (d.favorites.contains(def.id) ? "(★ marcado)" : ""));
            }
            lore.add("§b👁 Shift+clic: previsualizar " + PREVIEW_SECONDS + "s");
        }
        ItemStack it = Util.item(Material.PAPER, def.cmd, def.rarity.color + "§l" + def.name, lore);
        if (d != null && def.id.equals(d.equipped.get(def.slot))) Util.glow(it);
        if (d != null && !own) { it.setType(Material.PAPER); }
        return it;
    }

    public boolean equip(Player p, CosmeticDef def) {
        PlayerData d = plugin.data().get(p);
        if (d == null) return false;
        if (!owned(d, def)) { p.sendMessage(Util.c(Util.PREFIX + "§cAún no has desbloqueado este cosmético.")); return false; }
        if (!def.published && !p.hasPermission("rewards.admin")) return false;
        String old = d.equipped.put(def.slot, def.id);
        if (old != null && !old.equals(def.id)) clearSlotVisual(p.getUniqueId(), def.slot);
        plugin.data().markDirty(d);
        refreshActive(p);
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.4f);
        return true;
    }

    public void unequip(Player p, String slot) {
        PlayerData d = plugin.data().get(p);
        if (d == null) return;
        d.equipped.remove(slot);
        clearSlotVisual(p.getUniqueId(), slot);
        plugin.data().markDirty(d);
        refreshActive(p);
    }

    public void unequipAll(Player p) {
        PlayerData d = plugin.data().get(p);
        if (d == null) return;
        d.equipped.clear();
        clearAllVisuals(p.getUniqueId());
        plugin.data().markDirty(d);
        refreshActive(p);
    }

    /** Previsualizacion temporal (no cambia lo equipado ni requiere propiedad). */
    public void preview(Player p, CosmeticDef def) {
        previews.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(def.slot, def.id);
        previewEnd.put(p.getUniqueId(), System.currentTimeMillis() + PREVIEW_SECONDS * 1000L);
        clearSlotVisual(p.getUniqueId(), def.slot);
        active.add(p.getUniqueId());
        p.sendMessage(Util.c(Util.PREFIX + "Previsualizando " + def.rarity.color + def.name + " §7durante " + PREVIEW_SECONDS + "s."));
    }

    public void refreshActive(Player p) {
        PlayerData d = plugin.data().get(p);
        boolean any = (d != null && !d.equipped.isEmpty()) || previews.containsKey(p.getUniqueId());
        if (any) active.add(p.getUniqueId()); else { active.remove(p.getUniqueId()); clearAllVisuals(p.getUniqueId()); }
    }

    public void onJoin(Player p, PlayerData d) {
        // elimina equipado que ya no exista / no se posea
        d.equipped.entrySet().removeIf(e -> { CosmeticDef c = get(e.getValue()); return c == null || !owned(d, c); });
        refreshActive(p);
        if (d.equipped.containsKey("SPAWN")) Bukkit.getScheduler().runTaskLater(plugin, () -> trigger(p, "SPAWN"), 10L);
    }

    public void onQuit(Player p) {
        UUID id = p.getUniqueId();
        active.remove(id); previews.remove(id); previewEnd.remove(id); lastPos.remove(id); bodyYaw.remove(id); maxFall.remove(id);
        pets.forget(id);
        clearAllVisuals(id);
    }

    public void shutdown() { for (UUID id : new ArrayList<>(handles.keySet())) clearAllVisuals(id); }

    // ------------------------------------------------------------------ visuales
    private void clearSlotVisual(UUID id, String slot) {
        Map<String, ModelHandle> m = handles.get(id);
        if (m == null) return;
        ModelHandle h = m.remove(slot);
        if (h != null) h.close();
    }
    private void clearAllVisuals(UUID id) {
        Map<String, ModelHandle> m = handles.remove(id);
        if (m != null) for (ModelHandle h : m.values()) h.close();
    }

    /** Dispara un efecto puntual (JUMP, LAND, KILL, DAMAGE, SPAWN, VICTORY). */
    public void trigger(Player p, String slot) {
        PlayerData d = plugin.data().get(p);
        if (d == null) return;
        String id = effective(p, d, slot);
        if (id == null) return;
        CosmeticDef def = get(id);
        if (def == null || p.isInvisible() || p.getGameMode() == GameMode.SPECTATOR) return;
        String key = p.getUniqueId() + slot;
        long now = System.currentTimeMillis();
        if (now - cooldown.getOrDefault(key, 0L) < 400) return;
        cooldown.put(key, now);
        auras.drawAll(def, p.getLocation(), tick, Math.toRadians(p.getLocation().getYaw()), true);
    }

    private String effective(Player p, PlayerData d, String slot) {
        Map<String, String> pv = previews.get(p.getUniqueId());
        if (pv != null && pv.containsKey(slot)) return pv.get(slot);
        return d.equipped.get(slot);
    }

    private void run() {
        tick += 2;
        if (active.isEmpty()) return;
        long now = System.currentTimeMillis();
        Iterator<UUID> it = active.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) { it.remove(); continue; }
            PlayerData d = plugin.data().get(p);
            if (d == null) continue;
            Long end = previewEnd.get(id);
            if (end != null && now > end) { previews.remove(id); previewEnd.remove(id); clearAllVisuals(id); refreshActive0(p, d, it); }
            boolean hidden = p.isInvisible() || p.isDead() || p.getGameMode() == GameMode.SPECTATOR;
            if (hidden) { clearAllVisuals(id); continue; }
            Location loc = p.getLocation();
            double yawRad = Math.toRadians(loc.getYaw());

            // yaw corporal suavizado (alas/capas)
            double[] by = bodyYaw.computeIfAbsent(id, k -> new double[]{loc.getYaw()});
            double diff = ((loc.getYaw() - by[0] + 540) % 360) - 180;
            by[0] += diff * 0.3;
            double bodyRad = Math.toRadians(by[0]);

            for (String slot : SLOTS) {
                String cid = effective(p, d, slot);
                if (cid == null) continue;
                CosmeticDef def = get(cid);
                if (def == null) continue;
                switch (slot) {
                    case "AURA", "TRANSFORM" -> auras.drawAll(def, loc, tick, bodyRad, false);
                    case "SPECIAL" -> {
                        boolean continuous = def.specs.stream().anyMatch(s -> s.forma.equalsIgnoreCase("RAIN"));
                        if (continuous) auras.drawAll(def, loc, tick, bodyRad, false);
                        else if (tick % 100 == 0) auras.drawAll(def, loc, tick, bodyRad, true);
                    }
                    case "WALK" -> {
                        double[] lp = lastPos.computeIfAbsent(id, k -> new double[]{loc.getX(), loc.getZ()});
                        double dx = loc.getX() - lp[0], dz = loc.getZ() - lp[1];
                        if (dx * dx + dz * dz > 0.012 && p.isOnGround()) auras.drawAll(def, loc, tick, yawRad, true);
                        lp[0] = loc.getX(); lp[1] = loc.getZ();
                    }
                    case "HAT" -> attach(p, slot, def, headLoc(p, def, yawRad), "idle");
                    case "BACK" -> attach(p, slot, def, relLoc(loc, def, bodyRad, sneakDrop(p)), "idle");
                    case "RING", "ORBIT" -> attach(p, slot, def, relLoc(loc, def, 0, 0), "idle");
                    case "PET" -> attach(p, slot, def, pets.follow(p, def, tick), "idle");
                    default -> {}
                }
                if (!def.specs.isEmpty() && (slot.equals("HAT") || slot.equals("PET") || slot.equals("BACK") || slot.equals("RING") || slot.equals("ORBIT"))
                        && tick % 6 == 0) {
                    Location at = slot.equals("PET") ? pets.follow(p, def, tick) : loc;
                    for (CosmeticDef.Spec s : def.specs) auras.draw(slot.equals("HAT") ? headLoc(p, def, yawRad) : at, s, tick, bodyRad, false);
                }
            }
            // aterrizaje
            float fall = p.getFallDistance();
            Float prev = maxFall.get(id);
            if (fall > 0) maxFall.put(id, Math.max(fall, prev == null ? 0 : prev));
            else if (prev != null) { if (prev > 1.6f && p.isOnGround()) trigger(p, "LAND"); maxFall.remove(id); }
        }
    }
    private void refreshActive0(Player p, PlayerData d, Iterator<UUID> it) {
        if (d.equipped.isEmpty() && !previews.containsKey(p.getUniqueId())) { it.remove(); clearAllVisuals(p.getUniqueId()); }
    }

    private double sneakDrop(Player p) { return p.isSneaking() ? -0.2 : 0; }

    private Location headLoc(Player p, CosmeticDef def, double yawRad) {
        Location eye = p.getEyeLocation();
        double fx = -Math.sin(yawRad), fz = Math.cos(yawRad), rx = -Math.cos(yawRad), rz = -Math.sin(yawRad);
        return new Location(p.getWorld(), eye.getX() + rx * def.ox - fx * def.oz, eye.getY() + 0.18 + def.oy,
                eye.getZ() + rz * def.ox - fz * def.oz, (float) (Math.toDegrees(yawRad) + def.yaw), 0f);
    }

    private Location relLoc(Location base, CosmeticDef def, double yawRad, double dy) {
        double fx = -Math.sin(yawRad), fz = Math.cos(yawRad), rx = -Math.cos(yawRad), rz = -Math.sin(yawRad);
        return new Location(base.getWorld(), base.getX() + rx * def.ox - fx * def.oz, base.getY() + def.oy + dy,
                base.getZ() + rz * def.ox - fz * def.oz, (float) (Math.toDegrees(yawRad) + def.yaw), 0f);
    }

    private void attach(Player p, String slot, CosmeticDef def, Location at, String anim) {
        if (!def.hasModel() || !plugin.models().available()) return;
        Map<String, ModelHandle> m = handles.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        ModelHandle h = m.get(slot);
        if (h == null || h.isClosed()) {
            if (tick % 20 != 0 && h == null && !plugin.models().loaded(def.model)) return;   // reintento cada 1s si el modelo aun no cargo
            h = plugin.models().spawn(def.model, at, (float) def.scale);
            if (h == null) return;
            h.animate(anim, ModelHandle.Mode.LOOP);
            m.put(slot, h);
        } else h.move(at);
    }

    public Collection<String> slotsOf(PlayerData d) { return d.equipped.keySet(); }
}
