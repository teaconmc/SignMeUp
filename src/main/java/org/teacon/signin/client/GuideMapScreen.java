package org.teacon.signin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import org.teacon.signin.SignMeUp;
import org.teacon.signin.data.GuideMap;
import org.teacon.signin.data.Trigger;
import org.teacon.signin.data.Waypoint;
import org.teacon.signin.network.TriggerActivation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GuideMapScreen extends Screen {

    private final GuideMap map;
    private final List<Waypoint> waypoints;

    private List<IReorderingProcessor> descText = Collections.emptyList();
    private int startingLine = 0;

    public GuideMapScreen(GuideMap map) {
        super(map.getTitle());
        this.map = map;
        this.waypoints = map.getWaypointIds().stream().map(SignMeUpClient.MANAGER::findWaypoint).collect(Collectors.toList());
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
        this.descText = this.font.trimStringToWidth(this.map.getDesc(), 140);
        this.addListener(new ScrollingHandler(this, i + x, j + y, i + x + 140, j + y + 60));
        y = 100;
        for (ResourceLocation triggerId : this.map.getTriggerIds()) {
            final Trigger trigger;
            if ((trigger = SignMeUpClient.MANAGER.findTrigger(triggerId)) != null) {
                this.addButton(new Button(i + x, j + y, 80, 20, trigger.title,
                        btn -> this.handleTrigger(triggerId),
                        (btn, transform, mouseX, mouseY) -> this.renderTooltip(transform, trigger.getDesc(), mouseX, mouseY))).active = false;
            }
        }

    }

    private void handleTrigger(ResourceLocation triggerId) {
        SignMeUp.channel.sendToServer(new TriggerActivation(triggerId));
        this.closeScreen();
    }

    void scrollUp() {
        if (--this.startingLine < 0) {
            this.startingLine = 0;
        }
    }

    void scrollDown() {
        if (++this.startingLine >= this.descText.size()) {
            this.startingLine = this.descText.size() - 1;
        }
    }

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
        this.font.func_243248_b(transforms, this.title, (i + 10F) / 2, (j + 10F) / 2, 0xA0A0A0);
        transforms.pop();
        // Subtitle
        this.font.func_243248_b(transforms, this.map.getSubtitle(), i + 170F, j + 10F, 0xA0A0A0);

        int height = 30;
        for (IReorderingProcessor text : this.descText.subList(this.startingLine, Math.min(this.startingLine + 6, this.descText.size()))) {
            this.font.func_238422_b_(transforms, text, i + 170F, j + height, 0xA0A0A0);
            height += 10;
        }

        // Horizontal bar, dividing long description and triggers
        this.hLine(transforms, i + 160, i + 320, j + 95, -1);

        super.render(transforms, mouseX, mouseY, partialTicks);
    }
}
