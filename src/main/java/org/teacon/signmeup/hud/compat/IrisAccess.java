package org.teacon.signmeup.hud.compat;

import it.unimi.dsi.fastutil.Pair;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import org.joml.Math;
import org.joml.Matrix4fc;
import org.teacon.signmeup.SignMeUp;

import java.util.Map;
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

        if (unit == null || unit.name.equals(name)) {
            return unit = buildUnit(name);
        }

        return unit;
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
}
