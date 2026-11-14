package com.aetheria.rewards.progression;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.data.PlayerData;
import com.aetheria.rewards.reward.Reward;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/** Calendario de adviento 1-24 de diciembre de 2026. Zona horaria y reclamo de dias pasados configurables. */
public class AdventCalendarManager {
    public enum State { LOCKED, AVAILABLE, CLAIMED, MISSED }
    private final AetheriaRewards plugin;
    public int year = 2026;
    public boolean claimPast = true;
    public ZonedDateTime endClaim;
    public final Map<Integer, Reward> days = new HashMap<>();

    public AdventCalendarManager(AetheriaRewards plugin) { this.plugin = plugin; load(); }
    private File file() { return new File(plugin.getDataFolder(), "advent.yml"); }

    public void load() {
        days.clear();
        if (!file().exists()) plugin.saveResource("advent.yml", false);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file());
        year = y.getInt("year", 2026);
        claimPast = y.getBoolean("claim-past-days", true);
        endClaim = BattlePassManager.parse(y.getString("end-claim", year + "-12-31T23:59"), plugin.zone());
        ConfigurationSection s = y.getConfigurationSection("days");
        if (s != null) for (String k : s.getKeys(false)) {
            try { int d = Integer.parseInt(k); Reward r = Reward.load(s.getConfigurationSection(k)); if (r != null && d >= 1 && d <= 24) days.put(d, r); }
            catch (NumberFormatException ignored) {}
        }
    }

    public void save() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file());
        y.set("claim-past-days", claimPast);
        y.set("days", null);
        for (var e : days.entrySet()) e.getValue().save(y.createSection("days." + e.getKey()));
        try { y.save(file()); } catch (IOException ex) { plugin.getLogger().warning("advent.yml: " + ex.getMessage()); }
    }

    /** Dia del calendario "de hoy": 0 = antes de diciembre, 25+ = despues del 24. */
    public int today() {
        LocalDate n = LocalDate.now(plugin.zone());
        LocalDate dec1 = LocalDate.of(year, 12, 1);
        if (n.isBefore(dec1)) return 0;
        return (int) (n.toEpochDay() - dec1.toEpochDay()) + 1;
    }

    public State state(PlayerData d, int day) {
        if (d.adventClaimed.contains(day)) return State.CLAIMED;
        int t = today();
        if (day > t) return State.LOCKED;
        if (ZonedDateTime.now(plugin.zone()).isAfter(endClaim)) return State.MISSED;
        if (day < t && !claimPast) return State.MISSED;
        return State.AVAILABLE;
    }

    public boolean claim(Player p, int day) {
        PlayerData d = plugin.data().get(p);
        if (d == null || state(d, day) != State.AVAILABLE) return false;
        Reward r = days.get(day);
        if (r == null) return false;
        if (!d.adventClaimed.add(day)) return false;       // marca antes de entregar
        plugin.data().markDirty(d);
        plugin.rewards().give(p, r);
        plugin.cosmetics().trigger(p, "VICTORY");
        return true;
    }
}
