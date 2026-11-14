package com.aetheria.rewards.progression;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.data.PlayerData;
import com.aetheria.rewards.reward.Reward;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.TreeMap;

/** Evento independiente de febrero 2027: misiones (scope FEBRERO) -> puntos -> recompensas por tramos. */
public class FebruaryEventManager {
    private final AetheriaRewards plugin;
    public ZonedDateTime start, end;
    public final Map<Integer, Reward> tiers = new TreeMap<>();   // puntos requeridos -> recompensa

    public FebruaryEventManager(AetheriaRewards plugin) { this.plugin = plugin; load(); }
    private File file() { return new File(plugin.getDataFolder(), "february.yml"); }

    public void load() {
        tiers.clear();
        if (!file().exists()) plugin.saveResource("february.yml", false);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file());
        start = BattlePassManager.parse(y.getString("start", "2027-02-01T00:00"), plugin.zone());
        end = BattlePassManager.parse(y.getString("end", "2027-03-01T00:00"), plugin.zone());
        ConfigurationSection s = y.getConfigurationSection("tiers");
        if (s != null) for (String k : s.getKeys(false)) {
            try { Reward r = Reward.load(s.getConfigurationSection(k)); if (r != null) tiers.put(Integer.parseInt(k), r); } catch (NumberFormatException ignored) {}
        }
    }

    public void save() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file());
        y.set("tiers", null);
        for (var e : tiers.entrySet()) e.getValue().save(y.createSection("tiers." + e.getKey()));
        try { y.save(file()); } catch (IOException ex) { plugin.getLogger().warning("february.yml: " + ex.getMessage()); }
    }

    public boolean isActive() { ZonedDateTime n = ZonedDateTime.now(plugin.zone()); return n.isAfter(start) && n.isBefore(end); }
    public long secondsLeft() { return java.time.Duration.between(ZonedDateTime.now(plugin.zone()), end).getSeconds(); }

    /** Puntos = misiones de febrero reclamadas. */
    public int points(PlayerData d) {
        int n = 0;
        for (QuestManager.Quest q : plugin.quests().scope("FEBRERO")) if (d.questsClaimed.contains(q.id)) n++;
        return n;
    }

    public boolean claimTier(Player p, int points) {
        PlayerData d = plugin.data().get(p);
        if (d == null || !isActive() || points(d) < points) return false;
        Reward r = tiers.get(points);
        if (r == null || !d.februaryClaimed.add("tier" + points)) return false;
        plugin.data().markDirty(d);
        plugin.rewards().give(p, r);
        return true;
    }
}
