package org.teacon.exhibition_portal.components;

import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ExhibitionWaypoint(
        @SerializedName("x") int x,
        @SerializedName("y") int y,
        @SerializedName("z") int z,
        @SerializedName("rx") float rx,
        @SerializedName("ry") float ry
) {
    public static final ExhibitionWaypoint EMPTY = new ExhibitionWaypoint(0, 0, 0, Float.NaN, Float.NaN);

    public static final StreamCodec<FriendlyByteBuf, ExhibitionWaypoint> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ExhibitionWaypoint::x,
            ByteBufCodecs.VAR_INT, ExhibitionWaypoint::y,
            ByteBufCodecs.VAR_INT, ExhibitionWaypoint::z,
            ByteBufCodecs.FLOAT, ExhibitionWaypoint::rx,
            ByteBufCodecs.FLOAT, ExhibitionWaypoint::ry,
            ExhibitionWaypoint::new
    );

    public static final MapCodec<ExhibitionWaypoint> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Codec.INT.fieldOf("x").forGetter(ExhibitionWaypoint::x),
                    Codec.INT.fieldOf("y").forGetter(ExhibitionWaypoint::y),
                    Codec.INT.fieldOf("z").forGetter(ExhibitionWaypoint::z),
                    Codec.FLOAT.fieldOf("rx").forGetter(ExhibitionWaypoint::rx),
                    Codec.FLOAT.fieldOf("ry").forGetter(ExhibitionWaypoint::ry)
            ).apply(i, ExhibitionWaypoint::new)
    );

    public String toUserString() {
        return this.equals(EMPTY) ? "@unset" : String.format("(%d %d %d) (%.2f %.2f)", x, y, z, rx, ry);
    }
}
