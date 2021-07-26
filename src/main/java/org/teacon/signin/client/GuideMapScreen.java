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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.ITextComponent;
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

    private int lastWaypointTextureOpacity = 0;
    private int lastWaypointTextureOpacityPrev = 0;
    private ResourceLocation lastWaypointTexture = null;

    private boolean hasWaypointTrigger = false;

    private final GuideMap map;
    private final Vector3d playerLocation;
    private final List<ResourceLocation> waypointIds;

    private ImageButton leftFlip, rightFlip;
    private ImageButton mapTriggerPrev, mapTriggerNext;

    private final List<TriggerButton> mapTriggers = Lists.newArrayList();
    private final ListMultimap<ResourceLocation, TriggerButton> waypointTriggers = ArrayListMultimap.create();

    private boolean needRefresh = false;

    public GuideMapScreen(ResourceLocation mapId, GuideMap map, Vector3d location) {
        super(map.getTitle());
        this.map = map;
        this.mapId = mapId;
        this.playerLocation = location;
        this.waypointIds = map.getWaypointIds();
    }

    public void refresh() {
        // Set refresh state
        this.needRefresh = true;
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
        this.mapTriggers.clear();
        List<ResourceLocation> mapTriggerIds = this.map.getTriggerIds();
        for (int i = 0, j = 0; i < mapTriggerIds.size(); ++i) {
            ResourceLocation triggerId = mapTriggerIds.get(i);
            final Trigger trigger = SignMeUpClient.MANAGER.findTrigger(triggerId);
            if (trigger == null) {
                continue;
            }
            this.mapTriggerPageSize = Math.max(this.mapTriggerPageSize, 1 + j / 6);
            final TriggerButton btn = this.addButton(new TriggerButton(x0 + 8, y0 + 21 + (j % 6) * 19, 62, 18,
                    2, trigger.disabled ? 203 : 163, trigger.disabled ? 0 : 20, GUIDE_MAP_LEFT, trigger,
                    (b) -> SignMeUp.channel.sendToServer(new TriggerFromMapPacket(this.mapId, triggerId))));
            this.mapTriggers.add(btn);
            btn.visible = false;
            ++j;
        }

        // Setup Waypoints
        this.waypointTriggers.clear();
        int mapCanvasX = x0 + 78, mapCanvasY = y0 + 23;
        for (ResourceLocation wpId : this.waypointIds) {
            Waypoint wp = SignMeUpClient.MANAGER.findWaypoint(wpId);
            if (wp == null) {
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
                for (int i = 0, j = 0; i < wpTriggerIds.size() && j < 7; ++i) {
                    ResourceLocation triggerId = wpTriggerIds.get(i);
                    final Trigger trigger = SignMeUpClient.MANAGER.findTrigger(triggerId);
                    if (trigger == null) {
                        continue;
                    }
                    TriggerButton btn = this.addButton(new TriggerButton(x1 + 109, y0 + 21 + j * 19, 62, 18,
                            2, trigger.disabled ? 203 : 163, trigger.disabled ? 0 : 20, GUIDE_MAP_RIGHT, trigger,
                            (b) -> SignMeUp.channel.sendToServer(new TriggerFromWaypointPacket(wpId, triggerId))));
                    this.waypointTriggers.put(wpId, btn);
                    btn.visible = false;
                    ++j;
                }
            }
        }
    }

    @Override
    public void tick() {
        if (this.needRefresh) {
            this.init(Minecraft.getInstance(), this.width, this.height);
            this.needRefresh = false;
        }

        this.lastWaypointTextureOpacityPrev = this.lastWaypointTextureOpacity;
        if ((this.lastWaypointTextureOpacity -= 10) <= 0) {
            this.lastWaypointTextureOpacity = 0;
            this.lastWaypointTexture = null;
        }

        this.mapTriggerPrev.visible = this.mapTriggerPage >= 1;
        this.mapTriggerNext.visible = this.mapTriggerPage < this.mapTriggerPageSize - 1;

        Waypoint wp = this.selectedWaypoint == null ? null : SignMeUp.MANAGER.findWaypoint(this.selectedWaypoint);
        this.leftFlip.visible = this.rightFlip.visible = wp != null ? wp.hasMoreThanOneImage() : this.map.hasMoreThanOneImage();

        this.hasWaypointTrigger = false;
        for (Map.Entry<ResourceLocation, TriggerButton> entry : this.waypointTriggers.entries()) {
            final boolean visible = Objects.equals(entry.getKey(), this.selectedWaypoint);
            this.hasWaypointTrigger = this.hasWaypointTrigger || visible;
            entry.getValue().visible = visible;
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
        this.renderWaypointTexture(mc, transforms, x0, y0, x1, partialTicks);
        this.renderMapTexture(mc, transforms, x0, y0, x1);

        super.render(transforms, mouseX, mouseY, partialTicks);

        this.renderTextCollection(this.font, transforms, x0, y0, x1);
    }

    private void renderTextCollection(FontRenderer font, MatrixStack transforms, int x0, int y0, int x1) {
        // Display the subtitle/desc of the map if no waypoint is selected
        ITextComponent title = this.map.getTitle(), subtitle = this.map.getSubtitle(), desc = this.map.getDesc();
        if (this.selectedWaypoint != null) {
            Waypoint wp = SignMeUpClient.MANAGER.findWaypoint(this.selectedWaypoint);
            if (wp != null) {
                subtitle = wp.getTitle();
                desc = wp.getDesc();
            }
        }
        // Draw title and subtitle depending on whether a waypoint is selected
        final int xTitle = x0 + 142 - font.getStringPropertyWidth(title) / 2;
        font.drawText(transforms, title, xTitle, y0 + 7F, 0x404040);
        final int xSubtitle = x1 + 56 - font.getStringPropertyWidth(subtitle) / 2;
        font.drawText(transforms, subtitle, xSubtitle, y0 + 7F, 0x404040);
        // I DISLIKE THIS METHOD BECAUSE IT FAILS TO HANDLE LINE BREAKING
        // A proper line breaking algorithm should comply with UAX #14, link below:
        // http://www.unicode.org/reports/tr14/
        // However it at least get things work for now. So it is the status quo.
        List<IReorderingProcessor> displayedDescList = font.trimStringToWidth(desc, 90);
        // Draw desc text
        for (int i = 0, size = Math.min(8, displayedDescList.size()); i < size; ++i) {
            font.func_238422_b_(transforms, displayedDescList.get(i), x1 + 10F, y0 + 81F + 9 * i, 0x404040);
        }
    }

    @SuppressWarnings("deprecation")
    private void renderMapTexture(Minecraft mc, MatrixStack transforms, int x0, int y0, int x1) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        // corner of the in-game window
        mc.textureManager.bindTexture(this.map.texture);
        // Who said we have to use 128 * 128 texture?
        blit(transforms, x0 + 78, y0 + 23, 0, 0, 128, 128, 128, 128);
    }

    @SuppressWarnings("deprecation")
    private void renderWaypointTexture(Minecraft mc, MatrixStack transforms, int x0, int y0, int x1, float partialTicks) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        ResourceLocation image = this.map.getDisplayingImageId();
        if (this.selectedWaypoint != null) {
            Waypoint wp = SignMeUpClient.MANAGER.findWaypoint(this.selectedWaypoint);
            if (wp != null) {
                image = wp.getDisplayingImageId();
            }
        }
        if (this.lastWaypointTexture == null) {
            this.lastWaypointTexture = image;
        } else if (this.lastWaypointTexture.equals(image)) {
            this.lastWaypointTextureOpacity = 100;
        } else if (this.lastWaypointTextureOpacity > 0) {
            final float alpha = MathHelper.lerp(partialTicks, this.lastWaypointTextureOpacityPrev, this.lastWaypointTextureOpacity) / 100F;
            mc.textureManager.bindTexture(this.lastWaypointTexture);
            blit(transforms, x1 + 7, y0 + 20, 0, 0, 96, 54, 96, 54);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F - alpha);
            mc.textureManager.bindTexture(GUIDE_MAP_RIGHT);
            blit(transforms, x1 + 7, y0 + 20, 7, 20, 96, 54);
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
        if (!this.hasWaypointTrigger) {
            blit(transforms, x1 + 108, y0 + 20, 181, 20, 64, 134);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x0 = (this.width - X_SIZE) / 2, y0 = (this.height - Y_SIZE) / 2;
        // Reset selected Wp when clicking everywhere on the map
        if (mouseX >= x0 + 78 && mouseX < x0 + 206 && mouseY >= y0 + 23 && mouseY < y0 + 151) {
            if (this.selectedWaypoint != null) {
                this.selectedWaypoint = null;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class FlipHandler implements Button.IPressable {
        private final int diff;

        private FlipHandler(int diff) {
            this.diff = diff;
        }

        @Override
        public void onPress(Button p_onPress_1_) {
            if (selectedWaypoint != null) {
                Waypoint wp = SignMeUpClient.MANAGER.findWaypoint(selectedWaypoint);
                if (wp != null) {
                    wp.modifyDisplayingImageIndex(this.diff);
                    return;
                }
            }
            map.modifyDisplayingImageIndex(this.diff);
        }
    }

    private final class TriggerButton extends ImageButton {
        private final Trigger trigger;

        private TriggerButton(int x, int y, int width, int height, int uOffset, int vOffset, int vDiff,
                              ResourceLocation image, Trigger trigger, IPressable pressable) {
            super(x, y, width, height, uOffset, vOffset, vDiff, image, pressable);
            this.trigger = trigger;
        }

        @Override
        public void renderWidget(MatrixStack transforms, int mouseX, int mouseY, float partialTicks) {
            super.renderWidget(transforms, mouseX, mouseY, partialTicks);
            final FontRenderer font = Minecraft.getInstance().fontRenderer;
            final int stringWidth = font.getStringPropertyWidth(this.trigger.getTitle());
            final int x0 = this.x + this.width / 2 - stringWidth / 2, y0 = this.y + (this.height - 8) / 2;
            font.drawText(transforms, this.trigger.getTitle(), x0, y0, this.trigger.disabled ? 0xFFFFFF : 0x404040);
            if (this.isHovered()) {
                GuideMapScreen.this.renderTooltip(transforms, this.trigger.getDesc(), mouseX, mouseY);
            }
        }
    }
}
