package org.teacon.signmeup.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.irisshaders.iris.uniforms.custom.cached.CachedUniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.teacon.signmeup.hud.InnerMiniMapPanel;
import org.teacon.signmeup.hud.compat.IrisAccess;

import java.util.function.BiConsumer;

/**
 * @author USS_Shenzhou
 */
@Mixin(value = CustomUniforms.class, remap = false)
public class IrisCustomUniformsMixin {
    @Inject(method = "push", at = @At("RETURN"))
    private void resetUniformWhenRenderingMinimap(Object pass, CallbackInfo ci, @Local Object2IntMap<CachedUniform> uniforms) {
        if (InnerMiniMapPanel.rendering) {
            for (CachedUniform uniform : uniforms.keySet()) {
                BiConsumer<Object2IntMap<CachedUniform>, CachedUniform> consumer = IrisAccess.SMU_RESET_UNIFORMS.get(uniform.getName());
                if (consumer != null) {
                    consumer.accept(uniforms, uniform);
                }
            }
        }
    }
}
