package org.teacon.signin.client;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.teacon.signin.SignMeUp;
import org.teacon.signin.data.GuideMap;
import org.teacon.signin.data.Trigger;
import org.teacon.signin.data.Waypoint;
import org.teacon.signin.network.TriggerActivation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class GuideMapScreen extends Screen {

    private static final ResourceLocation MAP_ICONS = new ResourceLocation("minecraft", "textures/map/map_icons.png");

    public static final int SIDE = 10;
    public static final int SIDEBAR_WIDTH = 80;
    public static final int SIDEBAR_ANIMATION_TIME = 500;
    private long sidebarAnimationStartTime;
    private boolean sidebarActivatedLastFrame = false;
    protected final List<Widget> mapTriggerButtons = Lists.newArrayList();

    private final GuideMap map;
    private final List<ResourceLocation> waypointIds;
    private final List<Consumer<ResourceLocation>> waypointFocusListener = new ArrayList<>();

    private List<IReorderingProcessor> descText = Collections.emptyList();
    private int startingLine = 0;
    private double mapSidebarScrollAmount = 0;

    public GuideMapScreen(GuideMap map) {
        super(map.getTitle());
        this.map = map;
        this.waypointIds = map.getWaypointIds();
    }

    // Updated every frame in this#render
    private void renderAnimatedSidebar(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {

        int displacement;

        // mouse not in this frame, display nothing
        if (mouseX >= SIDE) {
            if (this.sidebarActivatedLastFrame) {
                // if mouse is within the new range
                if (mouseX < SIDEBAR_WIDTH) {
                    // just make displacement equals zero this time
                    for (Widget btn : this.mapTriggerButtons) {
                        btn.x = 0;
                    }
                    // we don't reset start time in this case
                } else {
                    // immediately let the list invisible
                    // todo: do a smooth slide back
                    for (Widget btn : this.mapTriggerButtons) {
                        btn.x = -80;
                    }
                    // in last frame, moved out this frame
                    // reset start time
                    this.sidebarAnimationStartTime = 0;
                    // display nothing in this case
                    // mouse is not in zone this frame, so it is "not in zone in last frame" for next frame
                    this.sidebarActivatedLastFrame = false;
                }
            } else {
                // never in, just display nothing
                // reset start time
                this.sidebarAnimationStartTime = 0;
                // display nothing in this case
                // mouse is not in zone this frame, so it is "not in zone in last frame" for next frame
                this.sidebarActivatedLastFrame = false;
            }
        }
        // mouse in this frame, display the sidebar
        else {
            if (this.sidebarActivatedLastFrame) {
                // in last frame, still in this frame. We don't reset startTime in this case.
                float time = MathHelper.clamp(System.currentTimeMillis() - this.sidebarAnimationStartTime, 0, SIDEBAR_ANIMATION_TIME);
                displacement = (int) (time / SIDEBAR_ANIMATION_TIME * SIDEBAR_WIDTH);
            } else {
                // not in last frame, moved in this frame. We set the startTime in this case.
                this.sidebarAnimationStartTime = System.currentTimeMillis();
                // first frame in, so there is no replacement, only display.
                displacement = 0;
            }
            // mouse is in zone this frame, so it is "in zone in last frame" for next frame
            this.sidebarActivatedLastFrame = true;

            // now we render the sidebar
            // reduce displacement
            displacement -= SIDEBAR_WIDTH;

            // render line icons
            for (Widget btn : this.mapTriggerButtons) {
                btn.x = displacement;
            }

        }
    }

    @Override
    protected void init() {
        super.init();
        int i = (this.width - 320) / 2;
        int j = (this.height - 180) / 2;
        int x = 170;
        int y = 30;
        // I DISLIKE THIS METHOD BECAUSE IT FAILS TO HANDLE LINE BREAKING
        // A proper line breaking algorithm should comply with UAX #14, link below:
        // http://www.unicode.org/reports/tr14/
        // However it at least get things work for now. So it is the status quo.
        
        // Set the descText to be map desc initially
        // It will change when you select a waypoint
        this.descText = this.font.trimStringToWidth(this.map.getDesc(), 140);
        
        // Setup scrolling handler for the description text
        this.addListener(new DescTextScrollingHandler(this, i + x, j + y + 100, i + x + 140, j + y + 60 + 100));
        // Setup scrolling handler for the map trigger list
        this.addListener(new LeftSidebarScrollingHandler(this, 0, 0, 80, this.height));

        y = 0;
        // Setup trigger buttons from GuideMap
        for (ResourceLocation triggerId : this.map.getTriggerIds()) {
            final Trigger trigger;
            if ((trigger = SignMeUpClient.MANAGER.findTrigger(triggerId)) != null) {
                Button btn = new Button(-80, y, 80, 20, trigger.getTitle(),
                        new TriggerHandler(triggerId),
                        new TooltipRenderer(trigger.getDesc()));
                this.addButton(btn);
                this.mapTriggerButtons.add(btn);
                y += 20;
            }
        }

        // Setup Waypoints
        y = 30;
        int mapCanvasX = i + 10, mapCanvasY = j + 40;
        for (ResourceLocation wpId : this.waypointIds) {
            y = 100;
            Waypoint wp = SignMeUpClient.MANAGER.findWaypoint(wpId);
            if (wp == null || wp.disabled) {
                continue;
            }
            // For map_icons.png, each icon is 16x16 pixels, so after we get the center coordinate,
            // we also need to shift left/up by 8 pixels to center the icon.
            final Vector3i absCenter = this.map.center;
            final Vector3i absPos = wp.getRenderLocation();
            final Vector3i relativePos = new Vector3i(absPos.getX() - absCenter.getX(), 0, absPos.getZ() - absCenter.getZ());
            int waypointX = mapCanvasX + Math.round((float)relativePos.getX() / this.map.range * 64F) - 4 + 64;
            int waypointY = mapCanvasY + Math.round((float)relativePos.getZ() / this.map.range * 64F) - 4 + 64;
            // Setup Waypoints as ImageButtons
            this.addButton(new ImageButton(waypointX, waypointY, 8, 8, 80, 0, 0,
                    MAP_ICONS, 128, 128,
                    (btn) -> this.waypointFocusListener.forEach(listener -> listener.accept(wpId)),
                    (btn, transform, mouseX, mouseY) -> {
                        this.renderTooltip(transform, Arrays.asList(
                                wp.getTitle().func_241878_f(),
                                new TranslationTextComponent("sign_me_in.waypoint.distance", String.format(Locale.ROOT, "%.1f", Math.sqrt(wp.getRenderLocation().distanceSq(this.minecraft.player.getPositionVec(), true)))).func_241878_f()
                        ), mouseX, mouseY);
                    }, wp.getTitle()));

            // Setup trigger buttons from Waypoints
            for (ResourceLocation triggerId : wp.getTriggerIds()) {
                final Trigger trigger;
                if ((trigger = SignMeUpClient.MANAGER.findTrigger(triggerId)) != null) {
                    Button btn = this.addButton(new Button(i + x, j + y, 80, 20, trigger.getTitle(),
                            new TriggerHandler(triggerId),
                            new TooltipRenderer(trigger.getDesc())));
                    this.waypointFocusListener.add(newWpId -> btn.visible = Objects.equals(wpId, newWpId));
                    btn.visible = false; // After first initialization, all buttons associated with a waypoint are invisible
                    y += 20;
                }
            }
        }
    }

    // --------- Text scrolling (left) related methods --------- //
    void scrollUpDesc() {
        if (--this.startingLine < 0) {
            this.startingLine = 0;
        }
    }

    void scrollDownDesc() {
        if (++this.startingLine >= this.descText.size()) {
            this.startingLine = this.descText.size() - 1;
        }
    }

    // --------- Sidebar scrolling (left) related methods --------- //
    // Accessor, no need to say more
    public double getMapSidebarScrollAmount() {
        return this.mapSidebarScrollAmount;
    }

    // Updated every frame in this#render
    private void updateMapTriggerButtonsY() {
        int btnOffsetY = 0;
        for (Widget btn : this.mapTriggerButtons) {
            btn.y = (int) (- this.mapSidebarScrollAmount + btnOffsetY);
            btnOffsetY += btn.getHeight();
        }
    }

    // Basically getting the scroll region's height
    int getMaxSidebarPosition() {
        return this.mapTriggerButtons.size() * this.mapTriggerButtons.get(0).getHeight(); //todo: do we need header height?
    }

    // Get the maximum amount the list can scroll. When getMaxDescPosition is smaller than this.height, then the list CANNOT be scrolled.
    int getMaxSidebarScroll() {
        return Math.max(0, this.getMaxSidebarPosition() - this.height);
    }

    // Get the height of a single sidebar button
    int getSidebarButtonHeight() {
        return this.mapTriggerButtons.get(0).getHeight();
    }

    // Clamp between 0 and max scroll amt
    void setSidebarScrollAmount(double scroll) {
        this.mapSidebarScrollAmount = MathHelper.clamp(scroll, 0.0D, (double)this.getMaxSidebarScroll());
    }
    // ------------------------------------------------------------ //

    @Override
    public void render(MatrixStack transforms, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(transforms);
        // If your IDE complains about deprecated method: ignore it, there is no proper replacement
        // available at this time moment. Please wait for Mojang finishing Blaze3D.
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        // Draw 128 * 128 pixels map starting from x = 10, y = 30 relative to the top-left
        // corner of the in-game window
        this.minecraft.textureManager.bindTexture(this.map.texture);
        int i = (this.width - 320) / 2;
        int j = (this.height - 180) / 2;
        // Who said we have to use 128 * 128 texture?
        innerBlit(transforms, i + 10, i + 10 + 128, j + 40, j + 40 + 128, this.getBlitOffset(), 256, 256, 0F, 0F, 256, 256);
        // Vertical bar
        this.vLine(transforms, i + 150, j + 5, j+ 175, -1); // -1 aka 0xFFFFFFFF, opaque pure white

        // Text drawing begin.
        // Remember, fonts is a separate texture, so if you want to do a blit on another texture,
        // bind it first by calling TextureManager.bindTexture first!

        // Title, size doubled on two dimensions (total quadruple) than normal text
        transforms.push();
        transforms.scale(2F, 2F, 2F);
        this.font.drawText(transforms, this.title, (i + 10F) / 2, (j + 10F) / 2, 0xA0A0A0);
        transforms.pop();
        // Subtitle
        this.font.drawText(transforms, this.map.getSubtitle(), i + 170F, j + 10F, 0xA0A0A0);

        int height = 30;
        for (IReorderingProcessor text : this.descText.subList(this.startingLine, Math.min(this.startingLine + 6, this.descText.size()))) {
            this.font.func_238422_b_(transforms, text, i + 170F, j + height, 0xA0A0A0);
            height += 10;
        }

        // TODO: RENDER IMAGES

        // Horizontal bar, dividing long description and triggers
//        this.hLine(transforms, i + 160, i + 320, j + 95, -1);

        // Map trigger buttons list
        this.renderAnimatedSidebar(transforms, mouseX, mouseY, partialTicks);
        this.updateMapTriggerButtonsY();

        super.render(transforms, mouseX, mouseY, partialTicks);
    }

    private final class WaypointHandler implements Button.IPressable {

        @Override
        public void onPress(Button theButton) {

        }
    }

    private final class TriggerHandler implements Button.IPressable {

        private final ResourceLocation id;

        TriggerHandler(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public void onPress(Button theButton) {
            SignMeUp.channel.sendToServer(new TriggerActivation(this.id));
            GuideMapScreen.this.closeScreen();
        }
    }

    private final class TooltipRenderer implements Button.ITooltip {

        private final ITextComponent textToRender;

        private TooltipRenderer(ITextComponent text) {
            this.textToRender = text;
        }

        @Override
        public void onTooltip(Button button, MatrixStack transforms, int mouseX, int mouseY) {
            GuideMapScreen.this.renderTooltip(transforms, this.textToRender, mouseX, mouseY);
        }
    }
}
