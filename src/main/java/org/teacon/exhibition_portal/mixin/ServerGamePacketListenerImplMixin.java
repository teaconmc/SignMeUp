package org.teacon.exhibition_portal.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.teacon.exhibition_portal.client.interaction.StampingCounterBlock;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @ModifyExpressionValue(
            method = "handlePickItemFromBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ServerboundPickItemFromBlockPacket;includeData()Z"
            )
    )
    private boolean includeData(boolean original, @Local(ordinal = 0) BlockState blockState) {
        return original || blockState.getBlock() instanceof StampingCounterBlock;
    }
}
