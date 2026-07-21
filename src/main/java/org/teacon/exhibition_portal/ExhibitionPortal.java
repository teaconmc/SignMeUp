package org.teacon.exhibition_portal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.util.UndashedUuid;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.teacon.exhibition_portal.client.interaction.StampingCounterBlock;
import org.teacon.exhibition_portal.client.interaction.StampingCounterBlockEntity;
import org.teacon.exhibition_portal.utils.EnumStringArgument;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

@Mod(ExhibitionPortal.MODID)
@SuppressWarnings("NotNullFieldNotInitialized")
public final class ExhibitionPortal {
    public static final String MODID = "exhibition_portal";
    public static FMLModContainer MOD_CONTAINER;
    public static String VERSION;

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, MODID);

    public static final DeferredBlock<StampingCounterBlock> STAMPING_COUNTER = BLOCKS.register(
            "stamping_counter", registryName -> new StampingCounterBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, registryName))
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .sound(SoundType.WOOD)
                    .lightLevel(_ -> 15)
            )
    );
    public static final DeferredItem<BlockItem> STAMPING_COUNTER_ITEM = ITEMS.registerSimpleBlockItem(
            "stamping_counter", STAMPING_COUNTER, props -> props
    );
    public static final Supplier<BlockEntityType<StampingCounterBlockEntity>> STAMPING_COUNTER_BE = BLOCK_ENTITIES.register(
            "stamping_counter",
            () -> new BlockEntityType<>(StampingCounterBlockEntity::new, true, STAMPING_COUNTER.get())
    );
    public final DeferredHolder<ArgumentTypeInfo<?, ?>, EnumStringArgument.Info> ENUM_ARGUMENT_TYPE = COMMAND_ARGUMENT_TYPES.register(
            "enum", () -> ArgumentTypeInfos.registerByClass(EnumStringArgument.class, new EnumStringArgument.Info())
    );

    public ExhibitionPortal(FMLModContainer container, IEventBus bus) {
        MOD_CONTAINER = container;
        VERSION = container.getModInfo().getVersion().toString();
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ITEMS.register(bus);
        COMMAND_ARGUMENT_TYPES.register(bus);
    }

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new TypeAdapter<UUID>() {
                @Override
                public void write(JsonWriter out, UUID value) throws IOException {
                    out.value(UndashedUuid.toString(value));
                }

                @Override
                public UUID read(JsonReader in) throws IOException {
                    String s = in.nextString();
                    if (s.contains("-")) {
                        return UUID.fromString(s);
                    } else {
                        return UndashedUuid.fromStringLenient(s);
                    }
                }
            })
            .create();

    public static <T> Map<UUID, T> collectByID(Supplier<Map<UUID, T>> mapSupplier, List<T> list, Function<T, UUID> idMapper) {
        Map<UUID, T> map = list.stream().collect(Collector.of(
                mapSupplier, (m, o) -> m.put(idMapper.apply(o), o),
                (map1, map2) -> {
                    map1.putAll(map2);
                    return map1;
                }
        ));
        if (map.size() != list.size()) {
            throw new IllegalArgumentException("Duplicate elements.");
        }
        return map;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
