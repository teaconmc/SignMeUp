package org.teacon.exhibition_portal.components;

import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public record ExhibitionMetadata(
        @SerializedName("id") UUID uuid,
        @SerializedName("name") String name,
        @SerializedName("description") String description,
        @SerializedName("introduction") String introduction,
        @SerializedName("evt_max") byte evtMax,
        @SerializedName("evt_min") byte evtMin,
        @SerializedName("waypoint") ExhibitionWaypoint waypoint
) {
    public static final StreamCodec<FriendlyByteBuf, ExhibitionMetadata> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ExhibitionMetadata::uuid,
            ByteBufCodecs.STRING_UTF8, ExhibitionMetadata::name,
            ByteBufCodecs.STRING_UTF8, ExhibitionMetadata::description,
            ByteBufCodecs.STRING_UTF8, ExhibitionMetadata::introduction,
            ByteBufCodecs.BYTE, ExhibitionMetadata::evtMax,
            ByteBufCodecs.BYTE, ExhibitionMetadata::evtMin,
            ExhibitionWaypoint.STREAM_CODEC, ExhibitionMetadata::waypoint,
            ExhibitionMetadata::new
    );

    public static final MapCodec<ExhibitionMetadata> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    UUIDUtil.CODEC.fieldOf("id").forGetter(ExhibitionMetadata::uuid),
                    Codec.STRING.fieldOf("name").forGetter(ExhibitionMetadata::name),
                    Codec.STRING.fieldOf("description").forGetter(ExhibitionMetadata::description),
                    Codec.STRING.fieldOf("introduction").forGetter(ExhibitionMetadata::introduction),
                    Codec.BYTE.fieldOf("evt_max").forGetter(ExhibitionMetadata::evtMax),
                    Codec.BYTE.fieldOf("evt_min").forGetter(ExhibitionMetadata::evtMin),
                    ExhibitionWaypoint.CODEC.fieldOf("waypoint").forGetter(ExhibitionMetadata::waypoint)
            ).apply(i, ExhibitionMetadata::new)
    );

    public static @NonNull ExhibitionMetadata ofDefault(ExhibitionDeclaration exhibition) {
        return new ExhibitionMetadata(
                exhibition.uuid(), "@unset", String.join(", ", exhibition.mods()), String.join(", ", exhibition.mods()),
                (byte) -1, (byte) -1,
                ExhibitionWaypoint.EMPTY
        );
    }

    public ExhibitionMetadata withUuid(UUID uuid) {
        return new ExhibitionMetadata(uuid, name, description, introduction, evtMax, evtMin, waypoint);
    }

    public ExhibitionMetadata withName(String name) {
        return new ExhibitionMetadata(uuid, name, description, introduction, evtMax, evtMin, waypoint);
    }

    public ExhibitionMetadata withDescription(String description) {
        return new ExhibitionMetadata(uuid, name, description, introduction, evtMax, evtMin, waypoint);
    }

    public ExhibitionMetadata withIntroduction(String introduction) {
        return new ExhibitionMetadata(uuid, name, description, introduction, evtMax, evtMin, waypoint);
    }

    public ExhibitionMetadata withEVT(byte min, byte max) {
        return new ExhibitionMetadata(uuid, name, description, introduction, max, min, waypoint);
    }

    public ExhibitionMetadata withWaypoint(ExhibitionWaypoint waypoint) {
        return new ExhibitionMetadata(uuid, name, description, introduction, evtMax, evtMin, waypoint);
    }
}
