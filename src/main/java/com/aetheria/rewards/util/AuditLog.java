package com.aetheria.rewards.util;

import com.aetheria.rewards.AetheriaRewards;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Registro de auditoria (aperturas, recompensas, canjes, acciones de admin). Escritura asincrona, un archivo por dia. */
public class AuditLog {
    private final AetheriaRewards plugin;
    private final File dir;
    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AuditLog(AetheriaRewards plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "logs");
        dir.mkdirs();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flush, 100L, 100L);
    }

    public File dir() { return dir; }

    public void log(String type, Player p, String detail) {
        queue.add(ZonedDateTime.now(plugin.zone()).format(TS) + " | " + type + " | " + p.getName() + " | " + p.getUniqueId() + " | " + Util.strip(detail));
    }
    public void logRaw(String type, String actor, String detail) {
        queue.add(ZonedDateTime.now(plugin.zone()).format(TS) + " | " + type + " | " + actor + " | - | " + Util.strip(detail));
    }

    private File today() { return new File(dir, "audit-" + LocalDate.now(plugin.zone()) + ".log"); }

    public synchronized void flush() {
        if (queue.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = queue.poll()) != null) sb.append(line).append('\n');
        try {
            Files.writeString(today().toPath(), sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo escribir el registro de auditoria: " + e.getMessage());
        }
    }

    /** Muestra las ultimas 'n' lineas que mencionan a 'who' (nombre o uuid), buscando en los registros mas recientes. */
    public void tail(CommandSender to, String who, int n) {
        flush();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> out = new ArrayList<>();
            File[] files = dir.listFiles((d, name) -> name.startsWith("audit-") && name.endsWith(".log"));
            if (files != null) {
                java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName).reversed());
                for (File f : files) {
                    try {
                        List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
                        for (int i = lines.size() - 1; i >= 0 && out.size() < n; i--)
                            if (lines.get(i).toLowerCase().contains(who.toLowerCase())) out.add(lines.get(i));
                    } catch (IOException ignored) {}
                    if (out.size() >= n) break;
                }
            }
            java.util.Collections.reverse(out);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (out.isEmpty()) to.sendMessage(Util.c(Util.PREFIX + "§7Sin registros para §f" + who));
                else { to.sendMessage(Util.c(Util.PREFIX + "§7Últimos " + out.size() + " registros de §f" + who + "§7:")); for (String l : out) to.sendMessage(Util.c("§8" + l)); }
            });
        });
    }
}
