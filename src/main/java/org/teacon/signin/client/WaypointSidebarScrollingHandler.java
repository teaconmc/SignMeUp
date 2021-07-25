package org.teacon.signin.client;

import net.minecraft.client.Minecraft;

public final class WaypointSidebarScrollingHandler extends DescTextScrollingHandler {
    public WaypointSidebarScrollingHandler(GuideMapScreen parent, double topX, double topY, double bottomX, double bottomY) {
        super(parent, topX, topY, bottomX, bottomY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Adjust for macOS-style scrolling
        if (Minecraft.IS_RUNNING_ON_MAC) {
            delta = -delta;
        }
        this.parent.setWpSidebarScrollAmount(this.parent.getWpSidebarScrollAmount() + delta * (double)this.parent.getWpSidebarButtonHeight() / 2.0D);
        return true;
    }
}
