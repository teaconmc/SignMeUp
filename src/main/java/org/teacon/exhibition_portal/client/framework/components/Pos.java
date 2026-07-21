package org.teacon.exhibition_portal.client.framework.components;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

public record Pos(float x, float y) {
    public static Pos ofLossy(double x, double y) {
        return new Pos((float) x, (float) y);
    }

    public static Pos getMouse() {
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        return Pos.ofLossy(minecraft.mouseHandler.getScaledXPos(window), minecraft.mouseHandler.getScaledYPos(window));
    }
}
