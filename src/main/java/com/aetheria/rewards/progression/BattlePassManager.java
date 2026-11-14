package com.aetheria.rewards.progression;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.data.PlayerData;
import com.aetheria.rewards.reward.Reward;
import com.aetheria.rewards.util.Util;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/** Pase de batalla con pista gratuita y premium (permiso configurable, por defecto rewards.premium). */
public class BattlePassManager {
    private final AetheriaRewards plugin;
    public String seasonId, seasonName, premiumPermission;
    public ZonedDateTime start, end;
    public int maxLevel, baseXp, xpIncrement;
    public final Map<Integer, Reward> free = new HashMap<>();
    public final Map<Integer, Reward> premium = new HashMap<>();

    public BattlePassManager(AetheriaRewards plugin) { this.plugin = plugin; load(); }

    private File file() { return new File(plugin.getDataFolder(), "battlepass.yml"); }

    public void load() {
        free.clear(); premium.clear();
        if (!file().exists()) plugin.saveResource("battlepass.yml", false);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file());
        seasonId = y.getString("season.id", "s1");
        seasonName = y.getString("season.name", "Temporada 1");
        premiumPermission = y.getString("premium-permission", "rewards.premium");
        start = parse(y.getString("season.start", "2026-12-01T00:00"), plugin.zone());
        end = parse(y.getString("season.end", "2027-03-01T00:00"), plugin.zone());
        maxLevel = y.getInt("max-level", 30);
        baseXp = y.getInt("xp-base", 500);
        xpIncrement = y.getInt("xp-increment", 100);
        ConfigurationSection s = y.getConfigurationSection("levels");
        if (s != null) for (String k : s.getKeys(false)) {
            int lv;
            try { lv = Integer.parseInt(k); } catch (NumberFormatException e) { continue; }
            Reward f = Reward.load(s.getConfigurationSection(k + ".free")), p = Reward.load(s.getConfigurationSection(k + ".premium"));
            if (f != null) free.put(lv, f);
            if (p != null) premium.put(lv, p);
        }
    }

    static ZonedDateTime parse(String s, java.time.ZoneId z) {
        try { return LocalDateTime.parse(s).atZone(z); } catch (Exception e) { return ZonedDateTime.now(z).plusDays(60); }
    }

    public void save() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file());
        y.set("levels", null);
        for (var e : free.entrySet()) e.getValue().save(y.createSection("levels." + e.getKey() + ".free"));
        for (var e : premium.entrySet()) e.getValue().save(y.createSection("levels." + e.getKey() + ".premium"));
        try { y.save(file()); } catch (IOException ex) { plugin.getLogger().warning("battlepass.yml: " + ex.getMessage()); }
    }

    public boolean isActive() { ZonedDateTime n = ZonedDateTime.now(plugin.zone()); return n.isAfter(start) && n.isBefore(end); }
    public long secondsLeft() { return java.time.Duration.between(ZonedDateTime.now(plugin.zone()), end).getSeconds(); }
    public int xpForLevel(int level) { return baseXp + (level - 1) * xpIncrement; }   // XP para pasar del nivel anterior a 'level'
    public boolean hasPremium(Player p) { return p.hasPermission(premiumPermission); }

    /** Nivel actual (0..maxLevel) segun XP acumulada. */
    public int level(PlayerData d) {
        long xp = d.bpXp; int lv = 0;
        while (lv < maxLevel && xp >= xpForLevel(lv + 1)) { xp -= xpForLevel(lv + 1); lv++; }
        return lv;
    }
    public long xpIntoLevel(PlayerData d) {
        long xp = d.bpXp; int lv = 0;
        while (lv < maxLevel && xp >= xpForLevel(lv + 1)) { xp -= xpForLevel(lv + 1); lv++; }
        return xp;
    }

    private void seasonCheck(PlayerData d) {
        if (!seasonId.equals(d.bpSeason)) {
            d.bpSeason = seasonId; d.bpXp = 0; d.bpClaimedFree.clear(); d.bpClaimedPremium.clear();
            plugin.quests().reset(d, "SEASON");
            plugin.data().markDirty(d);
        }
    }
    public void onJoin(Player p, PlayerData d) { seasonCheck(d); }

    public void addXp(Player p, long amount) {
        PlayerData d = plugin.data().get(p);
        if (d == null || amount <= 0) return;
        seasonCheck(d);
        if (!isActive()) { p.sendMessage(Util.c(Util.PREFIX + "§7La temporada no está activa: XP no aplicada.")); return; }
        int before = level(d);
        d.bpXp += amount;
        int after = level(d);
        plugin.data().markDirty(d);
        if (after > before) {
            p.sendMessage(Util.c(Util.PREFIX + "§6¡Pase de batalla nivel §e" + after + "§6! §7Reclama tus recompensas con §e/pase"));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f);
            plugin.cosmetics().trigger(p, "VICTORY");
        }
    }

    /** premiumTrack=true -> pista premium (requiere permiso). */
    public boolean claim(Player p, int level, boolean premiumTrack) {
        PlayerData d = plugin.data().get(p);
        if (d == null) return false;
        seasonCheck(d);
        if (level < 1 || level > level(d)) return false;
        if (premiumTrack && !hasPremium(p)) { p.sendMessage(Util.c(Util.PREFIX + "§cNecesitas el pase premium.")); return false; }
        Reward r = (premiumTrack ? premium : free).get(level);
        if (r == null) return false;
        Map<Integer, Reward> m = premiumTrack ? premium : free;
        var claimed = premiumTrack ? d.bpClaimedPremium : d.bpClaimedFree;
        if (!claimed.add(level)) return false;     // ya reclamada -> sin duplicados
        plugin.data().markDirty(d);
        plugin.rewards().give(p, r);
        return true;
    }

    public int claimAll(Player p) {
        PlayerData d = plugin.data().get(p);
        if (d == null) return 0;
        int n = 0, lv = level(d);
        for (int i = 1; i <= lv; i++) {
            if (!d.bpClaimedFree.contains(i) && free.containsKey(i) && claim(p, i, false)) n++;
            if (hasPremium(p) && !d.bpClaimedPremium.contains(i) && premium.containsKey(i) && claim(p, i, true)) n++;
        }
        return n;
    }
}
