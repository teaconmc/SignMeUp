package org.teacon.exhibition_portal.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.teacon.exhibition_portal.client.framework.components.Rectangle;

import java.util.List;

public record ExhibitionStamp(String id, Identifier item, Rectangle location, float rotate) {
    public static final StreamCodec<FriendlyByteBuf, ExhibitionStamp> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(16), ExhibitionStamp::id,
            Identifier.STREAM_CODEC, ExhibitionStamp::item,
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, Rectangle::x,
                    ByteBufCodecs.FLOAT, Rectangle::y,
                    ByteBufCodecs.FLOAT, Rectangle::w,
                    ByteBufCodecs.FLOAT, Rectangle::h,
                    Rectangle::new
            ), ExhibitionStamp::location,
            ByteBufCodecs.FLOAT, ExhibitionStamp::rotate,
            ExhibitionStamp::new
    );

    @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
    public static final MapCodec<ExhibitionStamp> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Codec.string(0, 16).fieldOf("id").forGetter(ExhibitionStamp::id),
                    Identifier.CODEC.fieldOf("item").forGetter(ExhibitionStamp::item),
                    Codec.FLOAT.listOf(4, 4).xmap(
                            floats -> new Rectangle(floats.get(0), floats.get(1), floats.get(2), floats.get(3)),
                            rectangle -> List.of(rectangle.x(), rectangle.y(), rectangle.w(), rectangle.h())
                    ).fieldOf("location").forGetter(ExhibitionStamp::location),
                    Codec.FLOAT.fieldOf("rotate").forGetter(ExhibitionStamp::rotate)
            ).apply(i, ExhibitionStamp::new)
    );

    public ExhibitionStamp withItem(Identifier item) {
        return new ExhibitionStamp(id, item, location, rotate);
    }

    public ExhibitionStamp withLocation(Rectangle location) {
        return new ExhibitionStamp(id, item, location, rotate);
    }

    public ExhibitionStamp withRotate(float rotate) {
        return new ExhibitionStamp(id, item, location, rotate);
    }
}
