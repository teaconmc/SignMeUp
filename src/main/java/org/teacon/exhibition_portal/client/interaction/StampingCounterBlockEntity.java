package org.teacon.exhibition_portal.client.interaction;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.teacon.exhibition_portal.ExhibitionPortal;

import java.util.UUID;

public class StampingCounterBlockEntity extends BlockEntity {
    private UUID exhibition;
    private String stampID;
    private boolean isRectangleStamp;

    public StampingCounterBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(ExhibitionPortal.STAMPING_COUNTER_BE.get(), worldPosition, blockState);
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        this.exhibition = input.read("exhibition", UUIDUtil.CODEC).orElse(null);
        this.stampID = input.getString("stamp_id").orElse(null);
        this.isRectangleStamp = input.getBooleanOr("rectangle", false);
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        saveStamp(output, this.exhibition, this.stampID, this.isRectangleStamp);
    }

    public static void saveStamp(@NonNull ValueOutput output, UUID exhibition, String stampID, boolean isRectangleStamp) {
        output.storeNullable("exhibition", UUIDUtil.CODEC, exhibition);
        output.storeNullable("stamp_id", Codec.STRING, stampID);
        output.putBoolean("rectangle", isRectangleStamp);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public UUID getExhibition() {
        return exhibition;
    }

    public String getStampID() {
        return stampID;
    }

    public boolean isRectangleStamp() {
        return isRectangleStamp;
    }
}