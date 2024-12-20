package org.teacon.signmeup.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.irisshaders.iris.pipeline.programs.SodiumShader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.teacon.signmeup.hud.InnerMiniMapPanel;

/**
 * @author USS_Shenzhou
 */
@Mixin(value = SodiumShader.class, remap = false)
public class IrisSodiumShaderMixin {

    @Redirect(method = "resetState", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;bindWrite(Z)V"))
    private void cancelBindWhenRenderingMiniMap(RenderTarget instance, boolean setViewport) {
        if (!InnerMiniMapPanel.rendering) {
            instance.bindWrite(setViewport);
        }
    }
}
