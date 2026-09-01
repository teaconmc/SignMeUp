package org.teacon.exhibition_portal.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.network.registration.HandlerThread;
import org.lwjgl.glfw.GLFW;
import org.teacon.exhibition_portal.ExhibitionPortal;
import org.teacon.exhibition_portal.client.framework.binding.LayoutParameter;
import org.teacon.exhibition_portal.client.screens.map.MapScreen;
import org.teacon.exhibition_portal.components.EPOperation;
import org.teacon.exhibition_portal.components.Exhibition;
import org.teacon.exhibition_portal.network.UpdateExhibitionPacket;
import org.teacon.exhibition_portal.network.UpdateOperationPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(Dist.CLIENT)
public class EPClient {
    public static final LayoutParameter<Map<UUID, Exhibition>> GALLERY_LOOKUP = LayoutParameter.of(Map.of());
    public static final LayoutParameter<List<UUID>> GALLERIES = LayoutParameter.of(List.of());
    public static final LayoutParameter<EPOperation> OPERATION = LayoutParameter.of(EPOperation.INSTANCE);

    private static final Lazy<KeyMapping> ENTRYPOINT_KEY = Lazy.of(() -> new KeyMapping(
            "exhibition_portal.entrypoint",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            KeyMapping.Category.MISC
    ));

    @SubscribeEvent
    private static void on(RegisterKeyMappingsEvent event) {
        event.register(ENTRYPOINT_KEY.get());
    }

    @SubscribeEvent
    private static void on(ClientTickEvent.Pre event) {
        while (ENTRYPOINT_KEY.get().consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof MapScreen) {
                minecraft.setScreen(null);
            } else {
                minecraft.setScreen(new MapScreen());
            }
        }
    }

    @SubscribeEvent
    private static void on(RegisterClientPayloadHandlersEvent event) {
        event.register(UpdateExhibitionPacket.TYPE, HandlerThread.NETWORK, (payload, context) -> {
            List<UUID> galleries = payload.galleries().stream().map(Exhibition::uuid).toList();
            Map<UUID, Exhibition> lookup = ExhibitionPortal.collectByID(HashMap::new, payload.galleries(), Exhibition::uuid);

            context.enqueueWork(() -> {
                GALLERIES.set(galleries);
                GALLERY_LOOKUP.set(new HashMap<>(lookup));
            });
        });

        event.register(UpdateOperationPacket.TYPE, HandlerThread.MAIN, (payload, _) -> {
            OPERATION.set(payload.operation());
        });
    }
}
