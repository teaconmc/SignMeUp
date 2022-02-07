package org.teacon.signin.data;

import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import org.teacon.signin.network.PartialUpdatePacket;

import java.util.Set;

public interface PlayerTracker {

    EntitySelector getSelector();

    Set<ServerPlayer> getTracking();

    void setTracking(Set<ServerPlayer> players);

    PartialUpdatePacket getNotifyPacket(boolean remove, ResourceLocation id);
}
