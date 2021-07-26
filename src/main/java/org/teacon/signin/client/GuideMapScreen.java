package org.teacon.signin.client;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.teacon.signin.SignMeUp;
import org.teacon.signin.data.GuideMap;
import org.teacon.signin.data.Trigger;
import org.teacon.signin.data.Waypoint;
import org.teacon.signin.network.TriggerFromMapPacket;
import org.teacon.signin.network.TriggerFromWaypointPacket;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class GuideMapScreen extends Screen {

    private static final int X_SIZE = 385;
    private static final int Y_SIZE = 161;

    public final ResourceLocation mapId;

    private static final ResourceLocation MAP_ICONS = new ResourceLocation("minecraft", "textures/map/map_icons.png");
    private static final ResourceLocation GUIDE_MAP_LEFT = new ResourceLocation("sign_up", "textures/gui/guide_map_left.png");
    private static final ResourceLocation GUIDE_MAP_RIGHT = new ResourceLocation("sign_up", "textures/gui/guide_map_right.png");

    private int mapTriggerPage = 0;
    private int mapTriggerPageSize = 0;
    private ResourceLocation selectedWaypoint;

    private final GuideMap map;
    private final Vector3d playerLocation;
    private final List<ResourceLocation> waypointIds;

    private ImageButton leftFlip, rightFlip;
    private ImageButton mapTriggerPrev, mapTriggerNext;

    private final List<TriggerButton> mapTriggers = Lists.newArrayList();
    private final ListMultimap<ResourceLocation, TriggerButton> waypointTriggers = ArrayListMultimap.create();

    public GuideMapScreen(ResourceLocation mapId, GuideMap map, Vector3d location) {
        super(map.getTitle());
        this.map = map;
        this.mapId = mapId;
        this.playerLocation = location;
        this.waypointIds = map.getWaypointIds();
    }

    @Override
    protected void init() {
        super.init();
        int x0 = (this.width - X_SIZE) / 2, y0 = (this.height - Y_SIZE) / 2, x1 = x0 + 206;

        // Left and Right Image Flip Button
        this.leftFlip = this.addButton(new ImageButton(x1 + 7, y0 + 20, 10, 54, 155, 163, 0, GUIDE_MAP_RIGHT, new FlipHandler(1)));
        this.rightFlip = this.addButton(new ImageButton(x1 + 93, y0 + 20, 10, 54, 167, 163, 0, GUIDE_MAP_RIGHT, new FlipHandler(-1)));

        // Prev and next page for map triggers
        this.mapTriggerPrev = this.addButton(new ImageButton(x0 + 6, y0 + 138, 33, 17, 66, 163, 19, GUIDE_MAP_LEFT, (btn) -> --this.mapTriggerPage));
        this.mapTriggerNext = this.addButton(new ImageButton(x0 + 39, y0 + 138, 33, 17, 99, 163, 19, GUIDE_MAP_LEFT, (btn) -> ++this.mapTriggerPage));

        // Setup trigger buttons from GuideMap
        List<ResourceLocation> mapTriggerIds = this.map.getTriggerIds();
        for (int i = 0, mapTriggerIdSize = mapTriggerIds.size(); i < mapTriggerIdSize; ++i) {
            ResourceLocation triggerId = mapTriggerIds.get(i);
            this.mapTriggerPageSize = Math.max(this.mapTriggerPageSize, 1 + i / 6);
            final TriggerButton btn = this.addButton(new TriggerButton(
                    x0 + 8, y0 + 21 + (i % 6) * 19, 62, 18, 2, 163, 20, GUIDE_MAP_LEFT, triggerId, 0x404040,
                    (b) -> SignMeUp.channel.sendToServer(new TriggerFromMapPacket(this.mapId, triggerId))));
            this.mapTriggers.add(btn);
            btn.visible = false;
        }

        // Setup Waypoints
        int mapCanvasX = x0 + 78, mapCanvasY = y0 + 23;
        for (ResourceLocation wpId : this.waypointIds) {
            Waypoint wp = SignMeUpClient.MANAGER.findWaypoint(wpId);
            if (wp == null || wp.isDisabled()) {
                continue;
            }
            // For map_icons.png, each icon is 4x4 pixels, so after we get the center coordinate,
            // we also need to shift left/up by 2 pixels to center the icon.
            final Vector3i center = this.map.center;
            final Vector3i renderLocation = wp.getRenderLocation();
            final int wpX = Math.round(((float) (renderLocation.getX() - center.getX())) / this.map.radius * 64) + 64;
            final int wpY = Math.round(((float) (renderLocation.getZ() - center.getZ())) / this.map.radius * 64) + 64;
            if (wpX >= 1 && wpX <= 127 && wpY >= 1 && wpY <= 127) {
                // Setup Waypoints as ImageButtons
                this.addButton(new ImageButton(mapCanvasX + wpX - 2, mapCanvasY + wpY - 2, 4, 4, 58, 2, 0, MAP_ICONS,
                        128, 128, (btn) -> this.selectedWaypoint = wpId, (btn, transform, mouseX, mouseY) -> {
                    double distance = Math.sqrt(wp.getActualLocation().distanceSq(this.playerLocation, true));
                    this.renderTooltip(transform, Arrays.asList(
                            wp.getTitle().func_241878_f(),
                            new TranslationTextComponent("sign_up.waypoint.distance",
                                    Math.round(distance * 10.0) / 10.0).func_241878_f()
                    ), mouseX, mouseY);
                }, wp.getTitle()));
                // Setup trigger buttons from Waypoints
                List<ResourceLocation> wpTriggerIds = wp.getTriggerIds();
                for (int i = 0, max = Math.min(7, wpTriggerIds.size()); i < max; ++i) {
                    ResourceLocation triggerId = wpTriggerIds.get(i);
                    TriggerButton btn = this.addButton(new TriggerButton(
                            x1 + 109, y0 + 21 + i * 19, 62, 18, 2, 163, 20, GUIDE_MAP_RIGHT, triggerId, 0x404040,
                            (b) -> SignMeUp.channel.sendToServer(new TriggerFromWaypointPacket(wpId, triggerId))));
                    this.waypointTriggers.put(wpId, btn);
                    btn.visible = false;
                }
            }
        }
    }

    @Override
    public void tick() {
        this.mapTriggerPrev.visible = this.mapTriggerPage >= 1;
        this.mapTriggerNext.visible = this.mapTriggerPage < this.mapTriggerPageSize - 1;
        Waypoint wp = this.selectedWaypoint == null ? null : SignMeUp.MANAGER.findWaypoint(this.selectedWaypoint);
        this.leftFlip.visible = this.rightFlip.visible = wp != null && !wp.isDisabled() && wp.hasMoreThanOneImage();
        for (Map.Entry<ResourceLocation, TriggerButton> entry : this.waypointTriggers.entries()) {
            entry.getValue().visible = Objects.equals(entry.getKey(), this.selectedWaypoint);
        }
        for (int i = 0, size = this.mapTriggers.size(); i < size; ++i) {
            this.mapTriggers.get(i).visible = i / 6 == this.mapTriggerPage;
        }
    }

    @Override
    public void render(MatrixStack transforms, int mouseX, int mouseY, float partialTicks) {
        final Minecraft mc = Objects.requireNonNull(this.minecraft);

        int x0 = (this.width - X_SIZE) / 2, y0 = (this.height - Y_SIZE) / 2, x1 = x0 + 206;

        this.renderBackground(transforms);
        this.renderBackgroundTexture(mc, transforms, x0, y0, x1);
        this.renderWaypointTexture(mc, transforms, x0, y0, x1);
        this.renderMapTexture(mc, transforms, x0, y0, x1);

        super.render(transforms, mouseX, mouseY, partialTicks);

        this.renderTextCollection(this.font, transforms, x0, y0, x1);
    }

    private void renderTextCollection(FontRenderer font, MatrixStack transforms, int x0, int y0, int x1) {
        // Display labels for prev and next page buttons
        final int prevColor = this.mapTriggerPrev.visible ? 0x404040 : 0xFFFFFF;
        final int nextColor = this.mapTriggerNext.visible ? 0x404040 : 0xFFFFFF;
        ITextComponent prevPage = new StringTextComponent("<"), nextPage = new StringTextComponent(">");
        font.drawText(transforms, prevPage, x0 + 23F - font.getStringPropertyWidth(prevPage) / 2F, y0 + 143F, prevColor);
        font.drawText(transforms, nextPage, x0 + 56F - font.getStringPropertyWidth(nextPage) / 2F, y0 + 143F, nextColor);
        // Display the subtitle/desc of the map if no waypoint is selected
        ITextComponent title = this.map.getTitle(), subtitle = this.map.getSubtitle(), desc = this.map.getDesc();
        if (this.selectedWaypoint != null) {
            Waypoint wp = SignMeUpClient.MANAGER.findWaypoint(this.selectedWaypoint);
            if (wp != null && !wp.isDisabled()) {
                subtitle = wp.getTitle();
                desc = wp.getDesc();
            }
        }
        // Draw title and subtitle depending on whether a waypoint is selected
        font.drawText(transforms, title, x0 + 142F - font.getStringPropertyWidth(title) / 2F, y0 + 7F, 0x404040);
        font.drawText(transforms, subtitle, x1 + 55F - font.getStringPropertyWidth(subtitle) / 2F, y0 + 7F, 0x404040);
        // I DISLIKE THIS METHOD BECAUSE IT FAILS TO HANDLE LINE BREAKING
        // A proper line breaking algorithm should comply with UAX #14, link below:
        // http://www.unicode.org/reports/tr14/
        // However it at least get things work for now. So it is the status quo.
        List<IReorderingProcessor> displayedDescList = font.trimStringToWidth(desc, 90);
        // Draw desc text
        for (int i = 0, size = Math.min(8, displayedDescList.size()); i < size; ++i) {
            font.func_238422_b_(transforms, displayedDescList.get(i), x1 + 10F, y0 + 82F + 9 * i, 0x404040);
        }
    }

    @SuppressWarnings("deprecation")
    private void renderMapTexture(Minecraft mc, MatrixStack transforms, int x0, int y0, int x1) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        // corner of the in-game window
        mc.textureManager.bindTexture(this.map.texture);
        // Who said we have to use 128 * 128 texture?
        blit(transforms, x0 + 78, y0 + 23, 0, 0, 128, 128, 128, 128);
    }

    @SuppressWarnings("deprecation")
    private void renderWaypointTexture(Minecraft mc, MatrixStack transforms, int x0, int y0, int x1) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        ResourceLocation image = Waypoint.DEFAULT_IMAGE;
        for (ResourceLocation wpId : this.waypointIds) {
            if (this.selectedWaypoint == wpId) {
                Waypoint wp = SignMeUpClient.MANAGER.findWaypoint(wpId);
                if (wp != null && !wp.isDisabled()) {
                    image = wp.getDisplayingImageId();
                    break;
                }
            }
        }
        mc.textureManager.bindTexture(image);
        blit(transforms, x1 + 7, y0 + 20, 0, 0, 96, 54, 96, 54);
    }

    @SuppressWarnings("deprecation")
    private void renderBackgroundTexture(Minecraft mc, MatrixStack transforms, int x0, int y0, int x1) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.textureManager.bindTexture(GUIDE_MAP_LEFT);
        blit(transforms, x0, y0, 0, 0, 211, 161);
        blit(transforms, x0 + 6, y0 + 138, 66, 201, 66, 17);
        mc.textureManager.bindTexture(GUIDE_MAP_RIGHT);
        blit(transforms, x1 + 5, y0, 5, 0, 174, 161);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x0 = (this.width - X_SIZE) / 2, y0 = (this.height - Y_SIZE) / 2;
        // Reset selected Wp when clicking everywhere on the map
        if (mouseX >= x0 + 42 && mouseX < x0 + 170 && mouseY >= y0 + 25 && mouseY < y0 + 153) {
            if (this.selectedWaypoint != null) {
                this.selectedWaypoint = null;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private final class FlipHandler implements Button.IPressable {
        private final int diff;

        private FlipHandler(int diff) {
            this.diff = diff;
        }

        @Override
        public void onPress(Button p_onPress_1_) {
            for (ResourceLocation wpId : waypointIds) {
                Waypoint wp = SignMeUpClient.MANAGER.findWaypoint(wpId);
                if (wp != null && !wp.isDisabled()) {
                    wp.modifyDisplayingImageIndex(this.diff);
                }
            }
        }
    }

    private final class TriggerButton extends ImageButton {
        private final ResourceLocation triggerId;
        private final int textColor;

        private TriggerButton(int x, int y, int width, int height, int uOffset, int vOffset, int vDiff,
                              ResourceLocation image, ResourceLocation triggerId, int textColor, IPressable pressable) {
            super(x, y, width, height, uOffset, vOffset, vDiff, image, pressable);
            this.triggerId = triggerId;
            this.textColor = textColor;
        }

        @Override
        public void renderWidget(MatrixStack transforms, int mouseX, int mouseY, float partialTicks) {
            super.renderWidget(transforms, mouseX, mouseY, partialTicks);
            final FontRenderer font = Minecraft.getInstance().fontRenderer;
            final Trigger trigger = SignMeUpClient.MANAGER.findTrigger(this.triggerId);
            if (trigger != null) {
                final int stringWidth = font.getStringPropertyWidth(trigger.getTitle());
                final float x0 = this.x + (this.width - stringWidth) / 2F, y0 = this.y + (this.height - 9) / 2F;
                font.drawText(transforms, trigger.getTitle(), x0, y0, this.textColor);
                if (this.isHovered()) {
                    GuideMapScreen.this.renderTooltip(transforms, trigger.getDesc(), mouseX, mouseY);
                }
            }
        }
    }
}
