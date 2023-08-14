package org.teacon.signin.client.gui.component;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class TooltipText extends AbstractWidget {
    protected Minecraft mc;

    public TooltipText(int x, int y, Component message, Tooltip tooltip) {
        super(x, y, Minecraft.getInstance().font.width(message), Minecraft.getInstance().font.lineHeight, message);
        setTooltip(tooltip);

        mc = Minecraft.getInstance();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.enableScissor(getX(), getY(), getX() + 67, getY() + getHeight());
        graphics.drawString(mc.font, getMessage().copy().withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                getX(), getY(), 0x404040);
        graphics.disableScissor();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // Todo: narrator support.
    }
}
