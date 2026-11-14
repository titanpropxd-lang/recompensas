package com.aetheria.rewards.gui;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Base de todas las GUIs: mismo marco morado/dorado/negro. */
public abstract class Menu implements InventoryHolder {
    protected final AetheriaRewards plugin;
    protected final Player player;
    protected final int rows;
    protected final String title;
    protected Inventory inv;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected Menu(AetheriaRewards plugin, Player player, int rows, String title) {
        this.plugin = plugin; this.player = player; this.rows = rows; this.title = title;
        this.inv = Bukkit.createInventory(this, rows * 9, MenuSkin.title(plugin, theme(), rows, title));
    }

    protected abstract void build();
    /** Tema visual del fondo: main | advent | feb. */
    protected String theme() { return "main"; }
    public void onClose() {}

    public void open() { render(); player.openInventory(inv); }
    public void refresh() { render(); }

    private void render() {
        inv.clear(); actions.clear();
        frame();
        build();
    }

    /** Marco: negro de fondo, borde morado y esquinas doradas. */
    protected void frame() {
        if (plugin.skin()) { ItemStack blank = Util.blank(); for (int i = 0; i < rows * 9; i++) inv.setItem(i, blank); return; }
        ItemStack black = Util.item(Material.BLACK_STAINED_GLASS_PANE, "§r");
        ItemStack purple = Util.item(Material.PURPLE_STAINED_GLASS_PANE, "§r");
        ItemStack gold = Util.item(Material.YELLOW_STAINED_GLASS_PANE, "§r");
        int n = rows * 9;
        for (int i = 0; i < n; i++) {
            int r = i / 9, c = i % 9;
            boolean edge = r == 0 || r == rows - 1 || c == 0 || c == 8;
            boolean corner = (r == 0 || r == rows - 1) && (c == 0 || c == 8);
            inv.setItem(i, corner ? gold : edge ? purple : black);
        }
    }

    protected void set(int slot, ItemStack it, Consumer<InventoryClickEvent> action) {
        inv.setItem(slot, it);
        if (action != null) actions.put(slot, action); else actions.remove(slot);
    }
    protected void set(int slot, ItemStack it) { set(slot, it, null); }

    protected void clearInner(int fromSlot, int toSlot) {
        for (int i = fromSlot; i <= toSlot; i++) { inv.setItem(i, null); actions.remove(i); }
    }

    /** Boton con icono ornamental del resource pack (CMD) si 'gui-skin' esta activo; si no, el item vanilla. */
    protected ItemStack ui(int cmd, Material fallback, String name, String... lore) {
        if (!plugin.skin()) return Util.item(fallback, name, lore);
        return Util.item(Material.PAPER, cmd, name, java.util.List.of(lore));
    }
    protected void back(int slot, Runnable r) {
        set(slot, ui(9001, Material.ARROW, "§e« Volver"), e -> r.run());
    }
    protected void close(int slot) {
        set(slot, ui(9002, Material.BARRIER, "§c✖ Cerrar"), e -> player.closeInventory());
    }

    public void click(InventoryClickEvent e) {
        Consumer<InventoryClickEvent> a = actions.get(e.getRawSlot());
        if (a != null) a.accept(e);
    }

    @Override public Inventory getInventory() { return inv; }
}
