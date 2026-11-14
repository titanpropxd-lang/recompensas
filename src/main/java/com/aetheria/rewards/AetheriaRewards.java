package com.aetheria.rewards;

import com.aetheria.rewards.admin.AdminCommand;
import com.aetheria.rewards.cosmetics.CosmeticListener;
import com.aetheria.rewards.cosmetics.CosmeticManager;
import com.aetheria.rewards.crate.ChestExplorerManager;
import com.aetheria.rewards.crate.CrateListener;
import com.aetheria.rewards.crate.CrateManager;
import com.aetheria.rewards.data.PlayerData;
import com.aetheria.rewards.data.PlayerDataManager;
import com.aetheria.rewards.gui.GUIManager;
import com.aetheria.rewards.model.ModelManager;
import com.aetheria.rewards.progression.*;
import com.aetheria.rewards.reward.RewardService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.ZoneId;

/** Plugin unico: crates, llaves, cosmeticos, pase de batalla, adviento, febrero y administracion. */
public class AetheriaRewards extends JavaPlugin {
    private PlayerDataManager data;
    private ModelManager models;
    private RewardService rewards;
    private GUIManager gui;
    private CosmeticManager cosmetics;
    private CrateManager crates;
    private ChestExplorerManager explorer;
    private QuestManager quests;
    private BattlePassManager battlePass;
    private AdventCalendarManager advent;
    private FebruaryEventManager february;
    private ZoneId zone;

    @Override public void onEnable() {
        saveDefaultConfig();
        zone = readZone();
        data = new PlayerDataManager(this);
        models = new ModelManager(this);
        rewards = new RewardService(this);
        gui = new GUIManager(this);
        cosmetics = new CosmeticManager(this);
        crates = new CrateManager(this);
        explorer = new ChestExplorerManager(this);
        quests = new QuestManager(this);
        battlePass = new BattlePassManager(this);
        advent = new AdventCalendarManager(this);
        february = new FebruaryEventManager(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(data, this);
        pm.registerEvents(gui, this);
        pm.registerEvents(new CosmeticListener(this), this);
        pm.registerEvents(new CrateListener(this), this);
        pm.registerEvents(new QuestListener(this), this);

        AdminCommand cmd = new AdminCommand(this);
        for (String name : new String[]{"rewards", "cosmeticos", "pase", "adviento", "febrero", "aeadmin"}) {
            PluginCommand pc = getCommand(name);
            if (pc != null) { pc.setExecutor(cmd); pc.setTabCompleter(cmd); }
        }
        // los modelos de BetterModel cargan de forma asincrona: se crea lo visual con retraso
        Bukkit.getScheduler().runTaskLater(this, () -> crates.spawnAll(), 80L);
        // recarga en caliente: jugadores ya conectados
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = data.get(p);
            if (d != null) onPlayerLoaded(p, d);
        }
        getLogger().info("AetheriaRewards listo. Zona horaria: " + zone);
    }

    @Override public void onDisable() {
        if (crates != null) { crates.finishAllSessions(); crates.despawnAll(); }
        if (cosmetics != null) cosmetics.shutdown();
        if (data != null) data.saveAllSync();
    }

    private ZoneId readZone() {
        try { return ZoneId.of(getConfig().getString("timezone", "America/Mexico_City")); }
        catch (Exception e) { getLogger().warning("Zona horaria invalida; se usa UTC."); return ZoneId.of("UTC"); }
    }

    public void reloadAll() {
        reloadConfig();
        zone = readZone();
        cosmetics.load();
        crates.loadCrates();
        quests.load();
        battlePass.load();
        advent.load();
        february.load();
    }

    /** Se llama cuando los datos del jugador ya estan en memoria. */
    public void onPlayerLoaded(Player p, PlayerData d) {
        quests.checkResets(d);
        battlePass.onJoin(p, d);
        cosmetics.onJoin(p, d);
        Bukkit.getScheduler().runTaskLater(this, () -> { if (p.isOnline()) rewards.flushPending(p); }, 40L);
    }
    public void onPlayerUnload(Player p) { cosmetics.onQuit(p); }

    public PlayerDataManager data() { return data; }
    public ModelManager models() { return models; }
    public RewardService rewards() { return rewards; }
    public GUIManager gui() { return gui; }
    public CosmeticManager cosmetics() { return cosmetics; }
    public CrateManager crates() { return crates; }
    public ChestExplorerManager explorer() { return explorer; }
    public QuestManager quests() { return quests; }
    public BattlePassManager battlePass() { return battlePass; }
    public AdventCalendarManager advent() { return advent; }
    public FebruaryEventManager february() { return february; }
    public ZoneId zone() { return zone; }
    /** true si las GUIs usan el fondo ornamental del resource pack. */
    public boolean skin() { return getConfig().getBoolean("gui-skin", false); }
}
