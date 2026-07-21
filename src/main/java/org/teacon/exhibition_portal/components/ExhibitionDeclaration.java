package org.teacon.exhibition_portal.components;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.UUID;

public record ExhibitionDeclaration(
        @SerializedName("id") UUID uuid,
        @SerializedName("mods") List<String> mods,
        @SerializedName("domain") byte domain,
        @SerializedName("administrators") List<UUID> administrators
) {
    public static final StreamCodec<FriendlyByteBuf, ExhibitionDeclaration> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ExhibitionDeclaration::uuid,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ExhibitionDeclaration::mods,
            ByteBufCodecs.BYTE, ExhibitionDeclaration::domain,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), ExhibitionDeclaration::administrators,
            ExhibitionDeclaration::new
    );
}
