package com.aetheria.rewards.gui;

import com.aetheria.rewards.AetheriaRewards;
import com.aetheria.rewards.util.Util;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

/**
 * Skin de GUIs: el titulo del inventario dibuja un fondo ornamental (fuente 'aetheria:gui' del resource pack).
 * Si 'gui-skin' esta desactivado en config.yml (o el pack no esta cargado) se usa el marco de cristales.
 */
public final class MenuSkin {
    private static final List<String> THEMES = List.of("main", "advent", "feb");
    private MenuSkin() {}

    public static Component title(AetheriaRewards plugin, String theme, int rows, String name) {
        if (!plugin.skin() || rows < 3 || rows > 6) return Util.c(Util.title(name));
        int glyph = 0xE100 + Math.max(0, THEMES.indexOf(theme)) * 4 + (rows - 3);
        Key font = Key.key("aetheria", "gui");
        return Component.text("\uF808" + (char) glyph).font(font).color(NamedTextColor.WHITE)
                .append(Component.text("\uF8F9").font(font))
                .append(Component.text(name).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
    }
}
