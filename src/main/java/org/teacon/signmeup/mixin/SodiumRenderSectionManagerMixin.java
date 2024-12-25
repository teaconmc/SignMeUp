package org.teacon.signmeup.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkUpdateType;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkJobCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.teacon.signmeup.hud.InnerMiniMapPanel;

/**
 * @author USS_Shenzhou
 */
@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class SodiumRenderSectionManagerMixin {
    @WrapOperation(method = "createTerrainRenderList", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;getSearchDistance()F"), require = 0)
    private float disableDistanceLimitWhenRenderingMiniMap(RenderSectionManager instance, Operation<Float> original) {
        if (InnerMiniMapPanel.rendering) {
            return 1000;
        }
        return original.call(instance);
    }

    @WrapWithCondition(
            method = "submitSectionTasks(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobCollector;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;submitSectionTasks(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/ChunkUpdateType;Z)V"
            )
    )
    private boolean takeOverSectionTasksWhenRenderingMiniMap(RenderSectionManager instance, ChunkJobCollector result, ChunkUpdateType job, boolean section) {
        if (!InnerMiniMapPanel.rendering) {
            return true;
        }
        return switch (job) {
            case IMPORTANT_SORT, SORT -> false;
            default -> true;
        };
    }
}
