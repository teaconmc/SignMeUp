package org.teacon.signin.data;

import net.minecraft.command.arguments.EntitySelector;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import org.teacon.signin.network.PartialUpdatePacket;

import java.util.Set;

public interface PlayerTracker {

    EntitySelector getSelector();

    Set<ServerPlayerEntity> getTracking();

    void setTracking(Set<ServerPlayerEntity> players);

    PartialUpdatePacket getNotifyPacket(boolean remove, ResourceLocation id);
}
