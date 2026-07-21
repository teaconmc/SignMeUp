package org.teacon.exhibition_portal.components;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.teacon.exhibition_portal.ExhibitionPortal;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/* package-private */ final class ExhibitionMetadataStorage extends SavedData {
    private final Map<UUID, ExhibitionMetadata> galleries;

    public ExhibitionMetadataStorage(Map<UUID, ExhibitionMetadata> galleries) {
        this.galleries = galleries;
    }

    private static final SavedDataType<ExhibitionMetadataStorage> TYPE = new SavedDataType<>(
            ExhibitionPortal.id("galleries"),
            () -> new ExhibitionMetadataStorage(new ConcurrentHashMap<>()),
            ExhibitionMetadata.CODEC.codec().listOf().xmap(
                    list -> new ExhibitionMetadataStorage(
                            ExhibitionPortal.collectByID(ConcurrentHashMap::new, list, ExhibitionMetadata::uuid)
                    ),
                    d -> new ArrayList<>(d.get().values())
            )
    );

    public static ExhibitionMetadataStorage getInstance(MinecraftServer server) {
        return Objects.requireNonNull(server.getLevel(Level.OVERWORLD)).getDataStorage().computeIfAbsent(TYPE);
    }

    public Map<UUID, ExhibitionMetadata> get() {
        return galleries;
    }
}
