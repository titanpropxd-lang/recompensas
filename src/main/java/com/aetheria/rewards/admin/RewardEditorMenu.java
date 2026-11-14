package com.aetheria.rewards.admin;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.crate.Crate;
import com.aetheria.rewards.gui.Menu;
import com.aetheria.rewards.reward.Reward;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/** Editor universal de una recompensa (objeto en mano, monedas, cosmetico, llave, XP, comando). */
public class RewardEditorMenu extends Menu {
    private Reward cur;
    private final Consumer<Reward> onSave; private final Runnable back;

    public RewardEditorMenu(AetheriaRewards plugin, Player p, Reward initial, Consumer<Reward> onSave, Runnable back) {
        super(plugin, p, 5, "Editor de Recompensa");
        this.cur = initial == null ? Reward.corona(10) : initial.copy();
        this.onSave = onSave; this.back = back;
    }

    @Override protected void build() {
        set(13, Util.glow(cur.icon(plugin)), null);
        set(4, Util.item(Material.PAPER, "§6Recompensa actual", cur.describe(plugin), "§8Tipo: " + cur.type.name()), null);

        set(19, Util.item(Material.CHEST, "§e📦 Usar objeto de mi mano", "§7Copia el objeto exacto (nombre,", "§7lore, encantamientos, modelo)."), e -> {
            ItemStack h = player.getInventory().getItemInMainHand();
            if (h.getType().isAir()) { player.sendMessage(Util.c(Util.PREFIX + "§cTen un objeto en la mano.")); return; }
            cur = Reward.item(h); refresh();
        });
        set(20, Util.item(Material.GOLD_NUGGET, "§6👑 Coronas Reales", "§7Moneda del reino (datapack)."), e -> { cur = Reward.corona(Math.max(1, cur.type == Reward.Type.CORONA ? cur.amount : 10)); refresh(); });
        set(21, Util.item(Material.AMETHYST_SHARD, "§d✦ Cosmético", "§7Elegir del catálogo."), e ->
            new CosmeticPickerMenu(plugin, player, def -> { cur = Reward.cosmetic(def.id); new RewardEditorMenu(plugin, player, cur, onSave, back).open(); },
                    () -> new RewardEditorMenu(plugin, player, cur, onSave, back).open()).open());
        set(22, Util.item(Material.TRIPWIRE_HOOK, "§e🗝 Llave de cofre", "§7Elegir crate."), e -> pickCrate());
        set(23, Util.item(Material.EXPERIENCE_BOTTLE, "§b✦ XP del pase", "§7Experiencia de pase de batalla."), e -> { cur = Reward.xp(cur.type == Reward.Type.XP ? cur.amount : 100); refresh(); });
        set(24, Util.item(Material.COMMAND_BLOCK, "§a⚙ Recompensa especial", "§7Comando de consola.", "§8%player% = nombre del jugador"), e ->
            plugin.gui().prompt(player, "Escribe el comando (sin /). Usa %player%:", t -> {
                plugin.gui().prompt(player, "Escribe el texto a mostrar al jugador:", d -> { cur = Reward.command(t, d); new RewardEditorMenu(plugin, player, cur, onSave, back).open(); });
            }));

        if (cur.type == Reward.Type.CORONA || cur.type == Reward.Type.KEY || cur.type == Reward.Type.XP || cur.type == Reward.Type.ITEM) {
            set(30, Util.item(Material.RED_STAINED_GLASS_PANE, "§c- Cantidad", "§7Clic: -1  •  Shift: -10"), e -> { cur.amount = Math.max(1, cur.amount - (e.isShiftClick() ? 10 : 1)); syncItem(); refresh(); });
            set(31, Util.item(Material.PAPER, "§fCantidad: §e" + cur.amount), null);
            set(32, Util.item(Material.LIME_STAINED_GLASS_PANE, "§a+ Cantidad", "§7Clic: +1  •  Shift: +10"), e -> { cur.amount += (e.isShiftClick() ? 10 : 1); syncItem(); refresh(); });
        }
        back(36, back);
        set(40, Util.glow(Util.item(Material.EMERALD, "§a§l✔ Guardar", "§7Aplica esta recompensa.")), e -> { onSave.accept(cur.copy()); back.run(); });
        close(44);
    }
    private void syncItem() { if (cur.type == Reward.Type.ITEM && cur.item != null) cur.item.setAmount(Math.max(1, Math.min(cur.item.getMaxStackSize(), cur.amount))); }

    private void pickCrate() {
        new Menu(plugin, player, 4, "Elegir llave") {
            @Override protected void build() {
                int s = 10;
                for (Crate c : plugin.crates().all()) {
                    if (s >= 17 && s < 19) s = 19;
                    if (s > 25) break;
                    set(s++, plugin.crates().keyItem(c.id, 1), ev -> { cur = Reward.key(c.id, cur.type == Reward.Type.KEY ? cur.amount : 1); new RewardEditorMenu(plugin, player, cur, onSave, back).open(); });
                }
                back(27, () -> new RewardEditorMenu(plugin, player, cur, onSave, back).open());
            }
        }.open();
    }
}
