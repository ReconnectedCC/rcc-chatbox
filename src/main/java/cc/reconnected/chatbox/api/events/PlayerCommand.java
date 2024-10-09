package cc.reconnected.chatbox.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

public interface PlayerCommand {
    Event<PlayerCommand> EVENT = EventFactory.createArrayBacked(PlayerCommand.class, (listeners) -> (player, command, args, ownerOnly) -> {
        for (PlayerCommand listener : listeners) {
            listener.command(player, command, args, ownerOnly);
        }
    });

    void command(ServerPlayerEntity player, String command, String[] args, boolean ownerOnly);
}
