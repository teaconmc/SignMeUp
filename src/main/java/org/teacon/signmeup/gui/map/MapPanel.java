package org.teacon.signmeup.gui.map;

import cn.ussshenzhou.t88.config.ConfigHelper;
import cn.ussshenzhou.t88.gui.container.TVerticalAndHorizontalScrollContainer;
import cn.ussshenzhou.t88.gui.util.ImageFit;
import cn.ussshenzhou.t88.gui.util.LayoutHelper;
import cn.ussshenzhou.t88.gui.widegt.TImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.teacon.signmeup.SignMeUp;
import org.teacon.signmeup.config.Map;
import org.teacon.signmeup.config.waypoints.Waypoint;

import java.util.List;

import static net.minecraft.util.Mth.PI;

/**
 * @author USS_Shenzhou
 */
public class MapPanel extends TVerticalAndHorizontalScrollContainer {
    private static final ResourceLocation SCROLLER_VERTICAL = SignMeUp.id("scrollbar_vert");
    private static final ResourceLocation SCROLLER_HORIZONTAL = SignMeUp.id("scrollbar_hori");
    private static final Quaternionf QUATERNION = new Quaternionf();

    final InnerMapPanel map = new InnerMapPanel();

    private final TImage me = new TImage(SignMeUp.id("textures/gui/me_map.png")) {
        @Override
        public void render(GuiGraphics guigraphics, int pMouseX, int pMouseY, float pPartialTick) {
            guigraphics.pose().pushPose();
            var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            QUATERNION.identity().rotateZ(PI + camera.getYRot() * PI / 180);
            guigraphics.pose().last().pose().rotateAround(QUATERNION, x + 16, y + 16, 0);
            super.render(guigraphics, pMouseX, pMouseY, pPartialTick);
            guigraphics.pose().popPose();
        }
    };

    private final WayPointsPanel wayPointsPanel = new WayPointsPanel();

    private float size = 1.5f;

    public MapPanel() {
        map.setImageFit(ImageFit.STRETCH);
        this.add(map);
        this.add(me);
        this.add(wayPointsPanel);
    }

    @Override
    public void tickT() {
        locateMe();
        super.tickT();
    }

    private void locateMe() {
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        var mePos = map.worldToGui(camera.getBlockPosition().getX(), camera.getBlockPosition().getZ());
        me.setBounds(mePos.x - 16, mePos.y - 16, 32, 32);
    }

    private static final ResourceLocation ARROW_OUTER = SignMeUp.id("textures/gui/arrow_outer.png");

    @Override
    public void render(GuiGraphics guigraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(guigraphics, pMouseX, pMouseY, pPartialTick);

        MapScreen s = (MapScreen) getTopParentScreen();
        if (s == null) {
            return;
        }

        String waypoint = s.getHighlightWaypoints();
        if (waypoint == null) {
            return;
        }
        Vector2i v = wayPointsPanel.lookupWaypoint(waypoint);
        int px = (int) (v.x - scrollAmountX), py = (int) (v.y - scrollAmountY);
        int gw = guigraphics.guiWidth(), gh = guigraphics.guiHeight();
        if (px >= 0 && px < gw && py >= 0 && py < gh) {
            guigraphics.fill(0, py - 1, gw, py + 1, 0xFFE8DDCD);
            guigraphics.fill(px - 1, 0, px + 1, gh, 0xFFE8DDCD);
        } else {
            double bx = 110, by = 20;
            double gw2 = gw / 2D, gh2 = gh / 2D;
            double dx = px - gw2, dy = py - gh2, tan = dx / dy;

            double ax = (dy < 0 ? -tan : tan) * (gh2 - by) + gw2, ay = dy > 0 ? gh - by : by;
            if (ax <= bx || ax >= gw - bx) {
                if (tan != 0) {
                    ay = (gw2 - bx) / (dx > 0 ? tan : -tan) + gh2;
                }
                ax = dx > 0 ? gw - bx : bx;
            }

            guigraphics.pose().pushPose();

            QUATERNION.identity().rotateZ((float) (-Math.atan(tan) + (dy > 0 ? Math.PI : 0D)));
            guigraphics.pose().last().pose().rotateAround(QUATERNION, (float) ax, (float) ay, 0);

            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            guigraphics.blit(ARROW_OUTER, (int) ax - 16, (int) ay - 6, 32, 32, 0F, 0F, 32, 32, 32, 32);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            guigraphics.pose().popPose();
        }
    }

    @Override
    public void layout() {
        int mapSize = (int) (getUsableHeight() * size);
        map.setBounds(
                Math.max((getUsableWidth() - mapSize) / 2, 0),
                Math.max((getUsableHeight() - mapSize) / 2, 0),
                mapSize, mapSize);
        LayoutHelper.BSameAsA(wayPointsPanel, map);
        locateMe();
        wayPointsPanel.update();
        super.layout();
    }

    private void zoom(double pMouseX, double pMouseY, float delta) {
        float newSize = Mth.clamp(size + delta * 0.275f, 0.75f, 10);
        double centerX = (scrollAmountX + pMouseX) / map.getWidth();
        double centerY = (scrollAmountY + pMouseY) / map.getHeight();
        double prevCenterX = (prevScrollAmountX + pMouseX) / map.getWidth();
        double prevCenterY = (prevScrollAmountY + pMouseY) / map.getHeight();
        size = newSize;
        this.layout();
        double newScrollX = centerX * map.getWidth() - pMouseX;
        double newScrollY = centerY * map.getHeight() - pMouseY;
        double prevNewScrollX = prevCenterX * map.getWidth() - pMouseX;
        double prevNewScrollY = prevCenterY * map.getHeight() - pMouseY;
        initPos();

        if (isScrollBarVisibleHorizontal()) {
            this.prevScrollAmountX = Mth.clamp(prevNewScrollX, 0, getMaxScrollX());
            this.scrollAmountX = Mth.clamp(newScrollX, 0, getMaxScrollX());
        } else {
            this.prevScrollAmountX = this.scrollAmountX = 0;
        }

        if (isScrollBarVisibleVertical()) {
            this.prevScrollAmountY = Mth.clamp(prevNewScrollY, 0, getMaxScrollY());
            this.scrollAmountY = Mth.clamp(newScrollY, 0, getMaxScrollY());
        } else {
            this.prevScrollAmountY = this.scrollAmountY = 0;
        }
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double deltaX, double deltaY) {
        if (this.isInRange(pMouseX, pMouseY)) {
            zoom(pMouseX, pMouseY, (float) deltaY);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected ResourceLocation getScrollerHorizontalTexture() {
        return SCROLLER_HORIZONTAL;
    }

    @Override
    protected ResourceLocation getScrollerVerticalTexture() {
        return SCROLLER_VERTICAL;
    }

    public List<Waypoint> getHighlightWaypoints(double pMouseX, double pMouseY) {
        return wayPointsPanel.getHighlightWaypoints(pMouseX + scrollAmountX, pMouseY + scrollAmountY);
    }

    public static class InnerMapPanel extends TImage {
        public InnerMapPanel() {
            super(SignMeUp.id("textures/gui/map.png"));
        }

        public Vector2i worldToGui(double x, double z) {
            var mapCfg = ConfigHelper.getConfigRead(Map.class);
            //world center
            var pos = new Vector2f(mapCfg.centerWorldX, mapCfg.centerWorldZ);
            //world top left
            pos.add(-mapCfg.worldSize / 2f, -mapCfg.worldSize / 2f);
            //world top left delta
            pos.mul(-1).add((float) x, (float) z);
            //world top left delta relative
            pos.mul(1f / mapCfg.worldSize);
            //gui top left delta
            pos.mul(this.getWidth());
            pos.add(this.getXT(), this.getYT());
            return new Vector2i((int) pos.x, (int) pos.y);
        }
    }
}
