package cc.reconnected.chatbox.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

public interface PlayerCommandEvent {
    Event<PlayerCommandEvent> EVENT = EventFactory.createArrayBacked(PlayerCommandEvent.class, (listeners) -> (player, command, args, ownerOnly) -> {
        for (PlayerCommandEvent listener : listeners) {
            listener.onCommand(player, command, args, ownerOnly);
        }
    });

    void onCommand(ServerPlayerEntity player, String command, String[] args, boolean ownerOnly);
}
