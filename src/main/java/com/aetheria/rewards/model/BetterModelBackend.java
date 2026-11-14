package com.aetheria.rewards.model;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.tracker.DummyTracker;
import kr.toxicity.model.api.tracker.ModelScaler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Unica clase que toca la API de BetterModel 2.0.1 (verificada contra el jar):
 * BetterModel.modelOrNull(String), ModelRenderer.create(PlatformLocation) -> DummyTracker,
 * DummyTracker.location(PlatformLocation), Tracker.animate(String, AnimationModifier), Tracker.scaler(ModelScaler),
 * Tracker.close(). Solo se carga si el plugin BetterModel esta activo.
 */
public final class BetterModelBackend implements ModelManager.Backend {
    @Override public boolean has(String model) { return BetterModel.modelOrNull(model) != null; }

    @Override public ModelHandle spawn(String model, Location loc, float scale) {
        ModelRenderer r = BetterModel.modelOrNull(model);
        if (r == null) return null;
        DummyTracker t = r.create(BukkitAdapter.adapt(loc));
        if (scale != 1f) t.scaler(ModelScaler.value(scale));
        return new Handle(t, loc);
    }

    private static final class Handle implements ModelHandle {
        private final DummyTracker t;
        private Location loc;
        private final Set<UUID> viewers = new HashSet<>();
        Handle(DummyTracker t, Location loc) { this.t = t; this.loc = loc; }
        @Override public void move(Location l) { this.loc = l; if (!t.isClosed()) t.location(BukkitAdapter.adapt(l)); }
        @Override public void refreshViewers(double radius, UUID exclude) {
            if (t.isClosed() || loc == null || loc.getWorld() == null) return;
            Set<UUID> now = new HashSet<>();
            for (Player p : loc.getWorld().getNearbyPlayers(loc, radius)) {
                if (p.getUniqueId().equals(exclude)) continue;      // el dueño no ve su propio modelo si lo oculto
                now.add(p.getUniqueId());
                if (viewers.add(p.getUniqueId())) t.spawn(BukkitAdapter.adapt(p));
            }
            Iterator<UUID> it = viewers.iterator();
            while (it.hasNext()) {
                UUID u = it.next();
                if (now.contains(u)) continue;
                it.remove();
                Player p = Bukkit.getPlayer(u);
                if (p != null) t.remove(BukkitAdapter.adapt(p));
            }
        }
        @Override public void animate(String animation, Mode mode) {
            if (t.isClosed()) return;
            AnimationIterator.Type type = switch (mode) {
                case LOOP -> AnimationIterator.Type.LOOP;
                case ONCE -> AnimationIterator.Type.PLAY_ONCE;
                case HOLD -> AnimationIterator.Type.HOLD_ON_LAST;
            };
            t.animate(animation, new AnimationModifier(0, 0, type));
        }
        @Override public void close() { if (!t.isClosed()) t.close(); }
        @Override public boolean isClosed() { return t.isClosed(); }
    }
}
