package org.teacon.signin.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class DynamicLocationStorage {

    private static final Logger LOGGER = LogManager.getLogger("SignMeUp");

    public static Capability<DynamicLocationStorage> CAP;

    final Map<ResourceLocation, GlobalPos> pos = new HashMap<>();
    final Map<ResourceLocation, GlobalPos> renderPos = new HashMap<>();

    public GlobalPos getActual(ResourceLocation id) {
        return pos.get(id);
    }

    public GlobalPos getRendering(ResourceLocation id) {
        return renderPos.get(id);
    }

    public void setActual(ResourceLocation id, Level level, BlockPos pos) {
        this.pos.put(id, GlobalPos.of(level.dimension(), pos));
    }

    public void setRendering(ResourceLocation id, Level level, BlockPos pos) {
        this.renderPos.put(id, GlobalPos.of(level.dimension(), pos));
    }

    public static final class Serializer {
        public Tag writeNBT(Capability<DynamicLocationStorage> capability, DynamicLocationStorage instance, Direction side) {
            return writeNBTImpl(instance);
        }

        public void readNBT(Capability<DynamicLocationStorage> capability, DynamicLocationStorage instance, Direction side, Tag nbt) {
            readNBTImpl(instance, nbt);
        }

        public static CompoundTag writeNBTImpl(DynamicLocationStorage instance) {
            final CompoundTag data = new CompoundTag();
            final CompoundTag actualPoses = new CompoundTag();
            final CompoundTag renderPoses = new CompoundTag();
            instance.pos.forEach((id, pos) -> collect(id, pos, actualPoses));
            instance.renderPos.forEach((id, pos) -> collect(id, pos, renderPoses));
            data.put("actual", actualPoses);
            data.put("render", renderPoses);
            return data;
        }

        public static void readNBTImpl(DynamicLocationStorage instance, Tag nbt) {
            if (nbt instanceof CompoundTag) {
                final CompoundTag data = (CompoundTag) nbt;
                final CompoundTag actualPoses = data.getCompound("actual");
                final CompoundTag renderPoses = data.getCompound("render");
                actualPoses.getAllKeys().forEach(id -> parse(actualPoses, id, instance.pos));
                renderPoses.getAllKeys().forEach(id -> parse(renderPoses, id, instance.renderPos));
            }
        }

        private static void collect(ResourceLocation id, GlobalPos pos, CompoundTag dst) {
            dst.put(id.toString(), GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).getOrThrow(false, LOGGER::warn));
        }

        private static void parse(CompoundTag src, String id, Map<ResourceLocation, GlobalPos> dst) {
            dst.put(new ResourceLocation(id), GlobalPos.CODEC.parse(NbtOps.INSTANCE, src.get(id)).getOrThrow(false, LOGGER::warn));
        }
    }

    public static final class Holder implements ICapabilityProvider, INBTSerializable<CompoundTag> {

        private final DynamicLocationStorage storage = new DynamicLocationStorage();
        private final LazyOptional<DynamicLocationStorage> wrapped = LazyOptional.of(() -> this.storage);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return cap == CAP ? this.wrapped.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return Serializer.writeNBTImpl(this.storage);
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            Serializer.readNBTImpl(this.storage, nbt);
        }
    }

}
