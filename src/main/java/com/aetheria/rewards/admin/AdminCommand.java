package com.aetheria.rewards.admin;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.cosmetics.CosmeticDef;
import com.aetheria.rewards.cosmetics.CosmeticMenu;
import com.aetheria.rewards.crate.Crate;
import com.aetheria.rewards.crate.Placement;
import com.aetheria.rewards.data.PlayerData;
import com.aetheria.rewards.gui.MainMenu;
import com.aetheria.rewards.progression.*;
import com.aetheria.rewards.reward.Currency;
import com.aetheria.rewards.reward.Reward;
import com.aetheria.rewards.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** AdminManager: comandos de jugador y de administracion. */
public class AdminCommand implements CommandExecutor, TabCompleter {
    private final AetheriaRewards plugin;
    public AdminCommand(AetheriaRewards plugin) { this.plugin = plugin; }

    private void msg(CommandSender s, String m) { s.sendMessage(Util.c(Util.PREFIX + m)); }

    @Override public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        String n = c.getName().toLowerCase();
        if (n.equals("aeadmin")) return admin(s, a);
        if (!(s instanceof Player p)) { msg(s, "Solo jugadores."); return true; }
        if (!p.hasPermission("rewards.use")) { msg(s, "§cSin permiso."); return true; }
        switch (n) {
            case "rewards" -> new MainMenu(plugin, p).open();
            case "cosmeticos" -> {
                if (a.length >= 2 && a[0].equalsIgnoreCase("probar")) {
                    CosmeticDef d = plugin.cosmetics().get(a[1]);
                    if (d == null || (!d.published && !p.hasPermission("rewards.admin"))) { msg(s, "§cCosmético no encontrado."); return true; }
                    plugin.cosmetics().preview(p, d);
                } else new CosmeticMenu(plugin, p).open();
            }
            case "pase" -> new BattlePassMenu(plugin, p).open();
            case "adviento" -> new AdventMenu(plugin, p).open();
            case "febrero" -> new FebruaryMenu(plugin, p).open();
        }
        return true;
    }

    private boolean admin(CommandSender s, String[] a) {
        if (!s.hasPermission("rewards.admin")) { msg(s, "§cSin permiso."); return true; }
        if (a.length == 0 || a[0].equalsIgnoreCase("menu")) {
            if (s instanceof Player p) new AdminMenu(plugin, p).open(); else msg(s, "Usa subcomandos: help");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "help" -> {
                for (String l : new String[]{"menu", "givekey <jugador> <crate> [n]", "givecoin <jugador> <n>", "unlock <jugador> <cosmético|all>",
                    "lock <jugador> <cosmético>", "xp <jugador> <n>", "victory <jugador>", "place <crate>", "hidden <crate>", "remove <id>", "rotate <id> <yaw>",
                    "list", "reset <jugador> <advent|pase|misiones|febrero>", "reload"}) s.sendMessage(Util.c("§6/aeadmin §f" + l));
            }
            case "reload" -> { plugin.reloadAll(); msg(s, "§aRecargado."); }
            case "list" -> {
                msg(s, "Crates: §f" + String.join(", ", plugin.crates().all().stream().map(x -> x.id).toList()));
                for (Placement p : plugin.crates().placements()) s.sendMessage(Util.c("§8 - §f" + p.id + " §7" + p.type + " " + p.world + " " + (int) p.x + "," + (int) p.y + "," + (int) p.z));
            }
            case "givekey" -> {
                Player t = target(s, a, 1); if (t == null || a.length < 3) return true;
                if (plugin.crates().get(a[2]) == null) { msg(s, "§cCrate inexistente."); return true; }
                int n = a.length > 3 ? parse(a[3], 1) : 1;
                plugin.rewards().give(t, Reward.key(a[2], n)); msg(s, "Entregadas " + n + " llaves a " + t.getName());
            }
            case "givecoin" -> {
                Player t = target(s, a, 1); if (t == null || a.length < 3) return true;
                Currency.give(plugin, t, parse(a[2], 1)); msg(s, "Coronas entregadas.");
            }
            case "unlock" -> {
                Player t = target(s, a, 1); if (t == null || a.length < 3) return true;
                PlayerData d = plugin.data().get(t); if (d == null) return true;
                if (a[2].equalsIgnoreCase("all")) { for (CosmeticDef x : plugin.cosmetics().all()) d.unlocked.add(x.id); plugin.data().markDirty(d); msg(s, "Todos desbloqueados."); }
                else if (plugin.cosmetics().get(a[2]) != null) { d.unlocked.add(a[2]); plugin.data().markDirty(d); msg(s, "Desbloqueado."); }
                else msg(s, "§cCosmético inexistente.");
            }
            case "lock" -> {
                Player t = target(s, a, 1); if (t == null || a.length < 3) return true;
                PlayerData d = plugin.data().get(t); if (d == null) return true;
                d.unlocked.remove(a[2]);
                CosmeticDef def = plugin.cosmetics().get(a[2]);
                if (def != null && a[2].equals(d.equipped.get(def.slot))) plugin.cosmetics().unequip(t, def.slot);
                plugin.data().markDirty(d); msg(s, "Bloqueado.");
            }
            case "xp" -> { Player t = target(s, a, 1); if (t == null || a.length < 3) return true; plugin.battlePass().addXp(t, parse(a[2], 0)); msg(s, "XP añadida."); }
            case "victory" -> { Player t = target(s, a, 1); if (t != null) plugin.cosmetics().trigger(t, "VICTORY"); }
            case "place", "hidden" -> {
                if (!(s instanceof Player p) || a.length < 2) { msg(s, "Uso: /aeadmin " + a[0] + " <crate> (como jugador)"); return true; }
                Crate cr = plugin.crates().get(a[1]); if (cr == null) { msg(s, "§cCrate inexistente."); return true; }
                var loc = p.getLocation(); loc.setYaw(loc.getYaw() + 180);
                Placement pl = plugin.crates().place(cr, loc, a[0].equalsIgnoreCase("hidden") ? Placement.Type.HIDDEN : Placement.Type.CRATE);
                msg(s, "Colocado: §f" + pl.id);
            }
            case "remove" -> { if (a.length < 2 || plugin.crates().placement(a[1]) == null) { msg(s, "§cId inexistente."); return true; } plugin.crates().removePlacement(a[1]); msg(s, "Eliminado."); }
            case "rotate" -> {
                Placement pl = a.length < 3 ? null : plugin.crates().placement(a[1]);
                if (pl == null) { msg(s, "Uso: /aeadmin rotate <id> <yaw>"); return true; }
                plugin.crates().rotate(pl, (float) parse(a[2], 0)); msg(s, "Rotado.");
            }
            case "reset" -> {
                Player t = target(s, a, 1); if (t == null || a.length < 3) return true;
                PlayerData d = plugin.data().get(t); if (d == null) return true;
                switch (a[2].toLowerCase()) {
                    case "advent" -> d.adventClaimed.clear();
                    case "pase" -> { d.bpXp = 0; d.bpClaimedFree.clear(); d.bpClaimedPremium.clear(); }
                    case "misiones" -> { d.quests.clear(); d.questsClaimed.clear(); }
                    case "febrero" -> d.februaryClaimed.clear();
                    default -> { msg(s, "§cOpción inválida."); return true; }
                }
                plugin.data().markDirty(d); msg(s, "Reiniciado.");
            }
            default -> msg(s, "§cSubcomando desconocido. /aeadmin help");
        }
        return true;
    }

    private Player target(CommandSender s, String[] a, int i) {
        if (a.length <= i) { msg(s, "§cFalta el jugador."); return null; }
        Player t = Bukkit.getPlayerExact(a[i]);
        if (t == null) msg(s, "§cJugador no conectado.");
        return t;
    }
    private int parse(String s, int def) { try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; } }

    @Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> out = new ArrayList<>();
        if (c.getName().equalsIgnoreCase("cosmeticos") && a.length == 1) out.add("probar");
        if (c.getName().equalsIgnoreCase("cosmeticos") && a.length == 2) plugin.cosmetics().all().forEach(d -> out.add(d.id));
        if (!c.getName().equalsIgnoreCase("aeadmin") || !s.hasPermission("rewards.admin")) return filter(out, a);
        if (a.length == 1) out.addAll(List.of("menu", "help", "givekey", "givecoin", "unlock", "lock", "xp", "victory", "place", "hidden", "remove", "rotate", "list", "reset", "reload"));
        else if (a.length == 2) {
            switch (a[0].toLowerCase()) {
                case "givekey", "givecoin", "unlock", "lock", "xp", "victory", "reset" -> Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
                case "place", "hidden" -> plugin.crates().all().forEach(x -> out.add(x.id));
                case "remove", "rotate" -> plugin.crates().placements().forEach(x -> out.add(x.id));
            }
        } else if (a.length == 3) {
            switch (a[0].toLowerCase()) {
                case "givekey" -> plugin.crates().all().forEach(x -> out.add(x.id));
                case "unlock", "lock" -> { out.add("all"); plugin.cosmetics().all().forEach(d -> out.add(d.id)); }
                case "reset" -> out.addAll(List.of("advent", "pase", "misiones", "febrero"));
            }
        }
        return filter(out, a);
    }
    private List<String> filter(List<String> l, String[] a) {
        String p = a.length == 0 ? "" : a[a.length - 1].toLowerCase();
        l.removeIf(x -> !x.toLowerCase().startsWith(p));
        return l;
    }
}
