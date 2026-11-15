package com.aetheria.rewards.model;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.data.renderer.ModelRenderer;
import kr.toxicity.model.api.platform.PlatformEntity;
import kr.toxicity.model.api.platform.PlatformPlayer;
import kr.toxicity.model.api.tracker.DummyTracker;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.Tracker;
import kr.toxicity.model.api.tracker.ModelRotation;
import kr.toxicity.model.api.tracker.ModelScaler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

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

    @Override public ModelHandle attach(String model, Player p, float scale, Supplier<Vector3f> offset, boolean followHead) {
        ModelRenderer r = BetterModel.modelOrNull(model);
        if (r == null) return null;
        PlatformEntity pe = BukkitAdapter.adapt(p);
        EntityTracker t = r.getOrCreate(pe);
        if (scale != 1f) t.scaler(ModelScaler.value(scale));
        if (followHead) t.rotation(() -> new ModelRotation(0f, p.getLocation().getYaw()));   // sombreros/mascaras giran con la CABEZA
        for (var b : t.bones()) b.addPositionModifier(bone -> true, v -> new Vector3f(v).add(offset.get()));
        return new EntityHandle(t, p);
    }

    private static String switchState(Tracker t, String cur, String next) {
        if (t.isClosed() || next.equals(cur)) return cur;
        if (t.animate(next, new AnimationModifier(0, 0, AnimationIterator.Type.LOOP))) { t.stopAnimation(cur); return next; }
        return cur;
    }

    private static void play(Tracker t, String animation, ModelHandle.Mode mode) {
        if (t.isClosed()) return;
        AnimationIterator.Type type = switch (mode) {
            case LOOP -> AnimationIterator.Type.LOOP;
            case ONCE -> AnimationIterator.Type.PLAY_ONCE;
            case HOLD -> AnimationIterator.Type.HOLD_ON_LAST;
        };
        t.animate(animation, new AnimationModifier(0, 0, type));
    }

    /** Modelo ligado a la entidad del jugador: lo mueve BetterModel junto con el jugador. */
    private static final class EntityHandle implements ModelHandle {
        private final EntityTracker t; private final Player owner; private boolean hiddenOwn = false; private String cur = "idle";
        EntityHandle(EntityTracker t, Player owner) { this.t = t; this.owner = owner; }
        @Override public void move(Location loc) { /* lo mueve BetterModel */ }
        @Override public void animate(String animation, Mode mode) { play(t, animation, mode); }
        @Override public void state(String a) { cur = switchState(t, cur, a); }
        @Override public void refreshViewers(double radius, UUID exclude) {
            boolean hide = exclude != null;
            if (t.isClosed() || hide == hiddenOwn) return;
            hiddenOwn = hide;
            PlatformPlayer pp = BukkitAdapter.adapt(owner);
            if (hide) t.hide(pp); else t.show(pp);       // ocultar SOLO para el dueño
        }
        @Override public void close() { if (!t.isClosed()) t.close(); }
        @Override public boolean isClosed() { return t.isClosed(); }
    }

    private static final class Handle implements ModelHandle {
        private final DummyTracker t;
        private String cur = "idle";
        private Location loc;
        private final Set<UUID> viewers = new HashSet<>();
        Handle(DummyTracker t, Location loc) { this.t = t; this.loc = loc; }
        @Override public void state(String a) { cur = switchState(t, cur, a); }
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
