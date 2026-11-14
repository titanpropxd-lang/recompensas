package com.aetheria.rewards.model;

import org.bukkit.Location;

/** Representacion de un modelo vivo en el mundo (independiente del backend). */
public interface ModelHandle {
    enum Mode { LOOP, ONCE, HOLD }
    void move(Location loc);
    void animate(String animation, Mode mode);
    void close();
    boolean isClosed();
}
