package com.aetheria.rewards.gui;

import com.aetheria.rewards.AetheriaRewards;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Listener unico de GUIs + entrada de texto por chat (para editores de admin). */
public class GUIManager implements Listener {
    private final AetheriaRewards plugin;
    private final Map<UUID, Consumer<String>> prompts = new ConcurrentHashMap<>();

    public GUIManager(AetheriaRewards plugin) { this.plugin = plugin; }

    public void prompt(Player p, String message, Consumer<String> handler) {
        p.closeInventory();
        p.sendMessage(com.aetheria.rewards.util.Util.c(com.aetheria.rewards.util.Util.PREFIX + message + " §8(escribe §ccancelar §8para salir)"));
        prompts.put(p.getUniqueId(), handler);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Consumer<String> h = prompts.remove(e.getPlayer().getUniqueId());
        if (h == null) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (text.equalsIgnoreCase("cancelar")) { e.getPlayer().sendMessage("§cCancelado."); return; }
            h.accept(text);
        });
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getView().getTopInventory().getHolder();
        if (!(holder instanceof Menu menu)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;
        menu.click(e);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Menu) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof Menu m) m.onClose();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { prompts.remove(e.getPlayer().getUniqueId()); }
}
