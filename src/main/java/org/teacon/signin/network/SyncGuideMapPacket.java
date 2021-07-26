package org.teacon.signin.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import org.teacon.signin.client.SignMeUpClient;
import org.teacon.signin.data.GuideMap;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

public final class SyncGuideMapPacket {

    private static final Gson GSON = new GsonBuilder().setLenient()
            .registerTypeAdapter(GuideMap.class, new GuideMap.Serializer())
            .registerTypeHierarchyAdapter(ITextComponent.class, new ITextComponent.Serializer())
            .create();

    private final SortedMap<ResourceLocation, GuideMap> maps;

    public SyncGuideMapPacket(SortedMap<ResourceLocation, GuideMap> mapsToSend) {
        this.maps = mapsToSend;
    }

    public SyncGuideMapPacket(PacketBuffer buf) {
        this.maps = new TreeMap<>(); // We want all guide maps to be sorted by key.
        try {
            final String src = new String(buf.readByteArray(), StandardCharsets.UTF_8);
            final JsonObject json = GSON.fromJson(src, JsonObject.class);
            json.entrySet().forEach(e -> SyncGuideMapPacket.accept(e, GuideMap.class, this.maps));
        } catch (Exception ignored) {
            ignored.printStackTrace(System.err);
        }
    }

    private static <T> void accept(Map.Entry<String, JsonElement> entry, Class<T> type, Map<ResourceLocation, T> dst) {
        dst.put(new ResourceLocation(entry.getKey()), GSON.fromJson(entry.getValue(), type));
    }

    public void write(PacketBuffer buf) {
        buf.writeByteArray(GSON.toJson(this.maps).getBytes(StandardCharsets.UTF_8));
    }

    public void handle(Supplier<NetworkEvent.Context> contextGetter) {
        // acceptUpdateFromServer has been marked with synchronized keyword,
        // thus we can do this directly on the netty worker thread.
        SignMeUpClient.MANAGER.acceptUpdateFromServer(this.maps);
        contextGetter.get().setPacketHandled(true);
    }

}
