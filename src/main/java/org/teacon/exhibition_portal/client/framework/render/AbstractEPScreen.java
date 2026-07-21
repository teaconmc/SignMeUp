package org.teacon.exhibition_portal.client.framework.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teacon.exhibition_portal.client.framework.binding.LayoutFailureException;

import java.util.List;

public abstract class AbstractEPScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEPScreen.class);

    protected AbstractEPScreen(Component title) {
        super(title);
    }

    private List<String> previousExceptionMessage;

    @Override
    public final void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float microTick) {
        graphics.pose().pushMatrix();
        try {
            render(graphics);
            graphics.pose().popMatrix();
        } catch (LayoutFailureException e) {
            graphics.pose().popMatrix();

            int p = minecraft.font.lineHeight;
            graphics.text(minecraft.font, Component.translatable("exhibition_portal.layout_failure"), p, p, 0xFFFFFFFF);
            List<String> message = ExceptionUtils.getStackTrace(e).lines().toList();
            for (int i = 0; i < message.size(); i++) {
                graphics.text(minecraft.font, Component.literal(message.get(i)).withoutShadow(), p, p * (i + 2), 0xFFFFFFFF);
            }

            if (!message.equals(previousExceptionMessage)) {
                previousExceptionMessage = message;
                LOGGER.warn("Exception occurred during UI layout.", e);
            }
        }
    }

    protected abstract void render(@NonNull GuiGraphicsExtractor graphics) throws LayoutFailureException;
}
