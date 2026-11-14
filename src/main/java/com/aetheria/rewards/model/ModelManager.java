package com.aetheria.rewards.model;

import com.aetheria.rewards.AetheriaRewards;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * ModelManager: puente con BetterModel + instalacion de los .bbmodel incluidos en el plugin.
 * Si BetterModel no esta, spawn() devuelve null y los sistemas usan su respaldo (particulas / BlockDisplay).
 */
public class ModelManager {
    public interface Backend { boolean has(String model); ModelHandle spawn(String model, Location loc, float scale); }

    private final AetheriaRewards plugin;
    private Backend backend;
    public static final List<String> BUNDLED = new ArrayList<>();

    public ModelManager(AetheriaRewards plugin) {
        this.plugin = plugin;
        if (plugin.getServer().getPluginManager().isPluginEnabled("BetterModel")) {
            try {
                backend = new BetterModelBackend();
                installBundledModels();
                plugin.getLogger().info("BetterModel detectado: modelos 3D activados.");
            } catch (Throwable t) {
                backend = null;
                plugin.getLogger().warning("BetterModel presente pero incompatible (" + t + "). Se usara el modo de respaldo.");
            }
        } else {
            plugin.getLogger().warning("BetterModel NO esta instalado: sin modelos 3D (cofres de respaldo, cosmeticos solo con particulas).");
        }
    }

    public boolean available() { return backend != null; }
    public boolean loaded(String model) { return backend != null && backend.has(model); }

    public ModelHandle spawn(String model, Location loc, float scale) {
        if (backend == null) return null;
        try { return backend.spawn(model, loc, scale); }
        catch (Throwable t) { plugin.getLogger().warning("Error creando modelo " + model + ": " + t); return null; }
    }

    /** Copia los .bbmodel incluidos a plugins/BetterModel/models (sin sobrescribir los existentes). */
    private void installBundledModels() {
        File dir = new File(plugin.getDataFolder().getParentFile(), "BetterModel/models");
        dir.mkdirs();
        try (InputStream idx = plugin.getResource("models/index.txt")) {
            if (idx == null) return;
            int copied = 0;
            for (String name : new String(idx.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\\R")) {
                name = name.trim(); if (name.isEmpty()) continue;
                BUNDLED.add(name);
                File out = new File(dir, name + ".bbmodel");
                if (out.exists()) continue;
                try (InputStream in = plugin.getResource("models/" + name + ".bbmodel")) {
                    if (in != null) { Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING); copied++; }
                }
            }
            if (copied > 0) plugin.getLogger().warning("Se instalaron " + copied + " modelos en BetterModel/models. REINICIA el servidor para que BetterModel los cargue y genere el resource pack.");
        } catch (Exception e) {
            plugin.getLogger().warning("No se pudieron instalar los modelos: " + e.getMessage());
        }
    }

    /** Respaldo visual si no hay modelo: un BlockDisplay (1 entidad por cofre, no por jugador). */
    public BlockDisplay fallbackBlock(Location loc, Material mat) {
        return loc.getWorld().spawn(loc.clone().add(-0.5, 0, -0.5), BlockDisplay.class, d -> {
            d.setBlock(mat.createBlockData());
            d.setPersistent(false);
            d.setTransformation(new Transformation(new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(1, 1, 1), new Quaternionf()));
        });
    }
}
