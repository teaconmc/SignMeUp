package org.teacon.exhibition_portal.components;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.network.UpdateExhibitionPacket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@EventBusSubscriber(modid = ExhibitionPortal.MODID)
public final class EPServer {
    private EPServer() {
    }

    private static Map<UUID, ExhibitionDeclaration> GALLERIES;
    public static final List<String> ALLOWED_STAMP_IDS = new ArrayList<>();
    private static Map<UUID, ExhibitionMetadata> GALLERY_METADATA;
    private static SavedData GALLERY_METADATA_STORAGE;

    @SubscribeEvent
    private static void on(AddServerReloadListenersEvent event) {
        event.addListener(
                ExhibitionPortal.id("load_exhibition_configuration"),
                (currentReload, taskExecutor, preparationBarrier, reloadExecutor) -> {
                    return CompletableFuture.supplyAsync(() -> {
                                try (BufferedReader reader = currentReload.resourceManager().openAsReader(ExhibitionPortal.id("galleries.json"))) {
                                    ExhibitionDeclaration[] value = ExhibitionPortal.GSON.fromJson(reader, ExhibitionDeclaration[].class);
                                    return Arrays.stream(value).collect(ImmutableMap.toImmutableMap(ExhibitionDeclaration::uuid, Function.identity()));
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            }, taskExecutor)
                            .thenComposeAsync(preparationBarrier::wait)
                            .thenAcceptAsync(exhibition -> {
                                GALLERIES = exhibition;
                            }, reloadExecutor);
                }
        );
        event.addListener(
                ExhibitionPortal.id("load_stamp_configuration"),
                (currentReload, taskExecutor, preparationBarrier, reloadExecutor) -> {
                    return CompletableFuture.supplyAsync(() -> {
                                try (BufferedReader reader = currentReload.resourceManager().openAsReader(ExhibitionPortal.id("stamps.json"))) {
                                    return ExhibitionPortal.GSON.fromJson(reader, String[].class);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            }, taskExecutor)
                            .thenComposeAsync(preparationBarrier::wait)
                            .thenAcceptAsync(stamps -> {
                                ALLOWED_STAMP_IDS.clear();
                                ALLOWED_STAMP_IDS.addAll(Arrays.asList(stamps));
                            }, reloadExecutor);
                }
        );
    }

    @SubscribeEvent
    private static void on(ServerStartedEvent event) {
        ExhibitionMetadataStorage s = ExhibitionMetadataStorage.getInstance(event.getServer());
        GALLERY_METADATA = s.get();
        GALLERY_METADATA_STORAGE = s;
    }

    @SubscribeEvent
    private static void on(ServerStoppedEvent event) {
        GALLERY_METADATA = null;
        GALLERY_METADATA_STORAGE = null;
    }

    @SubscribeEvent
    private static void on(PlayerEvent.PlayerLoggedInEvent event) {
        sendExhibitionData((ServerPlayer) event.getEntity());
    }

    public static void replaceMetadata(List<ExhibitionMetadata> metadatas) {
        GALLERY_METADATA.clear();
        for (ExhibitionMetadata metadata : metadatas) {
            GALLERY_METADATA.put(metadata.uuid(), metadata);
        }
        GALLERY_METADATA_STORAGE.setDirty();
        for (ServerPlayer player : Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer()).getPlayerList().getPlayers()) {
            EPServer.sendExhibitionData(player);
        }
    }

    @Nullable
    public static ExhibitionDeclaration getDeclarationOrNull(UUID uuid) {
        return GALLERIES.get(uuid);
    }

    @NotNull
    public static ExhibitionDeclaration getDeclaration(UUID uuid) {
        ExhibitionDeclaration declaration = GALLERIES.get(uuid);
        if (declaration == null) {
            throw new NoSuchElementException("Unknown exhibition id: " + uuid);
        }
        return declaration;
    }

    @NotNull
    public static ExhibitionMetadata getMetadata(UUID uuid) {
        ExhibitionDeclaration declaration = getDeclaration(uuid);
        return GALLERY_METADATA.getOrDefault(uuid, ExhibitionMetadata.ofDefault(declaration));
    }

    @NotNull
    public static ExhibitionMetadata updateMetadata(UUID uuid, UnaryOperator<ExhibitionMetadata> metadata) {
        ExhibitionMetadata now = metadata.apply(getMetadata(uuid));
        GALLERY_METADATA.put(uuid, now);
        GALLERY_METADATA_STORAGE.setDirty();
        for (ServerPlayer player : Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer()).getPlayerList().getPlayers()) {
            EPServer.sendExhibitionData(player);
        }

        return now;
    }

    public static List<UUID> getOwned(ServerPlayer player) {
        List<UUID> uuids = new ArrayList<>();
        for (ExhibitionDeclaration declaration : GALLERIES.values()) {
            if (declaration.administrators().contains(player.getUUID())) {
                uuids.add(declaration.uuid());
            }
        }
        return uuids;
    }

    public static ExhibitionFootprint getFootprint(ServerPlayer player, UUID uuid) {
        return ExhibitionFootprint.getStorage(player).get(uuid);
    }

    public static ExhibitionFootprint updateFootprint(ServerPlayer player, UUID uuid, UnaryOperator<ExhibitionFootprint> updater) {
        ExhibitionFootprint v = ExhibitionFootprint.getStorage(player).update(uuid, updater);
        sendExhibitionData(player);
        return v;
    }

    private static void sendExhibitionData(ServerPlayer player) {
        List<Exhibition> galleries = new ArrayList<>(GALLERIES.size());

        ExhibitionFootprint.Storage footprints = ExhibitionFootprint.getStorage(player);
        for (ExhibitionDeclaration exhibition : GALLERIES.values()) {
            UUID id = exhibition.uuid();
            galleries.add(new Exhibition(
                    id,
                    exhibition,
                    getMetadata(id),
                    footprints.get(id)
            ));
        }

        if (!galleries.isEmpty()) {
            UUID uuid = player.getGameProfile().id();
            Random random = new Random(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());

            galleries.sort(Comparator.<Exhibition>comparingInt(c -> c.declaration().domain())
                    .thenComparing(Exhibition::uuid)
            );
            for (int i = 0; i < galleries.size(); ) {
                byte domain = galleries.get(i).declaration().domain();

                int end = galleries.size();
                for (int j = i + 1; j < galleries.size(); j++) {
                    if (domain != galleries.get(j).declaration().domain()) {
                        end = j;
                        break;
                    }
                }

                Collections.shuffle(galleries.subList(i, end), random);
                i = end;
            }
        }

        player.connection.send(new UpdateExhibitionPacket(galleries));
    }
}
