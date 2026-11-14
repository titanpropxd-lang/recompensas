package com.aetheria.rewards.crate;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.data.PlayerData;
import com.aetheria.rewards.model.ModelHandle;
import com.aetheria.rewards.reward.Reward;
import com.aetheria.rewards.util.Rarity;
import com.aetheria.rewards.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CrateManager {
    private final AetheriaRewards plugin;
    private final KeyManager keys;
    private final Map<String, Crate> crates = new LinkedHashMap<>();
    private final Map<String, Placement> placements = new LinkedHashMap<>();
    private final Map<UUID, RouletteMenu> sessions = new HashMap<>();
    public final NamespacedKey placementTag;

    public CrateManager(AetheriaRewards plugin) {
        this.plugin = plugin;
        this.keys = new KeyManager(plugin);
        this.placementTag = new NamespacedKey(plugin, "placement");
        loadCrates();
        loadPlacements();
    }

    public KeyManager keys() { return keys; }

    // ------------------------------------------------------------------ persistencia
    private File cratesFile() { return new File(plugin.getDataFolder(), "crates.yml"); }
    private File placementsFile() { return new File(plugin.getDataFolder(), "placements.yml"); }

    public void loadCrates() {
        crates.clear();
        if (!cratesFile().exists()) plugin.saveResource("crates.yml", false);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(cratesFile());
        ConfigurationSection cs = y.getConfigurationSection("crates");
        if (cs == null) return;
        for (String id : cs.getKeys(false)) {
            ConfigurationSection s = cs.getConfigurationSection(id);
            Crate c = new Crate();
            c.id = id;
            c.name = s.getString("name", id);
            c.rarity = Rarity.parse(s.getString("rarity"), Rarity.COMMON);
            c.model = s.getString("model", "crate_" + id);
            c.scale = s.getDouble("scale", 1.0);
            c.fallbackBlock = s.getString("fallback-block", "BARREL");
            c.keyName = s.getString("key.name", "Llave de " + id);
            c.keyCmd = s.getInt("key.cmd", 3001);
            Material km = Material.matchMaterial(s.getString("key.material", "AMETHYST_SHARD"));
            c.keyMaterial = km == null ? Material.AMETHYST_SHARD : km;
            ConfigurationSection rs = s.getConfigurationSection("rewards");
            if (rs != null) for (String k : rs.getKeys(false)) {
                ConfigurationSection e = rs.getConfigurationSection(k);
                Reward r = Reward.load(e.getConfigurationSection("reward"));
                if (r == null) { plugin.getLogger().warning("Recompensa invalida en crate " + id + "#" + k); continue; }
                c.rewards.add(new Crate.Entry(r, e.getDouble("weight", 1), Rarity.parse(e.getString("rarity"), Rarity.COMMON)));
            }
            crates.put(id, c);
        }
        plugin.getLogger().info(crates.size() + " crates cargados.");
    }

    public void saveCrates() {
        YamlConfiguration y = new YamlConfiguration();
        for (Crate c : crates.values()) {
            String p = "crates." + c.id + ".";
            y.set(p + "name", c.name); y.set(p + "rarity", c.rarity.name()); y.set(p + "model", c.model);
            y.set(p + "scale", c.scale); y.set(p + "fallback-block", c.fallbackBlock);
            y.set(p + "key.name", c.keyName); y.set(p + "key.cmd", c.keyCmd); y.set(p + "key.material", c.keyMaterial.name());
            int i = 0;
            for (Crate.Entry e : c.rewards) {
                String q = p + "rewards.r" + (i++) + ".";
                y.set(q + "weight", e.weight); y.set(q + "rarity", e.rarity.name());
                e.reward.save(y.createSection(q + "reward"));
            }
        }
        try { y.save(cratesFile()); } catch (IOException ex) { plugin.getLogger().severe("No se pudo guardar crates.yml: " + ex.getMessage()); }
    }

    private void loadPlacements() {
        placements.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(placementsFile());
        ConfigurationSection ps = y.getConfigurationSection("placements");
        if (ps == null) return;
        for (String id : ps.getKeys(false)) {
            ConfigurationSection s = ps.getConfigurationSection(id);
            Placement p = new Placement();
            p.id = id; p.crateId = s.getString("crate");
            try { p.type = Placement.Type.valueOf(s.getString("type", "CRATE")); } catch (Exception e) { p.type = Placement.Type.CRATE; }
            p.world = s.getString("world"); p.x = s.getDouble("x"); p.y = s.getDouble("y"); p.z = s.getDouble("z"); p.yaw = (float) s.getDouble("yaw");
            placements.put(id, p);
        }
    }

    public void savePlacements() {
        YamlConfiguration y = new YamlConfiguration();
        for (Placement p : placements.values()) {
            String q = "placements." + p.id + ".";
            y.set(q + "crate", p.crateId); y.set(q + "type", p.type.name()); y.set(q + "world", p.world);
            y.set(q + "x", p.x); y.set(q + "y", p.y); y.set(q + "z", p.z); y.set(q + "yaw", p.yaw);
        }
        try { y.save(placementsFile()); } catch (IOException ex) { plugin.getLogger().severe("No se pudo guardar placements.yml: " + ex.getMessage()); }
    }

    // ------------------------------------------------------------------ accesores
    public Crate get(String id) { return crates.get(id); }
    public Collection<Crate> all() { return crates.values(); }
    public Collection<Placement> placements() { return placements.values(); }
    public Placement placement(String id) { return placements.get(id); }
    public String keyName(String crateId) { Crate c = crates.get(crateId); return c == null ? "§cLlave desconocida" : c.rarity.color + c.keyName; }
    public ItemStack keyItem(String crateId, int n) {
        Crate c = crates.get(crateId);
        return c == null ? Util.item(Material.BARRIER, "§cLlave inválida: " + crateId) : keys.create(c, n);
    }

    public Crate createCrate(String id, String name) {
        Crate c = new Crate(); c.id = id; c.name = "§f" + name; c.model = "crate_madera"; c.keyName = "Llave " + name;
        crates.put(id, c); saveCrates(); return c;
    }
    public void deleteCrate(String id) {
        crates.remove(id);
        for (Placement p : new ArrayList<>(placements.values())) if (id.equals(p.crateId)) removePlacement(p.id);
        saveCrates();
    }

    // ------------------------------------------------------------------ colocacion en el mundo
    public Placement place(Crate c, Location loc, Placement.Type type) {
        int n = 1; while (placements.containsKey(c.id + "_" + n)) n++;
        Placement p = new Placement();
        p.id = c.id + "_" + n; p.crateId = c.id; p.type = type;
        p.world = loc.getWorld().getName(); p.x = loc.getBlockX() + 0.5; p.y = loc.getY(); p.z = loc.getBlockZ() + 0.5; p.yaw = loc.getYaw();
        placements.put(p.id, p);
        savePlacements();
        spawnVisual(p);
        return p;
    }

    public void removePlacement(String id) {
        Placement p = placements.remove(id);
        if (p == null) return;
        despawnVisual(p);
        savePlacements();
    }

    public void rotate(Placement p, float yaw) { p.yaw = yaw; savePlacements(); despawnVisual(p); spawnVisual(p); }

    /** Crea todo lo visual (tras cargar el servidor / BetterModel). */
    public void spawnAll() { for (Placement p : placements.values()) spawnVisual(p); }
    public void despawnAll() { for (Placement p : placements.values()) despawnVisual(p); }

    public void spawnVisual(Placement p) {
        Crate c = crates.get(p.crateId);
        Location loc = p.location();
        if (c == null || loc == null) return;
        // 1) modelo BetterModel (paquetes, sin entidad)
        if ((p.handle == null || p.handle.isClosed()) && plugin.models().available()) {
            ModelHandle h = plugin.models().spawn(c.model, loc, (float) c.scale);
            if (h != null) { p.handle = h; h.animate("idle", ModelHandle.Mode.LOOP); }
            else if (p.modelRetries++ < 30) Bukkit.getScheduler().runTaskLater(plugin, () -> { if (placements.containsKey(p.id)) spawnVisual(p); }, 100L);
        }
        // 2) respaldo si no hay modelo
        if (p.handle == null && (p.fallback == null || !p.fallback.isValid()) && p.modelRetries >= 30 || p.handle == null && !plugin.models().available()) {
            if (isLoaded(loc) && (p.fallback == null || !p.fallback.isValid())) {
                Material m = Material.matchMaterial(c.fallbackBlock);
                p.fallback = plugin.models().fallbackBlock(loc, m == null ? Material.BARREL : m);
            }
        }
        // 3) hitbox de interaccion + etiqueta (1 entidad cada una por cofre; no por jugador)
        if (isLoaded(loc)) {
            if (p.hitbox == null || Bukkit.getEntity(p.hitbox) == null) {
                Interaction in = loc.getWorld().spawn(loc.clone().add(0, 0, 0), Interaction.class, i -> {
                    i.setInteractionWidth(1.1f); i.setInteractionHeight(1.1f); i.setResponsive(true); i.setPersistent(false);
                    i.getPersistentDataContainer().set(placementTag, PersistentDataType.STRING, p.id);
                });
                p.hitbox = in.getUniqueId();
            }
            if (p.type == Placement.Type.CRATE && (p.label == null || Bukkit.getEntity(p.label) == null)) {
                TextDisplay td = loc.getWorld().spawn(loc.clone().add(0, 1.55, 0), TextDisplay.class, t -> {
                    t.text(Util.c(c.name + "\n§7Clic derecho con " + c.rarity.color + c.keyName + "\n§8Clic izquierdo: ver recompensas"));
                    t.setBillboard(Display.Billboard.CENTER); t.setPersistent(false); t.setSeeThrough(false);
                    t.setBackgroundColor(Color.fromARGB(110, 20, 10, 40));
                    t.setViewRange(0.6f);
                    t.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1f, 1f, 1f), new Quaternionf()));
                });
                p.label = td.getUniqueId();
            }
        }
    }

    private static boolean isLoaded(Location l) { return l.getWorld().isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4); }

    public void despawnVisual(Placement p) {
        if (p.handle != null) { p.handle.close(); p.handle = null; }
        if (p.fallback != null) { p.fallback.remove(); p.fallback = null; }
        if (p.hitbox != null) { Entity e = Bukkit.getEntity(p.hitbox); if (e != null) e.remove(); p.hitbox = null; }
        if (p.label != null) { Entity e = Bukkit.getEntity(p.label); if (e != null) e.remove(); p.label = null; }
        p.busy = false;
    }

    public void onChunkLoad(Chunk ch) {
        long key = ((long) ch.getX() << 32) ^ (ch.getZ() & 0xFFFFFFFFL);
        for (Placement p : placements.values()) {
            if (!ch.getWorld().getName().equals(p.world) || p.chunkKey() != key) continue;
            Bukkit.getScheduler().runTask(plugin, () -> spawnVisual(p));
        }
    }

    // ------------------------------------------------------------------ apertura
    public void interact(Player pl, Placement p, boolean preview) {
        Crate c = crates.get(p.crateId);
        if (c == null) return;
        if (p.type == Placement.Type.HIDDEN) { plugin.explorer().discover(pl, p, c); return; }
        if (preview) { new CratePreviewMenu(plugin, pl, c, null).open(); return; }
        if (!pl.hasPermission("rewards.use")) return;
        PlayerData d = plugin.data().get(pl);
        if (d == null) return;
        if (p.busy || sessions.containsKey(pl.getUniqueId())) { pl.sendActionBar(Util.c("§cEl cofre está ocupado.")); return; }
        if (c.rewards.isEmpty()) { pl.sendMessage(Util.c(Util.PREFIX + "§cEste cofre no tiene recompensas configuradas.")); return; }
        if (!keys.consume(pl, c.id)) {
            pl.playSound(pl.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 0.8f);
            pl.sendMessage(Util.c(Util.PREFIX + "Necesitas " + c.rarity.color + c.keyName + "§7 para abrir este cofre."));
            return;
        }
        Crate.Entry result = c.roll();
        p.busy = true;
        startSession(pl, p, c, result);
    }

    private void startSession(Player pl, Placement p, Crate c, Crate.Entry result) {
        Location loc = p.location();
        if (p.handle != null) p.handle.animate("open", ModelHandle.Mode.HOLD);
        if (loc != null) {
            loc.getWorld().playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, 1.2f, 0.7f);
            loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
            loc.getWorld().spawnParticle(Particle.ENCHANT, loc.clone().add(0, 1, 0), 60, 0.6, 0.6, 0.6, 1.2);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.handle != null) p.handle.animate("reveal", ModelHandle.Mode.LOOP); }, 22L);
        RouletteMenu rm = new RouletteMenu(plugin, pl, c, result, p);
        sessions.put(pl.getUniqueId(), rm);
        rm.open();
        rm.start();
    }

    /** Llamado por la ruleta al terminar (o al cerrarse/desconectarse): entrega y cierra el cofre. Idempotente. */
    public void finish(Player pl, RouletteMenu rm, Placement p, Crate c, Crate.Entry result) {
        if (sessions.get(pl.getUniqueId()) != rm) return;
        sessions.remove(pl.getUniqueId());
        plugin.rewards().give(pl, result.reward);
        plugin.quests().progress(pl, "OPEN_CRATE", 1);
        Location loc = p.location();
        if (loc != null) {
            Rarity r = result.rarity;
            loc.getWorld().spawnParticle(r.ordinal() >= 3 ? Particle.TOTEM_OF_UNDYING : Particle.END_ROD, loc.clone().add(0, 1.3, 0), 50 + r.ordinal() * 25, 0.5, 0.6, 0.5, 0.25);
            loc.getWorld().playSound(loc, r.ordinal() >= 3 ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 1f);
        }
        pl.showTitle(net.kyori.adventure.title.Title.title(Util.c(result.rarity.tag()), Util.c(result.reward.describe(plugin)),
                net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(200), java.time.Duration.ofMillis(2200), java.time.Duration.ofMillis(500))));
        if (result.rarity.ordinal() >= Rarity.LEGENDARY.ordinal()) {
            plugin.cosmetics().trigger(pl, "VICTORY");
            Bukkit.broadcast(Util.c(Util.PREFIX + "§e" + pl.getName() + " §7obtuvo " + result.reward.describe(plugin) + " §7en un " + c.name + "§7!"));
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.handle != null) p.handle.animate("close", ModelHandle.Mode.ONCE);
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.handle != null) p.handle.animate("idle", ModelHandle.Mode.LOOP); p.busy = false; }, 22L);
        }, 50L);
    }

    public RouletteMenu session(UUID id) { return sessions.get(id); }

    /** Apagado / desconexion: nadie pierde su llave sin recibir el premio. */
    public void finishAllSessions() {
        for (RouletteMenu rm : new ArrayList<>(sessions.values())) rm.forceFinish();
    }
    public void finishSession(Player p) { RouletteMenu rm = sessions.get(p.getUniqueId()); if (rm != null) rm.forceFinish(); }
}
