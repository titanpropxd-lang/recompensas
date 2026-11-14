package com.aetheria.rewards.crate;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.gui.Menu;
import com.aetheria.rewards.util.Rarity;
import com.aetheria.rewards.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Ruleta: una tira de 9 recompensas que se desplaza y desacelera hasta detenerse en el premio ya decidido. */
public class RouletteMenu extends Menu {
    private final Crate crate; private final Crate.Entry result; private final Placement placement;
    private final List<Crate.Entry> strip = new ArrayList<>();
    private static final int STEPS = 34;
    private int pos = 0, stepDelay = 2, wait = 0;
    private boolean done = false;
    private BukkitTask task;

    public RouletteMenu(AetheriaRewards plugin, Player p, Crate c, Crate.Entry result, Placement pl) {
        super(plugin, p, 3, c.name.replaceAll("§.", ""));
        this.crate = c; this.result = result; this.placement = pl;
        for (int i = 0; i < STEPS + 9; i++) {
            Crate.Entry e = c.rewards.get(ThreadLocalRandom.current().nextInt(c.rewards.size()));
            strip.add(e);
        }
        strip.set(STEPS + 4, result);   // el centro de la ventana final
    }

    @Override protected void build() {
        ItemStack tint = Util.item(rarityPane(crate.rarity), "§r");
        for (int i = 0; i < 9; i++) { set(i, tint); set(18 + i, tint); }
        set(4, Util.item(Material.HOPPER, "§6§l▼ Tu premio ▼"));
        set(22, Util.item(Material.END_ROD, "§6§l▲"));
        render();
    }

    private Material rarityPane(Rarity r) {
        return switch (r) {
            case COMMON -> Material.GRAY_STAINED_GLASS_PANE; case RARE -> Material.BLUE_STAINED_GLASS_PANE;
            case EPIC -> Material.PURPLE_STAINED_GLASS_PANE; case LEGENDARY -> Material.YELLOW_STAINED_GLASS_PANE;
            default -> Material.MAGENTA_STAINED_GLASS_PANE;
        };
    }

    private void render() {
        for (int i = 0; i < 9; i++) {
            Crate.Entry e = strip.get(pos + i);
            ItemStack ic = e.reward.icon(plugin);
            var m = ic.getItemMeta();
            if (i == 4) { ic = Util.glow(ic); }
            set(9 + i, ic);
        }
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (done) return;
            if (++wait < stepDelay) return;
            wait = 0;
            if (pos >= STEPS) { complete(); return; }
            pos++;
            render();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f + Math.min(1f, pos / (float) STEPS));
            if (pos > STEPS - 12) stepDelay = 3 + (pos - (STEPS - 12));       // desaceleracion
            else if (pos > STEPS - 20) stepDelay = 3;
        }, 1L, 1L);
    }

    private void complete() {
        if (done) return;
        done = true;
        if (task != null) task.cancel();
        render();
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 1.2f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.crates().finish(player, this, placement, crate, result);
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (player.getOpenInventory().getTopInventory().getHolder() == this) player.closeInventory(); }, 35L);
        }, 12L);
    }

    /** Cierre prematuro, desconexion o apagado: se entrega el premio de inmediato. */
    public void forceFinish() {
        if (!done) { done = true; if (task != null) task.cancel(); }
        plugin.crates().finish(player, this, placement, crate, result);
    }

    @Override public void onClose() {
        if (!done) forceFinish();   // cerrar el menu no evita ni duplica el premio
    }
}
