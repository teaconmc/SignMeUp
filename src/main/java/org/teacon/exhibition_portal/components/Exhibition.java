package org.teacon.exhibition_portal.components;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;
import java.util.UUID;

public record Exhibition(
        UUID uuid,
        ExhibitionDeclaration declaration,
        ExhibitionMetadata metadata,
        ExhibitionFootprint footprint
) {
    public static final StreamCodec<FriendlyByteBuf, Exhibition> STREAM_CODEC = StreamCodec.composite(
            ExhibitionDeclaration.STREAM_CODEC, Exhibition::declaration,
            ExhibitionMetadata.STREAM_CODEC, Exhibition::metadata,
            ExhibitionFootprint.STREAM_CODEC, Exhibition::footprint,
            Exhibition::new
    );

    public Exhibition {
        UUID id1 = declaration.uuid(), id2 = metadata.uuid(), id3 = footprint.uuid();
        if (!Objects.equals(uuid, id1) || !Objects.equals(uuid, id2) || !Objects.equals(uuid, id3)) {
            throw new IllegalArgumentException(String.format("Mismatched exhibition instance id: %s != %s, %s, %s", uuid, id1, id2, id3));
        }
    }

    public Exhibition(ExhibitionDeclaration exhibition, ExhibitionMetadata metadata, ExhibitionFootprint footprint) {
        this(exhibition.uuid(), exhibition, metadata, footprint);
    }
}
