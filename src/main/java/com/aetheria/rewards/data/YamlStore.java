package com.aetheria.rewards.data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Un archivo YAML por jugador, escritura atomica (temporal + move). */
public final class YamlStore implements DataStore {
    private final File dir;
    public YamlStore(File dir) { this.dir = dir; dir.mkdirs(); }
    private File file(UUID id) { return new File(dir, id + ".yml"); }

    @Override public String load(UUID id) throws IOException {
        File f = file(id);
        return f.exists() ? Files.readString(f.toPath(), StandardCharsets.UTF_8) : null;
    }
    @Override public synchronized void save(UUID id, String yaml) throws IOException {
        File target = file(id), tmp = new File(dir, id + ".yml.tmp");
        Files.writeString(tmp.toPath(), yaml, StandardCharsets.UTF_8);
        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
    @Override public String name() { return "YAML (archivos)"; }
}
