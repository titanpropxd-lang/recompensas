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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carga asincrona al pre-login y guardado asincrono (YAML, SQLite o MySQL segun config).
 * Si la carga falla, el jugador queda en modo SOLO LECTURA: nunca se sobrescriben sus datos con un perfil vacio.
 */
public class PlayerDataManager implements Listener {
    private final AetheriaRewards plugin;
    private final DataStore store;
    private final YamlStore legacy;      // para migrar automaticamente desde archivos YAML antiguos
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerData> preloaded = new ConcurrentHashMap<>();

    public PlayerDataManager(AetheriaRewards plugin, DataStore store, YamlStore legacy) {
        this.plugin = plugin; this.store = store; this.legacy = legacy;
        Bukkit.getScheduler().runTaskTimer(plugin, this::autosave, 20L * 120, 20L * 120);
    }

    public DataStore store() { return store; }

    private PlayerData load(UUID id) {
        try {
            String raw = store.load(id);
            if (raw == null && legacy != null && store != legacy) raw = legacy.load(id);   // migracion automatica
            if (raw == null) return new PlayerData(id);
            YamlConfiguration y = new YamlConfiguration();
            y.loadFromString(raw);
            return PlayerData.load(id, y);
        } catch (Exception e) {
            plugin.getLogger().severe("NO se pudieron cargar los datos de " + id + " (" + store.name() + "): " + e.getMessage() + " -> modo solo lectura");
            PlayerData d = new PlayerData(id);
            d.readOnly = true;
            return d;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        preloaded.put(e.getUniqueId(), load(e.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerData d = preloaded.remove(p.getUniqueId());
        if (d == null) d = load(p.getUniqueId());      // recarga en caliente
        d.name = p.getName();
        cache.put(p.getUniqueId(), d);
        plugin.onPlayerLoaded(p, d);
        if (d.readOnly) p.sendMessage(com.aetheria.rewards.util.Util.c(com.aetheria.rewards.util.Util.PREFIX + "§cTus datos no se pudieron cargar. Avisa a un administrador."));
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
        if (d.readOnly) return;
        final String content = d.serialize();     // snapshot en hilo principal
        d.dirty = false;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> write(d.uuid, content));
    }

    private void write(UUID id, String content) {
        try { store.save(id, content); }
        catch (Exception ex) { plugin.getLogger().severe("No se pudo guardar " + id + " (" + store.name() + "): " + ex.getMessage()); }
    }

    private void autosave() { for (PlayerData d : cache.values()) if (d.dirty) saveAsync(d); }

    /** Apagado del servidor: guardado sincrono de todo. */
    public void saveAllSync() {
        for (PlayerData d : cache.values()) if (!d.readOnly) write(d.uuid, d.serialize());
    }
}
