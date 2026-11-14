package com.aetheria.rewards.data;

import com.aetheria.rewards.AetheriaRewards;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carga asincrona al pre-login (el disco nunca bloquea el hilo principal), guardado asincrono
 * con escritura atomica (archivo temporal + move) y autosave periodico solo de datos modificados.
 */
public class PlayerDataManager implements Listener {
    private final AetheriaRewards plugin;
    private final File dir;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerData> preloaded = new ConcurrentHashMap<>();

    public PlayerDataManager(AetheriaRewards plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "players");
        dir.mkdirs();
        Bukkit.getScheduler().runTaskTimer(plugin, this::autosave, 20L * 120, 20L * 120);
    }

    private File file(UUID id) { return new File(dir, id + ".yml"); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        File f = file(e.getUniqueId());
        PlayerData d = f.exists() ? PlayerData.load(e.getUniqueId(), YamlConfiguration.loadConfiguration(f)) : new PlayerData(e.getUniqueId());
        preloaded.put(e.getUniqueId(), d);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerData d = preloaded.remove(p.getUniqueId());
        if (d == null) { // recarga en caliente: carga sincrona de emergencia
            File f = file(p.getUniqueId());
            d = f.exists() ? PlayerData.load(p.getUniqueId(), YamlConfiguration.loadConfiguration(f)) : new PlayerData(p.getUniqueId());
        }
        d.name = p.getName();
        cache.put(p.getUniqueId(), d);
        plugin.onPlayerLoaded(p, d);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        plugin.onPlayerUnload(e.getPlayer());
        PlayerData d = cache.remove(id);
        if (d != null) saveAsync(d);
    }

    public PlayerData get(Player p) { return cache.get(p.getUniqueId()); }
    public PlayerData get(UUID id) { return cache.get(id); }

    public void markDirty(PlayerData d) { d.dirty = true; }

    public void saveAsync(PlayerData d) {
        final String content = d.serialize();     // snapshot en hilo principal
        final File target = file(d.uuid);
        d.dirty = false;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> write(target, content));
    }

    private synchronized void write(File target, String content) {
        try {
            File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
            Files.writeString(tmp.toPath(), content, StandardCharsets.UTF_8);
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            plugin.getLogger().severe("No se pudo guardar " + target.getName() + ": " + ex.getMessage());
        }
    }

    private void autosave() { for (PlayerData d : cache.values()) if (d.dirty) saveAsync(d); }

    /** Apagado del servidor: guardado sincrono de todo. */
    public void saveAllSync() {
        for (PlayerData d : cache.values()) write(file(d.uuid), d.serialize());
    }
}
