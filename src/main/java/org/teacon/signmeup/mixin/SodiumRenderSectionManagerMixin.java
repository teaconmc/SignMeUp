package org.teacon.signmeup.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.ChunkUpdateType;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkJobCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.teacon.signmeup.hud.InnerMiniMapPanel;

/**
 * @author USS_Shenzhou
 */
@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class SodiumRenderSectionManagerMixin {

    @Shadow
    protected abstract float getSearchDistance();

    @Shadow protected abstract void submitSectionTasks(ChunkJobCollector collector, ChunkUpdateType type, boolean ignoreEffortCategory);

    @Redirect(method = "createTerrainRenderList", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;getSearchDistance()F"), require = 0)
    private float disableDistanceLimitWhenRenderingMiniMap(RenderSectionManager instance) {
        if (InnerMiniMapPanel.rendering) {
            return 1000;
        }
        return this.getSearchDistance();
    }

    @Inject(method = "submitSectionTasks(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobCollector;)V", at = @At("HEAD"), cancellable = true)
    private void takeOverSectionTasksWhenRenderingMiniMap(ChunkJobCollector importantCollector, ChunkJobCollector semiImportantCollector, ChunkJobCollector deferredCollector, CallbackInfo ci) {
        if (InnerMiniMapPanel.rendering) {
            //this.submitSectionTasks(importantCollector, ChunkUpdateType.IMPORTANT_SORT, true);
            this.submitSectionTasks(semiImportantCollector, ChunkUpdateType.IMPORTANT_REBUILD, true);
            this.submitSectionTasks(deferredCollector, ChunkUpdateType.REBUILD, false);
            this.submitSectionTasks(deferredCollector, ChunkUpdateType.INITIAL_BUILD, false);
            //this.submitSectionTasks(deferredCollector, ChunkUpdateType.SORT, true);
            ci.cancel();
        }
    }
}
