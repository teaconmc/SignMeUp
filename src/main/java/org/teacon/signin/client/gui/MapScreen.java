package org.teacon.signin.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.teacon.signin.data.entity.GuideMap;

public class MapScreen extends GuideMapScreenBase {

    protected Vec3 location;

    public MapScreen(ResourceLocation mapId, GuideMap map, Vec3 location) {
        super(mapId, map);

        this.location = location;
    }

    @Override
    protected void doRender(GuiGraphics graphics, int mouseX, int mouseY, int x0, int y0, float partialTick) {
        renderBackgroundTexture(graphics, x0, y0, 0);

//        graphics.blit(map.texture, x0 + 97, y0 + 7, 0, 0, );
    }
}
