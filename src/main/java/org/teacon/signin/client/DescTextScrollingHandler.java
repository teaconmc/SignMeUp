package org.teacon.signin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;

public class DescTextScrollingHandler implements IGuiEventListener {

    protected final GuideMapScreen parent;
    private final double topX, topY, bottomX, bottomY;

    public DescTextScrollingHandler(GuideMapScreen parent, double topX, double topY, double bottomX, double bottomY) {
        this.parent = parent;
        this.topX = topX;
        this.topY = topY;
        this.bottomX = bottomX;
        this.bottomY = bottomY;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX > topX && mouseX < bottomX && mouseY > topY && mouseY < bottomY;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Adjust for macOS-style scrolling
        if (Minecraft.IS_RUNNING_ON_MAC) {
            delta = -delta;
        }
        if (delta > 0) {
            this.parent.scrollDownDesc();
        } else {
            this.parent.scrollUpDesc();
        }
        return true;
    }
}
