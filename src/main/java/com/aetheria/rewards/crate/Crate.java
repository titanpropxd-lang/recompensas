package com.aetheria.rewards.crate;

import com.aetheria.rewards.reward.Reward;
import com.aetheria.rewards.util.Rarity;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Crate {
    public static class Entry {
        public Reward reward; public double weight; public Rarity rarity;
        public Entry(Reward r, double w, Rarity ra) { reward = r; weight = w; rarity = ra; }
    }
    public String id, name, model, fallbackBlock = "BARREL";
    public Rarity rarity = Rarity.COMMON;
    public double scale = 1.0;
    public String keyName; public int keyCmd = 3001; public Material keyMaterial = Material.AMETHYST_SHARD;
    public final List<Entry> rewards = new ArrayList<>();

    public double totalWeight() { double t = 0; for (Entry e : rewards) t += Math.max(0, e.weight); return t; }
    public double chance(Entry e) { double t = totalWeight(); return t <= 0 ? 0 : Math.max(0, e.weight) / t * 100.0; }

    public Entry roll() {
        double t = totalWeight();
        if (t <= 0) return null;
        double r = ThreadLocalRandom.current().nextDouble() * t, acc = 0;
        for (Entry e : rewards) { acc += Math.max(0, e.weight); if (r < acc) return e; }
        return rewards.get(rewards.size() - 1);
    }
}
