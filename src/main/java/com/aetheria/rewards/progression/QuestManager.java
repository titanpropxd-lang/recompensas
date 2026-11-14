package com.aetheria.rewards.progression;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.data.PlayerData;
import com.aetheria.rewards.reward.Reward;
import com.aetheria.rewards.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Misiones diarias, semanales, de temporada y de evento. Reinicio por zona horaria configurable. */
public class QuestManager {
    public static class Quest {
        public String id, scope, type, name, desc; public int amount; public Reward reward;
    }
    private final AetheriaRewards plugin;
    private final Map<String, Quest> quests = new LinkedHashMap<>();

    public QuestManager(AetheriaRewards plugin) { this.plugin = plugin; load(); }

    private File file() { return new File(plugin.getDataFolder(), "quests.yml"); }

    public void load() {
        quests.clear();
        if (!file().exists()) plugin.saveResource("quests.yml", false);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file());
        ConfigurationSection s = y.getConfigurationSection("quests");
        if (s == null) return;
        for (String id : s.getKeys(false)) {
            ConfigurationSection q = s.getConfigurationSection(id);
            Quest x = new Quest();
            x.id = id; x.scope = q.getString("scope", "DAILY").toUpperCase(); x.type = q.getString("type", "KILL_MOB").toUpperCase();
            x.name = q.getString("name", id); x.desc = q.getString("desc", ""); x.amount = Math.max(1, q.getInt("amount", 1));
            x.reward = Reward.load(q.getConfigurationSection("reward"));
            if (x.reward == null) x.reward = Reward.xp(100);
            quests.put(id, x);
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Quest x : quests.values()) {
            String p = "quests." + x.id + ".";
            y.set(p + "scope", x.scope); y.set(p + "type", x.type); y.set(p + "name", x.name); y.set(p + "desc", x.desc); y.set(p + "amount", x.amount);
            x.reward.save(y.createSection(p + "reward"));
        }
        try { y.save(file()); } catch (IOException e) { plugin.getLogger().warning("quests.yml: " + e.getMessage()); }
    }

    public Collection<Quest> all() { return quests.values(); }
    public Quest get(String id) { return quests.get(id); }
    public List<Quest> scope(String scope) {
        List<Quest> l = new ArrayList<>();
        for (Quest q : quests.values()) if (scope.equals("ALL") || q.scope.equals(scope)) l.add(q);
        return l;
    }
    public void add(Quest q) { quests.put(q.id, q); save(); }
    public void remove(String id) { quests.remove(id); save(); }

    public ZoneId zone() { return plugin.zone(); }
    public String dailyKey() { return LocalDate.now(zone()).toString(); }
    public String weeklyKey() { LocalDate d = LocalDate.now(zone()).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); return d.toString(); }
    public long secondsToDailyReset() { return java.time.Duration.between(java.time.ZonedDateTime.now(zone()), LocalDate.now(zone()).plusDays(1).atStartOfDay(zone())).getSeconds(); }
    public long secondsToWeeklyReset() { return java.time.Duration.between(java.time.ZonedDateTime.now(zone()), LocalDate.now(zone()).with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atStartOfDay(zone())).getSeconds(); }

    /** Reinicia progreso diario/semanal/de temporada cuando cambia la clave. */
    public void checkResets(PlayerData d) {
        if (!dailyKey().equals(d.dailyKey)) { reset(d, "DAILY"); d.dailyKey = dailyKey(); plugin.data().markDirty(d); }
        if (!weeklyKey().equals(d.weeklyKey)) { reset(d, "WEEKLY"); d.weeklyKey = weeklyKey(); plugin.data().markDirty(d); }
    }
    public void reset(PlayerData d, String scope) {
        for (Quest q : quests.values()) if (q.scope.equals(scope)) { d.quests.remove(q.id); d.questsClaimed.remove(q.id); }
    }

    private boolean active(Quest q) {
        if (q.scope.equals("FEBRERO")) return plugin.february().isActive();
        return true;
    }

    public void progress(Player p, String type, int amount) {
        PlayerData d = plugin.data().get(p);
        if (d == null) return;
        checkResets(d);
        for (Quest q : quests.values()) {
            if (!q.type.equals(type) || !active(q) || d.questsClaimed.contains(q.id)) continue;
            int cur = d.quests.getOrDefault(q.id, 0);
            if (cur >= q.amount) continue;
            int now = Math.min(q.amount, cur + amount);
            d.quests.put(q.id, now);
            plugin.data().markDirty(d);
            if (now >= q.amount) p.sendMessage(Util.c(Util.PREFIX + "§a¡Misión completada: §f" + q.name + "§a! §7Reclámala en el menú de misiones."));
        }
    }

    public int progressOf(PlayerData d, Quest q) { return d.questsClaimed.contains(q.id) ? q.amount : d.quests.getOrDefault(q.id, 0); }

    public boolean claim(Player p, Quest q) {
        PlayerData d = plugin.data().get(p);
        if (d == null || d.questsClaimed.contains(q.id) || d.quests.getOrDefault(q.id, 0) < q.amount) return false;
        d.questsClaimed.add(q.id);                 // marca primero: sin duplicados
        plugin.data().markDirty(d);
        plugin.rewards().give(p, q.reward);
        return true;
    }
}
