package org.teacon.signmeup.mixin;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.sampler.SamplerBinding;
import net.minecraft.util.Tuple;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.teacon.signmeup.hud.InnerMiniMapPanel;

import java.util.HashMap;
import java.util.Set;
import java.util.function.IntSupplier;

import static org.lwjgl.opengl.GL45C.*;

/**
 * @author USS_Shenzhou
 */
@Mixin(value = SamplerBinding.class, remap = false)
public class IrisSampleBindingMixin {

    @Unique
    private int smuBlankTexture;

    @Unique
    private final HashMap<String, Tuple<Set<Integer>, Set<Integer>>> smuResetShadowsMap = new HashMap<>() {{
        put("bsl", new Tuple<>(Set.of(1), Set.of(5)));
        put("complementaryunbound", new Tuple<>(Set.of(3, 4, 5), Set.of(1, 3, 4, 5, 6, 7, 8)));
    }};

    @Unique
    private final HashMap<String, String> smuShaderpackNameCache = new HashMap<>();

    @Shadow
    @Final
    private IntSupplier texture;

    @Shadow
    @Final
    private int textureUnit;

    @Inject(method = "updateSampler", at = @At("RETURN"))
    private void smuRestShadow(CallbackInfo ci) {
        if (smuBlankTexture == 0) {
            smuBlankTexture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, smuBlankTexture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, new int[]{0, 0, 0, 255});
        }
        if (InnerMiniMapPanel.rendering) {
            var shaderpackName = Iris.getCurrentPackName();
            if (shaderpackName == null) {
                return;
            }
            String shaderpackKey;
            if (smuShaderpackNameCache.containsKey(shaderpackName)) {
                shaderpackKey = smuShaderpackNameCache.get(shaderpackName);
            } else {
                shaderpackKey = smuResetShadowsMap.keySet().stream().filter(key -> shaderpackName.toLowerCase().contains(key)).findFirst().orElse(null);
                smuShaderpackNameCache.put(shaderpackName, shaderpackKey);
            }
            var action = smuResetShadowsMap.get(shaderpackKey);
            if (action == null) {
                return;
            }
            Set<Integer> units;
            if (!InnerMiniMapPanel.renderingTranslucent) {
                units = action.getA();
            } else {
                units = action.getB();
            }
            if (units.contains(this.textureUnit)) {
                glBindTextureUnit(this.textureUnit, smuBlankTexture);
            }
        }
    }
}
