package org.teacon.signmeup.hud;

import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.gui.util.ImageFit;
import cn.ussshenzhou.t88.gui.widegt.TImage;
import cn.ussshenzhou.t88.gui.widegt.TPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.teacon.signmeup.SignMeUp;
import org.teacon.signmeup.config.MiniMap;
import org.teacon.signmeup.gui.map.MapScreen;
import org.teacon.signmeup.gui.settings.SettingsScreen;

import static net.minecraft.util.Mth.PI;

/**
 * @author USS_Shenzhou
 */
public class MiniMapPanel extends TPanel {
    private final TImage background = new TImage(SignMeUp.id("textures/gui/minimap_bg.png"));
    private final InnerMiniMapPanel innerMiniMap = new InnerMiniMapPanel();
    private static final Quaternionf QUATERNION = new Quaternionf();
    private final TImage me = new TImage(SignMeUp.id("textures/gui/me_minimap.png")) {
        @Override
        public void render(GuiGraphics guigraphics, int pMouseX, int pMouseY, float pPartialTick) {
            guigraphics.pose().pushPose();
            var cfgR = ConfigHelper.getConfigRead(MiniMap.class);
            if (!cfgR.followPlayerRotation) {
                var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
                QUATERNION.identity().rotateZ(PI + camera.getYRot() * PI / 180);
                guigraphics.pose().last().pose().rotateAround(QUATERNION, x + 8, y + 8, 0);
            }
            super.render(guigraphics, pMouseX, pMouseY, pPartialTick);
            guigraphics.pose().popPose();
        }
    };

    public MiniMapPanel() {
        super();
        this.add(background);
        background.setImageFit(ImageFit.FILL);
        this.add(innerMiniMap);
        this.add(me);
    }

    @Override
    public void tickT() {
        super.tickT();
        Minecraft mc = Minecraft.getInstance();

        var visible = !mc.getDebugOverlay().showDebugScreen()
                && !mc.options.hideGui
                && (mc.level != null && mc.level.dimension() == Level.OVERWORLD)
                && MiniMapAPI.INSTANCE.visible()
                && !(mc.screen instanceof MapScreen)
                && !(mc.screen instanceof SettingsScreen);
        if (!children.isEmpty() && (children.getFirst().isVisibleT() != visible)) {
            children.forEach(childTComponent -> childTComponent.setVisibleT(visible));
        }
    }

    @Override
    public void resizeAsHud(int screenWidth, int screenHeight) {
        innerMiniMap.setAbsBounds(0, 0, screenWidth, screenHeight);
        super.resizeAsHud(screenWidth, screenHeight);
        var scale = Minecraft.getInstance().getWindow().getGuiScale();
        var minimapSize = InnerMiniMapPanel.getMinimapScreenSize();
        background.setAbsBounds(
                (int) ((innerMiniMap.screenX1() - minimapSize / 13f / 2) / scale),
                (int) ((innerMiniMap.screenY1() - minimapSize / 13f * 1.9) / scale),
                (int) (minimapSize / 13f * 14 / scale),
                (int) (minimapSize / 13f * 15.4f / scale));
        me.setAbsBounds(
                (int) (innerMiniMap.x1() + InnerMiniMapPanel.getMinimapSize() / 2f - 8),
                (int) (innerMiniMap.y1() + InnerMiniMapPanel.getMinimapSize() / 2f - 8),
                16,
                16);
    }
}
