package org.teacon.signmeup.mixin;

import net.irisshaders.iris.gl.sampler.SamplerBinding;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.teacon.signmeup.hud.InnerMiniMapPanel;
import org.teacon.signmeup.hud.compat.IrisAccess;

import static org.lwjgl.opengl.GL45C.*;

/**
 * @author USS_Shenzhou
 */
@Mixin(value = SamplerBinding.class, remap = false)
public class IrisSampleBindingMixin {
    @Unique
    private int smu$blankTexture;

    @Shadow
    @Final
    private int textureUnit;

    @Inject(method = "updateSampler", at = @At("RETURN"))
    private void restShadowWhenRenderingMinimap(CallbackInfo ci) {
        if (smu$blankTexture == 0) {
            smu$blankTexture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, smu$blankTexture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, new int[]{0, 0, 0, 255});
        }

        if (InnerMiniMapPanel.rendering) {
            IrisAccess.ShaderPackTextureUnit unit = IrisAccess.getShaderPackTextureUnit();
            if (unit == null || unit.left() == null || unit.right() == null) {
                return;
            }

            if (IrisAccess.ShaderPackTextureUnit.isActive(
                    !InnerMiniMapPanel.renderingTranslucent ? unit.left() : unit.right(), textureUnit
            )) {
                glBindTextureUnit(this.textureUnit, smu$blankTexture);
            }
        }
    }
}
