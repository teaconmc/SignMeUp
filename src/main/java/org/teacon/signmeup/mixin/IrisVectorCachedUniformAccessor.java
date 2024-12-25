package org.teacon.signmeup.mixin;

import net.irisshaders.iris.uniforms.custom.cached.VectorCachedUniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author USS_Shenzhou
 */
@Mixin(value = VectorCachedUniform.class, remap = false)
public interface IrisVectorCachedUniformAccessor {
    @Accessor
    Object getCached();
}
