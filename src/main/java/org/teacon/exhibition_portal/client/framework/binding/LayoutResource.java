package org.teacon.exhibition_portal.client.framework.binding;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import org.teacon.exhibition_portal.ExhibitionPortal;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

@EventBusSubscriber(Dist.CLIENT)
public final class LayoutResource<T> extends LayoutBinding<T> {
    public interface ResourceReader<T> {
        T read(Identifier identifier) throws IOException;
    }

    private LayoutResource(DeclarationSource source, Supplier<List<LayoutBaseValue<?>>> upstream, LayoutFunction<T> factory) {
        super(source, factory, upstream);
    }

    private static final LayoutParameter<Object> RELOAD_FLAG = LayoutParameter.of(new Object());

    public static synchronized <T> LayoutResource<T> of(Identifier identifier, ResourceReader<T> reader) {
        return new LayoutResource<>(
                DeclarationSource.capture(), () -> List.of(RELOAD_FLAG),
                _ -> {
                    try {
                        return reader.read(identifier);
                    } catch (IOException e) {
                        throw new LayoutFailureException(e);
                    }
                }
        );
    }

    @SubscribeEvent
    private static void on(AddClientReloadListenersEvent event) {
        event.addListener(
                ExhibitionPortal.id("layout_resources"),
                (_, _, barrier, executor) -> barrier.wait(Boolean.TRUE).thenRunAsync(() -> {
                    synchronized (LayoutResource.class) {
                        RELOAD_FLAG.set(new Object());
                    }
                }, executor)
        );
    }
}
