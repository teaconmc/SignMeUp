package org.teacon.signin.client.gui;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.resources.ResourceLocation;
import org.teacon.signin.SignMeUp;
import org.teacon.signin.client.gui.component.TooltipText;
import org.teacon.signin.data.entity.GuideMap;

public abstract class GuideMapScreenBase extends Screen {
    public static final ResourceLocation TEXTURE = new ResourceLocation(SignMeUp.MODID, "textures/gui/gui.png");

    public static final int GUI_WIDTH = 360;
    public static final int GUI_HEIGHT = 271;

    protected final ResourceLocation mapId;
    protected final GuideMap map;

    public GuideMapScreenBase(ResourceLocation mapId, GuideMap map) {
        super(map.getTitle());

        this.mapId = mapId;
        this.map = map;
    }

    public ResourceLocation getMapId() {
        return mapId;
    }

    protected TooltipText title;

    @Override
    protected void init() {
        super.init();

        var x0 = (this.width - GUI_WIDTH) / 2;
        var y0 = (this.height - GUI_HEIGHT) / 2;

        title = new TooltipText(x0 + 17, y0 + 12, map.getTitle(), Tooltip.create(map.getDesc()));

        addRenderableWidget(title);
    }

    protected boolean needRefresh = false;

    public void refresh() {
        this.needRefresh = true;
    }

    protected abstract void doRender(GuiGraphics graphics, int mouseX, int mouseY, int x0, int y0, float partialTick);

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        var x0 = (this.width - GUI_WIDTH) / 2;
        var y0 = (this.height - GUI_HEIGHT) / 2;

        renderBackground(graphics);

        doRender(graphics, mouseX, mouseY, x0, y0, partialTick);
    }

    protected void renderBackgroundTexture(GuiGraphics graphics, int x, int y, int id) {
        graphics.blit(TEXTURE, x, y, GUI_WIDTH, GUI_HEIGHT, id * GUI_WIDTH, 0, GUI_WIDTH, GUI_HEIGHT, 1080, 552);
    }
}
