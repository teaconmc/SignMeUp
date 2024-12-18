package org.teacon.signmeup.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.irisshaders.iris.uniforms.custom.cached.CachedUniform;
import net.irisshaders.iris.uniforms.custom.cached.Float3VectorCachedUniform;
import net.irisshaders.iris.uniforms.custom.cached.Float4MatrixCachedUniform;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL21;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.teacon.signmeup.hud.InnerMiniMapPanel;

import java.util.HashMap;
import java.util.function.BiConsumer;

/**
 * @author USS_Shenzhou
 */
@Mixin(value = CustomUniforms.class, remap = false)
public class IrisCustomUniformsMixin {

    @Unique
    private static final Matrix4f SMU_MATRIX = new Matrix4f();

    @Unique
    private static final HashMap<String, BiConsumer<Object2IntMap<CachedUniform>, CachedUniform>> SMU_RESET_UNIFORMS = new HashMap<>() {{
        //----------BSL----------
        put("cameraPosition", (map, cachedUniform) -> {
            if (cachedUniform instanceof Float3VectorCachedUniform float3VectorCachedUniform) {
                var vec3 = (Vector3f) ((IrisVectorCachedUniformAccessor) float3VectorCachedUniform).getCached();
                GL21.glUniform3f(map.getInt(cachedUniform), vec3.x, vec3.y + 320, vec3.z);
            }
        });
        //player rot
        put("gbufferModelViewInverse", (map, cachedUniform) -> {
            if (cachedUniform instanceof Float4MatrixCachedUniform) {
                SMU_MATRIX.identity().rotateXYZ(-90, 0, 0);
                GL21.glUniformMatrix4fv(map.getInt(cachedUniform), false, SMU_MATRIX.get(new float[16]));
            }
        });
        put("gbufferModelView", (map, cachedUniform) -> {
            if (cachedUniform instanceof Float4MatrixCachedUniform) {
                SMU_MATRIX.identity().rotateXYZ(90, 0, 0);
                GL21.glUniformMatrix4fv(map.getInt(cachedUniform), false, SMU_MATRIX.get(new float[16]));
            }
        });
    }};


    @Inject(method = "push", at = @At("RETURN"))
    private void smuResetUniform(Object pass, CallbackInfo ci, @Local Object2IntMap<CachedUniform> uniforms) {
        if (InnerMiniMapPanel.rendering) {
            uniforms.keySet().stream()
                    .filter(cachedUniform -> SMU_RESET_UNIFORMS.containsKey(cachedUniform.getName()))
                    .forEach(cachedUniform -> SMU_RESET_UNIFORMS.get(cachedUniform.getName()).accept(uniforms, cachedUniform));
        }
    }
}
