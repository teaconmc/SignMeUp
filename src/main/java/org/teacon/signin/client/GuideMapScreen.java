package org.teacon.signin.client;

import com.google.common.collect.*;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.teacon.signin.SignMeUp;
import org.teacon.signin.data.entity.GuideMap;
import org.teacon.signin.data.entity.Trigger;
import org.teacon.signin.data.entity.Waypoint;
import org.teacon.signin.network.TriggerFromMapPacket;
import org.teacon.signin.network.TriggerFromWaypointPacket;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class GuideMapScreen extends Screen {

    private static final Logger LOGGER = LogManager.getLogger("SignMeUp");
    private static final Marker MARKER = MarkerManager.getMarker("GuideMapScreen");

    private static final int X_SIZE = 384;
    private static final int Y_SIZE = 288;

    private static final ResourceLocation GUIDE_MAP_TEXTURE = new ResourceLocation("sign_up", "textures/gui/texture.png");

    public final ResourceLocation mapId;


    private final GuideMap map;
    private final Vec3 playerLocation;
    private final List<ResourceLocation> waypointIds;
    private final SideState sideState = new SideState();

    private PolynomialMapping mapping;

    private ImageButton leftFlip, rightFlip;
    private ImageButton sidePrevious, sideNext;
    private ImageButton switchToWaypoint, switchToMap;
    private final List<TriggerButton> mapTriggers = Lists.newArrayList();
    private final Map<ResourceLocation, ImageButton> waypoints = Maps.newLinkedHashMap();
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
        int x0 = (this.width - X_SIZE) / 2, y0 = (this.height - Y_SIZE) / 2;

        // Left and Right Image Flip Button
        this.leftFlip = this.addRenderableWidget(Util.make(new ImageButton(
                x0 + 307, y0 + 257, 23, 14, 16, 304, 14, GUIDE_MAP_TEXTURE, 768, 384,
                b -> this.sideState.rotateWaypointTexture(-1)), b -> b.visible = false));
        this.rightFlip = this.addRenderableWidget(Util.make(new ImageButton(
                x0 + 333, y0 + 257, 23, 14, 42, 304, 14, GUIDE_MAP_TEXTURE, 768, 384,
                b -> this.sideState.rotateWaypointTexture(1)), b -> b.visible = false));

        // Prev and next page for triggers
        this.sidePrevious = this.addRenderableWidget(new ImageButton(
                x0 + 23, y0 + 250, 33, 17, 68, 304, 17, GUIDE_MAP_TEXTURE, 768, 384, b -> this.sideState.prev()));
        this.sideNext = this.addRenderableWidget(new ImageButton(
                x0 + 68, y0 + 250, 33, 17, 104, 304, 17, GUIDE_MAP_TEXTURE, 768, 384, b -> this.sideState.next()));

        // Switch to map or waypoint
        this.switchToWaypoint = this.addRenderableWidget(Util.make(new ImageButton(
                x0 + 76, y0 + 69, 33, 23, 176, 304, 23, GUIDE_MAP_TEXTURE, 768, 384,
                b -> this.sideState.switchToLastWaypointIfExists()), b -> b.setTooltip(
                        Tooltip.create(Component.translatable("sign_up.text.switch_to_point")))));
        this.switchToMap = this.addRenderableWidget(Util.make(new ImageButton(
                x0 + 76, y0 + 42, 33, 23, 140, 304, 23, GUIDE_MAP_TEXTURE, 768, 384,
                b -> this.sideState.switchToMap(this.mapTriggers.size())), b -> b.setTooltip(
                        Tooltip.create(Component.translatable("sign_up.text.switch_to_map")))));

        // Setup Map Trigger Buttons
        this.mapTriggers.clear();
        var mapTriggerIds = this.map.getTriggerIds();
        for (int i = 0, j = 0; i < mapTriggerIds.size(); ++i) {
            var triggerId = mapTriggerIds.get(i);
            var trigger = SignMeUpClient.MANAGER.findTrigger(triggerId);
            if (trigger != null) {
                this.mapTriggers.add(this.addRenderableWidget(Util.make(new TriggerButton(
                        x0 + 23, y0 + 102 + (j++ % 7) * 21, 78, 16, 212, 304, 16, GUIDE_MAP_TEXTURE, 768, 384, trigger,
                        b -> SignMeUp.channel.sendToServer(new TriggerFromMapPacket(this.mapId, triggerId))), b -> {
                    b.active = !trigger.disabled;
                    b.visible = false;
                })));
            }
        }
        this.sideState.switchToMap(this.mapTriggers.size());

        // Collect Waypoints
        int mapCanvasX = x0 + 109, mapCanvasY = y0 + 18;
        var waypoints = new ArrayList<Waypoint>(this.waypointIds.size());
        var waypointIds = new ArrayList<ResourceLocation>(this.waypointIds.size());
        for (var wpId : this.waypointIds) {
            var wp = SignMeUpClient.MANAGER.findWaypoint(wpId);
            if (wp != null) {
                waypoints.add(wp);
                waypointIds.add(wpId);
            }
        }

        // Setup Mapping
        int waypointSize = waypoints.size();
        double[] inputX = new double[waypointSize], inputY = new double[waypointSize];
        double[] outputX = new double[waypointSize], outputY = new double[waypointSize];
        Vec3i center = this.map.center;
        for (int i = 0; i < waypointSize; ++i) {
            Waypoint wp = waypoints.get(i);
            Vec3i actualLocation = wp.getActualLocation();
            Vec3i renderLocation = wp.getRenderLocation();
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
        this.waypoints.clear();
        this.waypointTriggers.clear();
        for (int i = 0; i < waypointSize; ++i) {
            var wp = waypoints.get(i);
            var wpId = waypointIds.get(i);
            var wpTriggerIds = wp.getTriggerIds();
            int wpX = Math.round((float) outputX[i] / this.map.radius * 128) + 128;
            int wpY = Math.round((float) outputY[i] / this.map.radius * 128) + 128;
            if (wpX >= 1 && wpX <= 255 && wpY >= 1 && wpY <= 255) {
                // Setup Waypoints as ImageButtons
                this.waypoints.put(wpId, this.addRenderableWidget(Util.make(new ImageButton(
                        mapCanvasX + wpX - 3, mapCanvasY + wpY - 3, 6, 6, 293, 304, 6, GUIDE_MAP_TEXTURE, 768, 384,
                        b -> this.sideState.switchToWaypoint(wpTriggerIds.size(), wpId, wp.getImageIds()), wp.getTitle()) {
                    @Override
                    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
                        super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
                        if (this.isHovered()) {
                            var distance = Math.sqrt(wp.getActualLocation().distToCenterSqr(GuideMapScreen.this.playerLocation));
                            GuideMapScreen.this.queuedTips.offer(Pair.of(wp.getTitle(), GuideMapScreen.this.toDistanceText(distance)));
                        }
                    }
                }, b -> b.visible = false)));
                // Setup trigger buttons from Waypoints
                for (int j = 0, k = 0; k < wpTriggerIds.size(); ++k) {
                    var triggerId = wpTriggerIds.get(k);
                    var trigger = SignMeUpClient.MANAGER.findTrigger(triggerId);
                    if (trigger != null) {
                        this.waypointTriggers.put(wpId, this.addRenderableWidget(Util.make(new TriggerButton(
                                x0 + 23, y0 + 102 + (j++ % 7) * 21, 78, 16, 212, 304, 16, GUIDE_MAP_TEXTURE, 768, 384, trigger,
                                b -> SignMeUp.channel.sendToServer(new TriggerFromWaypointPacket(wpId, triggerId))), b -> {
                            b.active = !trigger.disabled;
                            b.visible = false;
                        })));
                    }
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

        var sideMin = this.sideState.getSideMin();
        var sideMax = this.sideState.getSideMax();
        var wpCurrentId = this.sideState.getWaypointId();

        this.leftFlip.visible = this.rightFlip.visible = wpCurrentId != null;
        this.leftFlip.active = this.rightFlip.active = this.sideState.isWaypointTextureMoreThanOne();

        this.sideNext.active = !this.sideState.isLast();
        this.sidePrevious.active = !this.sideState.isFirst();

        this.switchToMap.active = true;
        this.switchToWaypoint.active = this.sideState.isLastWaypointAvailable();

        for (var waypointButton : this.waypoints.values()) {
            waypointButton.visible = wpCurrentId == null;
        }

        for (int i = 0, size = this.mapTriggers.size(); i < size; ++i) {
            this.mapTriggers.get(i).visible = wpCurrentId == null && i >= sideMin && i <= sideMax;
        }

        for (var wpId : this.waypointTriggers.keySet()) {
            var triggerButtons = this.waypointTriggers.get(wpId);
            for (int i = 0, size = triggerButtons.size(); i < size; ++i) {
                triggerButtons.get(i).visible = wpCurrentId == wpId && i >= sideMin && i <= sideMax;
            }
        }
    }

    private Component toDistanceText(double distance) {
        return Component.translatable("sign_up.waypoint.distance", "%.1f".formatted(distance));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int x0 = (this.width - X_SIZE) / 2, y0 = (this.height - Y_SIZE) / 2;
        var waypointId = this.sideState.getWaypointId();

        this.renderBackground(guiGraphics);
        if (waypointId == null) {
            this.renderBackgroundTexture(guiGraphics, x0, y0);
            this.renderMapTexture(guiGraphics, x0, y0);
        } else {
            this.renderWaypointBackgroundTexture(guiGraphics, x0, y0);
            this.renderWaypointTexture(guiGraphics, x0, y0, this.sideState.getWaypointTexture());
        }
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        if (waypointId == null) {
            this.renderPlayerHeads(guiGraphics, mouseX, mouseY, x0, y0);
            this.renderTitle(guiGraphics, mouseX, mouseY, x0, y0);
            this.renderWaypointDebugText(guiGraphics);
        } else {
            this.renderTitle(guiGraphics, mouseX, mouseY, x0, y0);
            this.renderTextCollection(guiGraphics, mouseX, mouseY, x0, y0);
        }
        this.renderQueuedTooltips();
    }

    private void renderTitle(GuiGraphics guiGraphics, int mouseX, int mouseY, int x0, int y0) {
        guiGraphics.drawString(this.font, this.map.getTitle(), x0 + 24, y0 + 24, 0x3C352A, false);
        if (mouseX >= x0 + 22 && mouseX < x0 + 102) {
            if (mouseY >= y0 + 19 && mouseY < y0 + 37) {
                this.queuedTips.offer(Pair.of(this.map.getTitle(), this.map.getSubtitle()));
            }
        }
    }

    private void renderWaypointDebugText(GuiGraphics guiGraphics) {
        // Draw title at the left of waypoints
        if (this.getMinecraft().options.renderDebug) {
            guiGraphics.pose().pushPose();
            var source = guiGraphics.bufferSource();
            var pose = guiGraphics.pose().last().pose().scale(0.5F);
            for (var entry : this.waypoints.entrySet()) {
                var wp = SignMeUpClient.MANAGER.findWaypoint(entry.getKey());
                if (wp != null) {
                    var y1 = entry.getValue().getY() * 2.0F + 2.0F;
                    var wpTitle = wp.getTitle().getVisualOrderText();
                    var x1 = entry.getValue().getX() * 2.0F - this.font.width(wpTitle) + 2.0F;
                    this.font.drawInBatch8xOutline(wpTitle, x1, y1, 0xD6CCBE, 0x3C352A, pose, source, 0xF000F0);
                }
            }
            guiGraphics.pose().popPose();
        }
    }

    private void renderTextCollection(GuiGraphics guiGraphics, int mouseX, int mouseY, int x0, int y0) {
        // Display the subtitle/desc of the map if no waypoint is selected
        Component title = this.map.getTitle(), subtitle = this.map.getSubtitle(), desc = this.map.getDesc();
        double distance = Math.sqrt(this.map.center.distToCenterSqr(GuideMapScreen.this.playerLocation));
        if (this.sideState.getWaypointId() != null) {
            Waypoint wp = SignMeUpClient.MANAGER.findWaypoint(this.sideState.getWaypointId());
            if (wp != null) {
                distance = Math.sqrt(wp.getActualLocation().distToCenterSqr(GuideMapScreen.this.playerLocation));
                subtitle = this.map.getSubtitle();
                title = wp.getTitle();
                desc = wp.getDesc();
            }
        }
        // Draw title and subtitle depending on whether a waypoint is selected
        final int xTitle = x0 + 237 - this.font.width(title) / 2;
        guiGraphics.drawString(this.font, title, xTitle, y0 + 24, 0x3C352A, false);
        if (mouseX >= x0 + 112 && mouseX < x0 + 262) {
            if (mouseY >= y0 + 22 && mouseY < y0 + 42) {
                this.queuedTips.offer(Pair.of(subtitle, this.toDistanceText(distance)));
            }
        }
        // I DISLIKE THIS METHOD BECAUSE IT FAILS TO HANDLE LINE BREAKING
        // A proper line breaking algorithm should comply with UAX #14, link below:
        // http://www.unicode.org/reports/tr14/
        // However it at least get things work for now. So it is the status quo.
        List<FormattedCharSequence> displayedDescList = this.font.split(desc, 240);
        // Draw desc text
        for (int i = 0, size = Math.min(7, displayedDescList.size()); i < size; ++i) {
            guiGraphics.drawString(this.font, displayedDescList.get(i), x0 + 117, y0 + 42 + 10 * i, 0x3C352A, false);
        }
    }

    private void renderQueuedTooltips() {
        List<FormattedCharSequence> tooltipTexts = new ArrayList<>(this.queuedTips.size() * 2 + 1);
        for (Pair<Component, Component> pair = this.queuedTips.poll(); pair != null; pair = this.queuedTips.poll()) {
            if (!tooltipTexts.isEmpty()) {
                tooltipTexts.add(Component.empty().getVisualOrderText());
            }
            tooltipTexts.add(pair.getFirst().getVisualOrderText());
            tooltipTexts.add(pair.getSecond().getVisualOrderText());
        }
        if (!tooltipTexts.isEmpty()) {
            this.setTooltipForNextRenderPass(tooltipTexts);
        }
    }

    private void renderPlayerHeads(GuiGraphics guiGraphics, int mouseX, int mouseY, int x0, int y0) {
        Minecraft mc = this.getMinecraft();
        Map<GameProfile, GlobalPos> players = SignMeUpClient.MANAGER.getAllPositions();

        int size = players.size();
        double[] inputX = new double[size], inputY = new double[size];
        double[] outputX = new double[size], outputY = new double[size];
        List<Map.Entry<GameProfile, GlobalPos>> chosenPlayers = new ArrayList<>(players.size());

        int index = 0;
        int currentIndex = size;
        Vec3i center = this.map.center;
        UUID current = mc.player == null ? null : mc.player.getUUID();
        ResourceKey<Level> dimension = mc.level == null ? null : mc.level.dimension();
        for (Map.Entry<GameProfile, GlobalPos> entry : players.entrySet()) {
            var playerPos = entry.getValue();
            if (playerPos.dimension().equals(dimension)) {
                UUID playerId = entry.getKey().getId();
                if (playerId != null && playerId.equals(current)) {
                    currentIndex = index;
                }
                inputX[index] = playerPos.pos().getX() + 0.5 - center.getX();
                inputY[index] = playerPos.pos().getZ() + 0.5 - center.getY();
                chosenPlayers.add(entry);
                index += 1;
            }
        }
        this.mapping.interpolate(inputX, inputY, outputX, outputY);

        // make sure that current player is rendered at last
        for (int i = currentIndex - 1; i >= 0; --i) {
            this.renderPlayerHead(guiGraphics, mouseX, mouseY, x0, y0, chosenPlayers.get(i), outputX[i], outputY[i]);
        }
        for (int i = size - 1; i >= currentIndex; --i) {
            this.renderPlayerHead(guiGraphics, mouseX, mouseY, x0, y0, chosenPlayers.get(i), outputX[i], outputY[i]);
        }
    }

    private void renderPlayerHead(GuiGraphics guiGraphics,
                                  int mouseX, int mouseY, int x0, int y0,
                                  Map.Entry<GameProfile, GlobalPos> entry, double outputX, double outputY) {
        SkinManager skinManager = this.getMinecraft().getSkinManager();
        int wpX = Math.round((float) outputX / this.map.radius * 128) + 128;
        int wpY = Math.round((float) outputY / this.map.radius * 128) + 128;
        if (wpX >= 1 && wpX <= 255 && wpY >= 1 && wpY <= 255) {
            int mapCanvasX = x0 + 109, mapCanvasY = y0 + 18;
            guiGraphics.blit(skinManager.getInsecureSkinLocation(entry.getKey()),
                    mapCanvasX + wpX - 2, mapCanvasY + wpY - 2, 4, 4, 8, 8, 8, 8, 64, 64);
            if (mouseX >= x0 + 107 + wpX && mouseX < x0 + 111 + wpX) {
                if (mouseY >= y0 + 16 + wpY && mouseY < y0 + 20 + wpY) {
                    Vec3 blockCenter = Vec3.atCenterOf(entry.getValue().pos());
                    double blockDiffX = Math.max(0.0, Math.abs(blockCenter.x - this.playerLocation.x) - 0.5);
                    double blockDiffY = Math.max(0.0, Math.abs(blockCenter.y - this.playerLocation.y) - 0.5);
                    double blockDiffZ = Math.max(0.0, Math.abs(blockCenter.z - this.playerLocation.z) - 0.5);
                    double d = Math.sqrt(blockDiffX * blockDiffX + blockDiffY * blockDiffY + blockDiffZ * blockDiffZ);
                    this.queuedTips.offer(Pair.of(Component.literal(entry.getKey().getName()), this.toDistanceText(d)));
                }
            }
        }
    }

    private void renderBackgroundTexture(GuiGraphics guiGraphics, int x0, int y0) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUIDE_MAP_TEXTURE);
        guiGraphics.blit(GUIDE_MAP_TEXTURE, x0, y0, 0, 0, X_SIZE, Y_SIZE, 768, 384);
    }

    private void renderMapTexture(GuiGraphics guiGraphics, int x0, int y0) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // corner of the in-game window
        // Who said we have to use 128 * 128 texture?
        guiGraphics.blit(this.map.texture, x0 + 109, y0 + 18, 0, 0, 256, 256, 256, 256);
    }

    private void renderWaypointBackgroundTexture(GuiGraphics guiGraphics, int x0, int y0) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUIDE_MAP_TEXTURE);
        guiGraphics.blit(GUIDE_MAP_TEXTURE, x0, y0, X_SIZE, 0, X_SIZE, Y_SIZE, 768, 384);
    }

    private void renderWaypointTexture(GuiGraphics guiGraphics, int x0, int y0, @Nullable ResourceLocation texture) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (texture != null) {
            guiGraphics.blit(texture, x0 + 117, y0 + 119, 0, 0, 240, 135, 240, 135);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class TriggerButton extends ImageButton {
        private final Trigger trigger;

        private TriggerButton(int x, int y, int width, int height, int uOffset, int vOffset, int vDiff,
                              ResourceLocation image, int texX, int texY, Trigger trigger, OnPress pressable) {
            super(x, y, width, height, uOffset, vOffset, vDiff, image, texX, texY, pressable);
            this.trigger = trigger;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
            //final Font font = Minecraft.getInstance().font;
            final int stringWidth = font.width(this.trigger.getTitle());
            final int x0 = this.getX() + this.width / 2 - stringWidth / 2, y0 = this.getY() + (this.height - 8) / 2;
            guiGraphics.drawString(font, this.trigger.getTitle(), x0, y0, this.trigger.disabled ? 0x9B9284 : 0xD6CCBE, false);
            if (this.isHovered) {
                GuideMapScreen.this.setTooltipForNextRenderPass(this.trigger.getDesc());
            }
        }
    }

    private static final class SideState {
        private int sidePageIndex;
        private int sideMapCount;
        private int sideWaypointCount;
        private boolean showTheWaypoint;
        private @Nullable ResourceLocation waypointId;
        private final List<ResourceLocation> waypointTextures = new ArrayList<>();

        public boolean isFirst() {
            return this.sidePageIndex <= 0;
        }

        public boolean isLast() {
            return this.sidePageIndex >= this.getSideMaxPage() - 1;
        }

        public boolean isLastWaypointAvailable() {
            return this.waypointId != null;
        }

        public boolean isWaypointTextureMoreThanOne() {
            return this.waypointTextures.size() > 1;
        }

        public void next() {
            this.sidePageIndex = Mth.clamp(this.sidePageIndex + 1, 0, this.getSideMaxPage() - 1);
        }

        public void prev() {
            this.sidePageIndex = Mth.clamp(this.sidePageIndex - 1, 0, this.getSideMaxPage() - 1);
        }

        public void rotateWaypointTexture(int offset) {
            Collections.rotate(this.waypointTextures, -offset);
        }

        public void switchToMap(int mapTriggerCount) {
            this.sidePageIndex = 0;
            this.sideMapCount = mapTriggerCount;
            this.showTheWaypoint = false;
        }

        public void switchToWaypoint(int waypointTriggerCount,
                                     ResourceLocation waypointId,
                                     List<ResourceLocation> waypointTextures) {
            this.sidePageIndex = 0;
            this.sideWaypointCount = waypointTriggerCount;
            this.showTheWaypoint = true;
            this.waypointId = waypointId;
            this.waypointTextures.clear();
            this.waypointTextures.addAll(waypointTextures);
        }

        public void switchToLastWaypointIfExists() {
            this.sidePageIndex = 0;
            this.showTheWaypoint = this.waypointId != null;
        }

        public int getSideMin() {
            return this.sidePageIndex * 7;
        }

        public int getSideMax() {
            return this.sidePageIndex * 7 + 6;
        }

        public int getSideMaxPage() {
            var count = this.showTheWaypoint ? this.sideWaypointCount : this.sideMapCount;
            return count > 0 ? (count + 6) / 7 : 1;
        }

        public @Nullable ResourceLocation getWaypointId() {
            return this.showTheWaypoint ? this.waypointId : null;
        }

        public @Nullable ResourceLocation getWaypointTexture() {
            return this.showTheWaypoint && !this.waypointTextures.isEmpty() ? this.waypointTextures.get(0) : null;
        }
    }
}