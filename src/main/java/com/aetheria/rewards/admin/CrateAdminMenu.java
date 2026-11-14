package com.aetheria.rewards.admin;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.crate.Crate;
import com.aetheria.rewards.crate.CratePreviewMenu;
import com.aetheria.rewards.crate.Placement;
import com.aetheria.rewards.gui.Menu;
import com.aetheria.rewards.reward.Reward;
import com.aetheria.rewards.util.Rarity;
import com.aetheria.rewards.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class CrateAdminMenu extends Menu {
    public CrateAdminMenu(AetheriaRewards plugin, Player p) { super(plugin, p, 5, "Crates"); }

    @Override protected void build() {
        int s = 10;
        for (Crate c : plugin.crates().all()) {
            if (s == 17) s = 19;
            if (s > 34) break;
            if (s % 9 == 8) s += 2;
            ItemStack it = plugin.crates().keyItem(c.id, 1);
            var m = it.getItemMeta();
            m.displayName(Util.c(c.name));
            m.lore(java.util.List.of(Util.c(c.rarity.tag()), Util.c("§7Modelo: §f" + c.model), Util.c("§7Recompensas: §f" + c.rewards.size()), Util.c(""), Util.c("§e▶ Clic para editar")));
            it.setItemMeta(m);
            set(s++, it, e -> new CrateEditMenu(plugin, player, c).open());
        }
        set(40, Util.glow(Util.item(Material.EMERALD, "§a§l+ Nuevo crate", "§7Crea un crate vacío.")), e ->
            plugin.gui().prompt(player, "Escribe un id (sin espacios, ej: halloween):", t -> {
                String id = t.toLowerCase().replaceAll("[^a-z0-9_]", "");
                if (id.isEmpty() || plugin.crates().get(id) != null) { player.sendMessage(Util.c("§cId inválido o repetido.")); return; }
                new CrateEditMenu(plugin, player, plugin.crates().createCrate(id, id)).open();
            }));
        back(36, () -> new AdminMenu(plugin, player).open());
        close(44);
    }
}

class CrateEditMenu extends Menu {
    private final Crate c;
    CrateEditMenu(AetheriaRewards plugin, Player p, Crate c) { super(plugin, p, 5, c.id); this.c = c; }

    @Override protected void build() {
        set(4, Util.glow(plugin.crates().keyItem(c.id, 1)), null);
        set(19, Util.item(Material.CHEST, "§e§lRecompensas y probabilidades", "§7" + c.rewards.size() + " recompensas"), e -> new CrateRewardsMenu(plugin, player, c).open());
        set(20, Util.item(Material.SPYGLASS, "§bVista previa", "§7Como la ven los jugadores."), e -> new CratePreviewMenu(plugin, player, c, () -> new CrateEditMenu(plugin, player, c).open()).open());
        set(21, Util.item(Material.NAME_TAG, "§fNombre: " + c.name, "§7Clic para cambiar"), e -> plugin.gui().prompt(player, "Nuevo nombre (admite &):", t -> { c.name = t.replace('&', '§'); plugin.crates().saveCrates(); new CrateEditMenu(plugin, player, c).open(); }));
        set(22, Util.item(Material.AMETHYST_SHARD, "§fRareza: " + c.rarity.tag(), "§7Clic para cambiar"), e -> { c.rarity = Rarity.values()[(c.rarity.ordinal() + 1) % Rarity.values().length]; plugin.crates().saveCrates(); refresh(); });
        set(23, Util.item(Material.PAINTING, "§fModelo: §e" + c.model, "§7Nombre del .bbmodel en BetterModel", "§7Clic para cambiar"), e -> plugin.gui().prompt(player, "Nombre del modelo (ej: crate_magico):", t -> { c.model = t; plugin.crates().saveCrates(); new CrateEditMenu(plugin, player, c).open(); }));
        set(24, Util.item(Material.ANVIL, "§fLlave: §e" + c.keyName, "§7Clic: renombrar"), e -> plugin.gui().prompt(player, "Nombre de la llave:", t -> { c.keyName = t; plugin.crates().saveCrates(); new CrateEditMenu(plugin, player, c).open(); }));
        set(25, Util.item(Material.TRIPWIRE_HOOK, "§e🗝 Darme una llave"), e -> plugin.rewards().give(player, Reward.key(c.id, 1)));
        set(30, Util.item(Material.DISPENSER, "§a📍 Colocar cofre aquí", "§7Crate normal con modelo y animaciones."), e -> { plugin.crates().place(c, player.getLocation().add(0, 0, 0).setDirection(player.getLocation().getDirection().multiply(-1)), Placement.Type.CRATE); player.sendMessage(Util.c(Util.PREFIX + "§aCofre colocado.")); });
        set(31, Util.item(Material.ENDER_EYE, "§5👁 Colocar cofre oculto aquí", "§7Exploración: 1 premio por jugador."), e -> { plugin.crates().place(c, player.getLocation().setDirection(player.getLocation().getDirection().multiply(-1)), Placement.Type.HIDDEN); player.sendMessage(Util.c(Util.PREFIX + "§aCofre oculto colocado.")); });
        set(32, Util.item(Material.TNT, "§c✖ Eliminar crate", "§7Shift+clic para confirmar"), e -> { if (e.isShiftClick()) { plugin.crates().deleteCrate(c.id); new CrateAdminMenu(plugin, player).open(); } });
        back(36, () -> new CrateAdminMenu(plugin, player).open());
        close(44);
    }
}

class CrateRewardsMenu extends Menu {
    private final Crate c; private int page = 0;
    private static final int[] SLOTS = new int[28];
    static { int i = 0; for (int r = 1; r <= 4; r++) for (int col = 1; col <= 7; col++) SLOTS[i++] = r * 9 + col; }
    CrateRewardsMenu(AetheriaRewards plugin, Player p, Crate c) { super(plugin, p, 6, c.id + " - Recompensas"); this.c = c; }

    @Override protected void build() {
        int pages = Math.max(1, (c.rewards.size() + SLOTS.length - 1) / SLOTS.length);
        for (int i = 0; i < SLOTS.length; i++) {
            int idx = page * SLOTS.length + i;
            if (idx >= c.rewards.size()) break;
            Crate.Entry en = c.rewards.get(idx);
            ItemStack ic = en.reward.icon(plugin);
            var m = ic.getItemMeta();
            java.util.List<net.kyori.adventure.text.Component> l = m.hasLore() ? new java.util.ArrayList<>(m.lore()) : new java.util.ArrayList<>();
            l.add(Util.c("")); l.add(Util.c(en.rarity.tag() + " §8• §7Peso: §e" + en.weight + " §8(" + String.format("%.2f", c.chance(en)) + "%)"));
            l.add(Util.c("§7Clic: §a+1 §8• §7Derecho: §c-1")); l.add(Util.c("§7Shift: §a±10")); l.add(Util.c("§7F: cambiar rareza"));
            l.add(Util.c("§7Q: editar recompensa  •  Ctrl+Q: eliminar"));
            m.lore(l); ic.setItemMeta(m);
            set(SLOTS[i], ic, e -> {
                ClickType t = e.getClick();
                if (t == ClickType.DROP) { new RewardEditorMenu(plugin, player, en.reward, r -> { en.reward = r; plugin.crates().saveCrates(); }, () -> new CrateRewardsMenu(plugin, player, c).open()).open(); return; }
                if (t == ClickType.CONTROL_DROP) { c.rewards.remove(en); plugin.crates().saveCrates(); refresh(); return; }
                if (t == ClickType.SWAP_OFFHAND) { en.rarity = Rarity.values()[(en.rarity.ordinal() + 1) % Rarity.values().length]; plugin.crates().saveCrates(); refresh(); return; }
                double step = e.isShiftClick() ? 10 : 1;
                en.weight = Math.max(0, en.weight + (e.isRightClick() ? -step : step));
                plugin.crates().saveCrates(); refresh();
            });
        }
        set(49, Util.glow(Util.item(Material.EMERALD, "§a§l+ Añadir recompensa")), e ->
            new RewardEditorMenu(plugin, player, null, r -> { c.rewards.add(new Crate.Entry(r, 10, Rarity.COMMON)); plugin.crates().saveCrates(); }, () -> new CrateRewardsMenu(plugin, player, c).open()).open());
        if (page > 0) set(45, ui(9003, Material.ARROW, "§e« Anterior"), e -> { page--; refresh(); }); else back(45, () -> new CrateEditMenu(plugin, player, c).open());
        if (page + 1 < pages) set(53, ui(9004, Material.ARROW, "§eSiguiente »"), e -> { page++; refresh(); }); else close(53);
    }
}
