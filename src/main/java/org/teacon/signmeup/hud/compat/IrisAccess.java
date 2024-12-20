package org.teacon.signmeup.hud.compat;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.custom.cached.CachedUniform;
import net.irisshaders.iris.uniforms.custom.cached.Float3VectorCachedUniform;
import net.irisshaders.iris.uniforms.custom.cached.Float4MatrixCachedUniform;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL21;
import org.teacon.signmeup.SignMeUp;
import org.teacon.signmeup.mixin.IrisVectorCachedUniformAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class IrisAccess {
    static {
        if (!SignMeUp.IRIS_INSTALLED) {
            throw new AssertionError("Iris is not installed.");
        }
    }

    public static String getShaderPackName() {
        return Iris.getIrisConfig().getShaderPackName().orElse(null);
    }

    public static void setShaderPackName(String sp) {
        Iris.getIrisConfig().setShaderPackName(sp);
    }

    public static Matrix4fc getGBufferModelView() {
        return CapturedRenderingState.INSTANCE.getGbufferModelView();
    }

    public static void setGBufferModelView(Matrix4fc matrix4fc) {
        CapturedRenderingState.INSTANCE.setGbufferModelView(matrix4fc);
    }


    private static final Map<String, Pair<boolean[], boolean[]>> RESET_SHADERS_MAP = Map.of(
            "bsl", Pair.of(new int[]{1}, new int[]{5}),
            "complementaryunbound", Pair.of(new int[]{3, 4, 5}, new int[]{1, 3, 4, 5, 6, 7, 8})
    ).entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> Pair.of(ShaderPackTextureUnit.of(e.getValue().first()), ShaderPackTextureUnit.of(e.getValue().second()))
    ));

    private static ShaderPackTextureUnit unit = null;

    public static ShaderPackTextureUnit getShaderPackTextureUnit() {
        String name = Iris.getCurrentPackName();
        if (name == null) {
            return null;
        }

        if (unit == null || !unit.name.equals(name)) {
            return unit = buildUnit(name);
        } else {
            return unit;
        }
    }

    private static ShaderPackTextureUnit buildUnit(String name) {
        Pair<boolean[], boolean[]> config = RESET_SHADERS_MAP.get(name);
        if (config == null) {
            String ns = name.toLowerCase();
            for (Map.Entry<String, Pair<boolean[], boolean[]>> e : RESET_SHADERS_MAP.entrySet()) {
                if (ns.contains(e.getKey())) {
                    config = e.getValue();
                    break;
                }
            }
            if (config == null) {
                return new ShaderPackTextureUnit(name, null, null);
            }
        }

        return new ShaderPackTextureUnit(name, config.first(), config.second());
    }

    public record ShaderPackTextureUnit(String name, boolean[] left, boolean[] right) {
        private static boolean[] of(int[] set) {
            if (set.length == 0) {
                return new boolean[0];
            }

            int max = -1;
            for (int i : set) {
                max = Math.max(max, i);
            }
            boolean[] r = new boolean[max + 1];
            for (int i : set) {
                r[i] = true;
            }
            return r;
        }

        public static boolean isActive(boolean[] value, int index) {
            return index < value.length && value[index];
        }
    }

    public static final Map<String, BiConsumer<Object2IntMap<CachedUniform>, CachedUniform>> SMU_RESET_UNIFORMS = new HashMap<>();

    static {
        Matrix4f matrix4f = new Matrix4f();

        // ----------BSL----------
        SMU_RESET_UNIFORMS.put("cameraPosition", (map, cachedUniform) -> {
            if (cachedUniform instanceof Float3VectorCachedUniform float3VectorCachedUniform) {
                var vec3 = (Vector3f) ((IrisVectorCachedUniformAccessor) float3VectorCachedUniform).getCached();
                GL21.glUniform3f(map.getInt(cachedUniform), vec3.x, vec3.y + 320, vec3.z);
            }
        });
        // player rot
        SMU_RESET_UNIFORMS.put("gbufferModelViewInverse", (map, cachedUniform) -> {
            if (cachedUniform instanceof Float4MatrixCachedUniform) {
                matrix4f.identity().rotateXYZ(-90, 0, 0);
                GL21.glUniformMatrix4fv(map.getInt(cachedUniform), false, matrix4f.get(new float[16]));
            }
        });
        SMU_RESET_UNIFORMS.put("gbufferModelView", (map, cachedUniform) -> {
            if (cachedUniform instanceof Float4MatrixCachedUniform) {
                matrix4f.identity().rotateXYZ(90, 0, 0);
                GL21.glUniformMatrix4fv(map.getInt(cachedUniform), false, matrix4f.get(new float[16]));
            }
        });
    }
}
