package org.teacon.signin.client;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
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
public final class GuideMapScreen extends Screen {

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

    private final Queue<Pair<Component, Component>> queuedTips = Queues.newArrayDeque();

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
                this.addRenderableWidget(new ImageButton(
                        mapCanvasX + wpX - 2, mapCanvasY + wpY - 2, 4, 4,
                        58, 2, 0, MAP_ICONS, 128, 128, (btn) -> this.selectedWaypoint = wpId, wp.getTitle()) {
                    @Override
                    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
                        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
                        if (this.isHovered()) {
                            double distance = Math.sqrt(wp.getActualLocation().distToCenterSqr(GuideMapScreen.this.playerLocation));
                            GuideMapScreen.this.queuedTips.offer(Pair.of(wp.getTitle(), GuideMapScreen.this.toDistanceText(distance)));
                        }
                    }
                });
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

    private Component toDistanceText(double distance) {
        return Component.translatable("sign_up.waypoint.distance", "%.1f".formatted(distance));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x0 = (this.width - X_SIZE) / 2, y0 = (this.height - Y_SIZE) / 2, x1 = x0 + 206;

        this.renderBackground(guiGraphics);
        this.renderBackgroundTexture(guiGraphics, x0, y0, x1);
        this.renderWaypointTexture(guiGraphics, x0, y0, x1, partialTick);
        this.renderMapTexture(guiGraphics, x0, y0, x1);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.renderPlayerHeads(guiGraphics, mouseX, mouseY, x0, y0, x1);
        this.renderTextCollection(guiGraphics, mouseX, mouseY, x0, y0, x1);
    }

    private void renderTextCollection(GuiGraphics guiGraphics, int mouseX, int mouseY, int x0, int y0, int x1) {
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
        guiGraphics.drawString(font, title, xTitle, y0 + 7, 0x404040, false);
        final int xSubtitle = x1 + 56 - font.width(subtitle) / 2;
        guiGraphics.drawString(font, subtitle, xSubtitle, y0 + 7, 0x404040, false);
        // I DISLIKE THIS METHOD BECAUSE IT FAILS TO HANDLE LINE BREAKING
        // A proper line breaking algorithm should comply with UAX #14, link below:
        // http://www.unicode.org/reports/tr14/
        // However it at least get things work for now. So it is the status quo.
        List<FormattedCharSequence> displayedDescList = font.split(desc, 90);
        // Draw desc text
        for (int i = 0, size = Math.min(8, displayedDescList.size()); i < size; ++i) {
            guiGraphics.drawString(font, displayedDescList.get(i), x1 + 10, y0 + 81 + 9 * i, 0x404040, false);
        }
        List<Component> tooltipTexts = new ArrayList<>(this.queuedTips.size() * 2 + 1);
        for (Pair<Component, Component> pair = this.queuedTips.poll(); pair != null; pair = this.queuedTips.poll()) {
            if (!tooltipTexts.isEmpty()) {
                tooltipTexts.add(Component.empty());
            }
            tooltipTexts.add(pair.getFirst());
            tooltipTexts.add(pair.getSecond());
        }
        if (!tooltipTexts.isEmpty()) {
            guiGraphics.renderComponentTooltip(font, tooltipTexts, mouseX, mouseY);
        }
    }

    private void renderPlayerHeads(GuiGraphics guiGraphics, int mouseX, int mouseY, int x0, int y0, int x1) {
        Minecraft mc = Objects.requireNonNull(this.minecraft);
        List<? extends Player> players = mc.level == null ? List.of() : mc.level.players();

        int size = players.size();
        double[] inputX = new double[size], inputY = new double[size];
        double[] outputX = new double[size], outputY = new double[size];

        int currentIndex = size;
        Vec3i center = this.map.center;
        UUID current = mc.player == null ? null : mc.player.getUUID();
        for (int i = 0; i < size; ++i) {
            Player player = players.get(i);
            if (player.getUUID().equals(current)) {
                currentIndex = i;
            }
            inputX[i] = player.getX() - center.getX();
            inputY[i] = player.getZ() - center.getY();
        }
        this.mapping.interpolate(inputX, inputY, outputX, outputY);

        // make sure that current player is rendered at last
        for (int i = currentIndex - 1; i >= 0; --i) {
            this.renderPlayerHead(guiGraphics, mouseX, mouseY, x0, y0, players.get(i), outputX[i], outputY[i]);
        }
        for (int i = size - 1; i >= currentIndex; --i) {
            this.renderPlayerHead(guiGraphics, mouseX, mouseY, x0, y0, players.get(i), outputX[i], outputY[i]);
        }
    }

    private void renderPlayerHead(GuiGraphics guiGraphics,
                                  int mouseX, int mouseY, int x0, int y0,
                                  Player player, double outputX, double outputY) {
        int wpX = Math.round((float) outputX / this.map.radius * 64) + 64;
        int wpY = Math.round((float) outputY / this.map.radius * 64) + 64;
        if (wpX >= 1 && wpX <= 127 && wpY >= 1 && wpY <= 127) {
            // could we have dinnerbone or grumm joined our server?
            if (LivingEntityRenderer.isEntityUpsideDown(player)) {
                guiGraphics.blit(DefaultPlayerSkin.getDefaultSkin(player.getUUID()), x0 + 76 + wpX, y0 + 21 + wpY, 4, 4, 8, 16, 8, -8, 64, 64);
            } else {
                guiGraphics.blit(DefaultPlayerSkin.getDefaultSkin(player.getUUID()), x0 + 76 + wpX, y0 + 21 + wpY, 4, 4, 8, 8, 8, 8, 64, 64);
            }
            if (mouseX >= x0 + 76 + wpX && mouseX < x0 + 80 + wpX) {
                if (mouseY >= y0 + 21 + wpY && mouseY < y0 + 25 + wpY) {
                    double distance = player.position().distanceTo(this.playerLocation);
                    this.queuedTips.offer(Pair.of(player.getDisplayName(), this.toDistanceText(distance)));
                }
            }
        }
    }

    private void renderMapTexture(GuiGraphics guiGraphics, int x0, int y0, int x1) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // corner of the in-game window
        // Who said we have to use 128 * 128 texture?
        guiGraphics.blit(this.map.texture, x0 + 78, y0 + 23, 0, 0, 128, 128, 128, 128);
    }

    private void renderWaypointTexture(GuiGraphics guiGraphics, int x0, int y0, int x1, float partialTicks) {
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
            guiGraphics.blit(head, x1 + 7, y0 + 20, 0, 0, 96, 54, 96, 54);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            guiGraphics.blit(GUIDE_MAP_RIGHT, x1 + 7, y0 + 20, 7, 20, 96, 54);
            image = this.lastWaypointTextures.getFirst();
        }
        this.lastWaypointTextures.addFirst(head);
        guiGraphics.blit(image, x1 + 7, y0 + 20, 0, 0, 96, 54, 96, 54);
    }

    private void renderBackgroundTexture(GuiGraphics guiGraphics, int x0, int y0, int x1) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(GUIDE_MAP_LEFT, x0, y0, 0, 0, 211, 161);
        guiGraphics.blit(GUIDE_MAP_LEFT, x0 + 6, y0 + 138, 66, 201, 66, 17);
        guiGraphics.blit(GUIDE_MAP_RIGHT, x1 + 5, y0, 5, 0, 174, 161);
        if (!this.hasWaypointTrigger) {
            guiGraphics.blit(GUIDE_MAP_RIGHT, x1 + 108, y0 + 20, 181, 20, 64, 134);
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

    private final class FlipHandler implements Button.OnPress {
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
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            //final Font font = Minecraft.getInstance().font;
            final int stringWidth = font.width(this.trigger.getTitle());
            final int x0 = this.getX() + this.width / 2 - stringWidth / 2, y0 = this.getY() + (this.height - 8) / 2;
            guiGraphics.drawString(font, this.trigger.getTitle(), x0, y0, this.trigger.disabled ? 0xFFFFFF : 0x404040, false);
            if (this.isHovered) {
                guiGraphics.renderTooltip(font, this.trigger.getDesc(), mouseX, mouseY);
            }
        }
    }
}
