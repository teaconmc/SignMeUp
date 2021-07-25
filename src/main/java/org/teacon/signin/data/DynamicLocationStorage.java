package org.teacon.signin.data;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
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

    @CapabilityInject(DynamicLocationStorage.class)
    public static Capability<DynamicLocationStorage> CAP;

    final Map<ResourceLocation, GlobalPos> pos = new HashMap<>();
    final Map<ResourceLocation, GlobalPos> renderPos = new HashMap<>();

    public GlobalPos getActual(ResourceLocation id) {
        return pos.get(id);
    }

    public GlobalPos getRendering(ResourceLocation id) {
        return renderPos.get(id);
    }

    public void setActual(ResourceLocation id, World world, BlockPos pos) {
        this.pos.put(id, GlobalPos.getPosition(world.getDimensionKey(), pos));
    }

    public void setRendering(ResourceLocation id, World world, BlockPos pos) {
        this.renderPos.put(id, GlobalPos.getPosition(world.getDimensionKey(), pos));
    }

    public static final class Serializer implements Capability.IStorage<DynamicLocationStorage> {

        @Override
        public INBT writeNBT(Capability<DynamicLocationStorage> capability, DynamicLocationStorage instance, Direction side) {
            return writeNBTImpl(instance);
        }

        @Override
        public void readNBT(Capability<DynamicLocationStorage> capability, DynamicLocationStorage instance, Direction side, INBT nbt) {
            readNBTImpl(instance, nbt);
        }

        public static CompoundNBT writeNBTImpl(DynamicLocationStorage instance) {
            final CompoundNBT data = new CompoundNBT();
            final CompoundNBT actualPoses = new CompoundNBT();
            final CompoundNBT renderPoses = new CompoundNBT();
            instance.pos.forEach((id, pos) -> collect(id, pos, actualPoses));
            instance.renderPos.forEach((id, pos) -> collect(id, pos, renderPoses));
            data.put("actual", actualPoses);
            data.put("render", renderPoses);
            return data;
        }

        public static void readNBTImpl(DynamicLocationStorage instance, INBT nbt) {
            if (nbt instanceof CompoundNBT) {
                final CompoundNBT data = (CompoundNBT) nbt;
                final CompoundNBT actualPoses = data.getCompound("actual");
                final CompoundNBT renderPoses = data.getCompound("render");
                actualPoses.keySet().forEach(id -> parse(actualPoses, id, instance.pos));
                renderPoses.keySet().forEach(id -> parse(renderPoses, id, instance.renderPos));
            }
        }

        private static void collect(ResourceLocation id, GlobalPos pos, CompoundNBT dst) {
            dst.put(id.toString(), GlobalPos.CODEC.encodeStart(NBTDynamicOps.INSTANCE, pos).getOrThrow(false, LOGGER::warn));
        }

        private static void parse(CompoundNBT src, String id, Map<ResourceLocation, GlobalPos> dst) {
            dst.put(new ResourceLocation(id), GlobalPos.CODEC.parse(NBTDynamicOps.INSTANCE, src.get(id)).getOrThrow(false, LOGGER::warn));
        }
    }

    public static final class Holder implements ICapabilityProvider, INBTSerializable<CompoundNBT> {

        private final DynamicLocationStorage storage = new DynamicLocationStorage();
        private final LazyOptional<DynamicLocationStorage> wrapped = LazyOptional.of(() -> this.storage);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return cap == CAP ? this.wrapped.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundNBT serializeNBT() {
            return Serializer.writeNBTImpl(this.storage);
        }

        @Override
        public void deserializeNBT(CompoundNBT nbt) {
            Serializer.readNBTImpl(this.storage, nbt);
        }
    }

}
