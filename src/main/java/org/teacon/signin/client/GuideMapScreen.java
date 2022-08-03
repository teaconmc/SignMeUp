package org.teacon.signin.client;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.teacon.signin.SignMeUp;
import org.teacon.signin.data.GuideMap;
import org.teacon.signin.data.Trigger;
import org.teacon.signin.data.Waypoint;
import org.teacon.signin.network.TriggerFromMapPacket;
import org.teacon.signin.network.TriggerFromWaypointPacket;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class GuideMapScreen extends Screen
{

    private static final Logger LOGGER = LogManager.getLogger("SignMeUp");
    private static final Marker MARKER = MarkerManager.getMarker("GuideMapScreen");

    private static final int X_SIZE = 385;
    private static final int Y_SIZE = 161;

    public final ResourceLocation mapId;

    private static final ResourceLocation MAP_ICONS = new ResourceLocation("minecraft", "textures/map/map_icons.png");
    private static final ResourceLocation GUIDE_MAP_LEFT = new ResourceLocation("sign_up", "textures/gui/guide_map_left.png");
    private static final ResourceLocation GUIDE_MAP_RIGHT = new ResourceLocation("sign_up", "textures/gui/guide_map_right.png");

    private int mapTriggerPage = 0;
    private int mapTriggerPageSize = 0;
    private ResourceLocation selectedWaypoint;

    private int ticksAfterWaypointTextureChanged = 0;
    private final Deque<ResourceLocation> lastWaypointTextures = Queues.newArrayDeque();

    private boolean hasWaypointTrigger = false;

    private final GuideMap map;
    private final Vec3 playerLocation;
    private final List<ResourceLocation> waypointIds;

    private PolynomialMapping mapping;

    private ImageButton leftFlip, rightFlip;
    private ImageButton mapTriggerPrev, mapTriggerNext;

    private final List<TriggerButton> mapTriggers = Lists.newArrayList();
    private final ListMultimap<ResourceLocation, TriggerButton> waypointTriggers = ArrayListMultimap.create();

    private boolean needRefresh = false;

    public GuideMapScreen(ResourceLocation mapId, GuideMap map, Vec3 location) {
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
        this.leftFlip = this.addRenderableWidget(new ImageButton(x1 + 7, y0 + 20, 10, 54, 155, 163, 0, GUIDE_MAP_RIGHT, new FlipHandler(1)));
        this.rightFlip = this.addRenderableWidget(new ImageButton(x1 + 93, y0 + 20, 10, 54, 167, 163, 0, GUIDE_MAP_RIGHT, new FlipHandler(-1)));

        // Prev and next page for map triggers
        this.mapTriggerPrev = this.addRenderableWidget(new ImageButton(x0 + 6, y0 + 138, 33, 17, 66, 163, 19, GUIDE_MAP_LEFT, (btn) -> --this.mapTriggerPage));
        this.mapTriggerNext = this.addRenderableWidget(new ImageButton(x0 + 39, y0 + 138, 33, 17, 99, 163, 19, GUIDE_MAP_LEFT, (btn) -> ++this.mapTriggerPage));

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
            final TriggerButton btn = this.addRenderableWidget(new TriggerButton(x0 + 8, y0 + 21 + (j % 6) * 19, 62, 18,
                    2, trigger.disabled ? 203 : 163, trigger.disabled ? 0 : 20, GUIDE_MAP_LEFT, trigger,
                    (b) -> SignMeUp.channel.sendToServer(new TriggerFromMapPacket(this.mapId, triggerId))));
            this.mapTriggers.add(btn);
            btn.visible = false;
            ++j;
        }

        // Collect Waypoints
        this.waypointTriggers.clear();
        int mapCanvasX = x0 + 78, mapCanvasY = y0 + 23;
        final List<Waypoint> waypoints = new ArrayList<>(this.waypointIds.size());
        final List<ResourceLocation> waypointIds = new ArrayList<>(this.waypointIds.size());
        for (ResourceLocation wpId : this.waypointIds) {
            Waypoint wp = SignMeUpClient.MANAGER.findWaypoint(wpId);
            if (wp != null) {
                waypoints.add(wp);
                waypointIds.add(wpId);
            }
        }

        // Setup Mapping
        final int waypointSize = waypoints.size();
        final double[] inputX = new double[waypointSize], inputY = new double[waypointSize];
        final double[] outputX = new double[waypointSize], outputY = new double[waypointSize];
        final Vec3i center = this.map.center;
        for (int i = 0; i < waypointSize; ++i) {
            final Waypoint wp = waypoints.get(i);
            final Vec3i actualLocation = wp.getActualLocation();
            final Vec3i renderLocation = wp.getRenderLocation();
            inputX[i] = actualLocation.getX() - center.getX();
            inputY[i] = actualLocation.getZ() - center.getZ();
            outputX[i] = renderLocation.getX() - center.getX();
            outputY[i] = renderLocation.getZ() - center.getZ();
        }
        try {
            this.mapping = new PolynomialMapping(inputX, inputY, outputX, outputY);
            LOGGER.info(MARKER, "Generated the mapping for {} waypoint(s).", waypointSize);
            LOGGER.debug(MARKER, "The mapping is: {}", this.mapping);
        } catch (IllegalArgumentException e) {
            this.mapping = new PolynomialMapping(new double[0], new double[0], new double[0], new double[0]);
            LOGGER.warn(MARKER, "Unable to generate mapping for the map.", e);
        }

        // Setup Waypoints
        for (int i = 0; i < waypointSize; ++i) {
            final Waypoint wp = waypoints.get(i);
            final ResourceLocation wpId = waypointIds.get(i);
            final int wpX = Math.round((float) outputX[i] / this.map.radius * 64) + 64;
            final int wpY = Math.round((float) outputY[i] / this.map.radius * 64) + 64;
            if (wpX >= 1 && wpX <= 127 && wpY >= 1 && wpY <= 127) {
                // Setup Waypoints as ImageButtons
                this.addRenderableWidget(new ImageButton(mapCanvasX + wpX - 2, mapCanvasY + wpY - 2, 4, 4, 58, 2, 0, MAP_ICONS,
                        128, 128, (btn) -> this.selectedWaypoint = wpId, (btn, transform, mouseX, mouseY) -> {
                    double distance = Math.sqrt(wp.getActualLocation().distToCenterSqr(this.playerLocation));
                    this.renderComponentTooltip(transform, Arrays.asList(
                            wp.getTitle(),
                            new TranslatableComponent("sign_up.waypoint.distance",
                                    Math.round(distance * 10.0) / 10.0)
                    ), mouseX, mouseY);
                }, wp.getTitle()));
                // Setup trigger buttons from Waypoints
                List<ResourceLocation> wpTriggerIds = wp.getTriggerIds();
                for (int j = 0, k = 0; k < wpTriggerIds.size() && j < 7; ++k) {
                    ResourceLocation triggerId = wpTriggerIds.get(k);
                    final Trigger trigger = SignMeUpClient.MANAGER.findTrigger(triggerId);
                    if (trigger == null) {
                        continue;
                    }
                    TriggerButton btn = this.addRenderableWidget(new TriggerButton(x1 + 109, y0 + 21 + j * 19, 62, 18,
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

        ++this.ticksAfterWaypointTextureChanged;

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
    public void render(PoseStack transforms, int mouseX, int mouseY, float partialTicks) {
        final Minecraft mc = Objects.requireNonNull(this.minecraft);

        int x0 = (this.width - X_SIZE) / 2, y0 = (this.height - Y_SIZE) / 2, x1 = x0 + 206;

        this.renderBackground(transforms);
        this.renderBackgroundTexture(mc, transforms, x0, y0, x1);
        this.renderWaypointTexture(mc, transforms, x0, y0, x1, partialTicks);
        this.renderMapTexture(mc, transforms, x0, y0, x1);

        super.render(transforms, mouseX, mouseY, partialTicks);

        this.renderTextCollection(transforms, x0, y0, x1);
    }

    private void renderTextCollection(PoseStack transforms, int x0, int y0, int x1) {
        // Display the subtitle/desc of the map if no waypoint is selected
        Component title = this.map.getTitle(), subtitle = this.map.getSubtitle(), desc = this.map.getDesc();
        if (this.selectedWaypoint != null) {
            Waypoint wp = SignMeUpClient.MANAGER.findWaypoint(this.selectedWaypoint);
            if (wp != null) {
                subtitle = wp.getTitle();
                desc = wp.getDesc();
            }
        }
        // Draw title and subtitle depending on whether a waypoint is selected
        final int xTitle = x0 + 142 - font.width(title) / 2;
        font.draw(transforms, title, xTitle, y0 + 7F, 0x404040);
        final int xSubtitle = x1 + 56 - font.width(subtitle) / 2;
        font.draw(transforms, subtitle, xSubtitle, y0 + 7F, 0x404040);
        // I DISLIKE THIS METHOD BECAUSE IT FAILS TO HANDLE LINE BREAKING
        // A proper line breaking algorithm should comply with UAX #14, link below:
        // http://www.unicode.org/reports/tr14/
        // However it at least get things work for now. So it is the status quo.
        List<FormattedCharSequence> displayedDescList = font.split(desc, 90);
        // Draw desc text
        for (int i = 0, size = Math.min(8, displayedDescList.size()); i < size; ++i) {
            font.draw(transforms, displayedDescList.get(i), x1 + 10F, y0 + 81F + 9 * i, 0x404040);
        }
    }

    @SuppressWarnings("deprecation")
    private void renderMapTexture(Minecraft mc, PoseStack transforms, int x0, int y0, int x1) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // corner of the in-game window
        RenderSystem.setShaderTexture(0, this.map.texture);
        // Who said we have to use 128 * 128 texture?
        blit(transforms, x0 + 78, y0 + 23, 0, 0, 128, 128, 128, 128);
    }

    @SuppressWarnings("deprecation")
    private void renderWaypointTexture(Minecraft mc, PoseStack transforms, int x0, int y0, int x1, float partialTicks) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        ResourceLocation image = this.map.getDisplayingImageId();
        if (this.selectedWaypoint != null) {
            Waypoint wp = SignMeUpClient.MANAGER.findWaypoint(this.selectedWaypoint);
            if (wp != null) {
                image = wp.getDisplayingImageId();
            }
        }
        // Drop images that will not be used in the future
        float alpha = (this.ticksAfterWaypointTextureChanged + partialTicks) / 4F;
        while (alpha >= 1) {
            this.ticksAfterWaypointTextureChanged -= 4;
            this.lastWaypointTextures.pollFirst();
            --alpha;
        }
        // Ensure that the last image is current image
        final ResourceLocation tail = this.lastWaypointTextures.peekLast();
        if (!image.equals(tail)) {
            this.lastWaypointTextures.addLast(image);
        }
        // If there are more than one images, render both of them, otherwise set the timer to zero
        final ResourceLocation head = this.lastWaypointTextures.removeFirst();
        if (this.lastWaypointTextures.isEmpty()) {
            this.ticksAfterWaypointTextureChanged = 0;
        } else {
            RenderSystem.setShaderTexture(0, head);
            blit(transforms, x1 + 7, y0 + 20, 0, 0, 96, 54, 96, 54);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            RenderSystem.setShaderTexture(0, GUIDE_MAP_RIGHT);
            blit(transforms, x1 + 7, y0 + 20, 7, 20, 96, 54);
            image = this.lastWaypointTextures.getFirst();
        }
        this.lastWaypointTextures.addFirst(head);
        RenderSystem.setShaderTexture(0, image);
        blit(transforms, x1 + 7, y0 + 20, 0, 0, 96, 54, 96, 54);
    }

    @SuppressWarnings("deprecation")
    private void renderBackgroundTexture(Minecraft mc, PoseStack transforms, int x0, int y0, int x1) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUIDE_MAP_LEFT);
        blit(transforms, x0, y0, 0, 0, 211, 161);
        blit(transforms, x0 + 6, y0 + 138, 66, 201, 66, 17);
        RenderSystem.setShaderTexture(0, GUIDE_MAP_RIGHT);
        blit(transforms, x1 + 5, y0, 5, 0, 174, 161);
        if (!this.hasWaypointTrigger) {
            blit(transforms, x1 + 108, y0 + 20, 181, 20, 64, 134);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!super.mouseClicked(mouseX, mouseY, button)) {
            int x0 = (this.width - X_SIZE) / 2, y0 = (this.height - Y_SIZE) / 2;
            // Reset selected Wp when clicking everywhere on the map
            if (mouseX >= x0 + 78 && mouseX < x0 + 206 && mouseY >= y0 + 23 && mouseY < y0 + 151) {
                if (this.selectedWaypoint != null) {
                    this.selectedWaypoint = null;
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class FlipHandler implements Button.OnPress
    {
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
                              ResourceLocation image, Trigger trigger, OnPress pressable) {
            super(x, y, width, height, uOffset, vOffset, vDiff, image, pressable);
            this.trigger = trigger;
        }

        @Override
        public void renderButton(PoseStack transforms, int mouseX, int mouseY, float partialTicks) {
            super.renderButton(transforms, mouseX, mouseY, partialTicks);
            //final Font font = Minecraft.getInstance().font;
            final int stringWidth = font.width(this.trigger.getTitle());
            final int x0 = this.x + this.width / 2 - stringWidth / 2, y0 = this.y + (this.height - 8) / 2;
            font.draw(transforms, this.trigger.getTitle(), x0, y0, this.trigger.disabled ? 0xFFFFFF : 0x404040);
            if (this.isHovered) {
                GuideMapScreen.this.renderTooltip(transforms, this.trigger.getDesc(), mouseX, mouseY);
            }
        }
    }
}
