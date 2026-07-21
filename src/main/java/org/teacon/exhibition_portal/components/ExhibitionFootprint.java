package org.teacon.exhibition_portal.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jspecify.annotations.NonNull;
import org.teacon.exhibition_portal.ExhibitionPortal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@EventBusSubscriber
public record ExhibitionFootprint(UUID uuid, String mark, List<ExhibitionStamp> stamps) {
    public static final MapCodec<ExhibitionFootprint> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    UUIDUtil.CODEC.fieldOf("uuid").forGetter(ExhibitionFootprint::uuid),
                    Codec.string(0, 16).fieldOf("mark").forGetter(ExhibitionFootprint::mark),
                    ExhibitionStamp.CODEC.codec().listOf(0, 16).fieldOf("stamps").forGetter(ExhibitionFootprint::stamps)
            ).apply(i, ExhibitionFootprint::new)
    );

    public static final StreamCodec<FriendlyByteBuf, ExhibitionFootprint> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ExhibitionFootprint::uuid,
            ByteBufCodecs.stringUtf8(16), ExhibitionFootprint::mark,
            ExhibitionStamp.STREAM_CODEC.apply(ByteBufCodecs.list(16)), ExhibitionFootprint::stamps,
            ExhibitionFootprint::new
    );

    public static final String MARK_DEFAULT = "#default", MARK_VISITED = "#visited";

    public boolean isDefault() {
        return MARK_DEFAULT.equals(mark);
    }

    public ExhibitionFootprint withMark(String mark) {
        return new ExhibitionFootprint(uuid, mark, stamps);
    }

    public ExhibitionFootprint withStamp(ExhibitionStamp stamp) {
        for (int i = 0; i < stamps.size(); i++) {
            if (stamps.get(i).equals(stamp)) {
                return this;
            }

            if (stamps.get(i).id().equals(stamp.id())) {
                List<ExhibitionStamp> stamps = new ArrayList<>(this.stamps.size());
                stamps.addAll(stamps.subList(0, i));
                stamps.add(stamp);
                stamps.addAll(stamps.subList(i + 1, stamps.size()));
                return new ExhibitionFootprint(uuid, mark, stamps);
            }
        }

        List<ExhibitionStamp> stamps = new ArrayList<>(this.stamps.size() + 1);
        stamps.addAll(this.stamps);
        stamps.add(stamp);
        return new ExhibitionFootprint(uuid, mark, stamps);
    }

    public ExhibitionFootprint withoutStamp(String stampID) {
        for (int i = 0; i < stamps.size(); i++) {
            if (stamps.get(i).id().equals(stampID)) {
                List<ExhibitionStamp> stamps = new ArrayList<>(this.stamps.size() - 1);
                stamps.addAll(stamps.subList(0, i));
                stamps.addAll(stamps.subList(i + 1, stamps.size()));
                return new ExhibitionFootprint(uuid, mark, stamps);
            }
        }
        return this;
    }

    private static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ExhibitionPortal.MODID);

    private static final Supplier<AttachmentType<Storage>> TYPE = REGISTER.register(
            "user_footprint",
            () -> AttachmentType.builder(() -> new Storage(new ConcurrentHashMap<>()))
                    .serialize(new IAttachmentSerializer<>() {
                        private static final String KEY = "footprint";
                        private static final Codec<List<ExhibitionFootprint>> DELEGATE = CODEC.codec().listOf();

                        @Override
                        public Storage read(@NonNull IAttachmentHolder holder, @NonNull ValueInput input) {
                            List<ExhibitionFootprint> list = input.read(KEY, DELEGATE).orElseThrow();
                            return new Storage(ExhibitionPortal.collectByID(ConcurrentHashMap::new, list, ExhibitionFootprint::uuid));
                        }

                        @Override
                        public boolean write(Storage attachment, @NonNull ValueOutput output) {
                            output.store(KEY, DELEGATE, new ArrayList<>(attachment.map.values()));
                            return true;
                        }
                    })
                    .copyOnDeath()
                    .build()
    );

    @SubscribeEvent
    private static void on(FMLConstructModEvent event) {
        REGISTER.register(Objects.requireNonNull(event.getContainer().getEventBus()));
    }

    /* package-private */ static Storage getStorage(ServerPlayer player) {
        return player.getData(TYPE);
    }

    /* package-private */ static final class Storage {
        private final Map<UUID, ExhibitionFootprint> map;

        private Storage(Map<UUID, ExhibitionFootprint> map) {
            this.map = map;
        }

        public ExhibitionFootprint get(UUID uuid) {
            return map.getOrDefault(uuid, new ExhibitionFootprint(uuid, MARK_DEFAULT, List.of()));
        }

        public ExhibitionFootprint update(UUID uuid, UnaryOperator<ExhibitionFootprint> updater) {
            ExhibitionFootprint footprint = get(uuid);
            footprint = updater.apply(footprint);
            if (footprint.isDefault()) {
                map.remove(uuid);
            } else {
                map.put(uuid, footprint);
            }
            return footprint;
        }
    }
}
