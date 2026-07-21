package org.teacon.exhibition_portal.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.teacon.exhibition_portal.ExhibitionPortal;

public final class Components {
    private Components() {
    }

    public static MutableComponent join(Component... components) {
        MutableComponent literal = Component.literal("");
        for (Component component : components) {
            literal.append(component);
        }
        return literal;
    }

    public static final Style HEAVY = Style.EMPTY;

    public static final Style REGULAR = Style.EMPTY
            .withoutShadow();
}
