package org.teacon.signmeup.mixin;


import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.teacon.signmeup.hud.InnerMiniMapPanel;

/**
 * @author USS_Shenzhou
 */
@Mixin(value = GlFramebuffer.class, remap = false)
public class IrisGlFramebufferMixin {

    @Inject(method = "bind", at = @At("HEAD"), cancellable = true)
    private void cancelBindWhenRenderingMiniMap(CallbackInfo ci) {
        if (InnerMiniMapPanel.rendering) {
            ci.cancel();
        }
    }
}
