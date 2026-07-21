package org.teacon.exhibition_portal.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.teacon.exhibition_portal.client.framework.render.IGuiGraphicsExtension;

@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsMixin implements IGuiGraphicsExtension {
}
