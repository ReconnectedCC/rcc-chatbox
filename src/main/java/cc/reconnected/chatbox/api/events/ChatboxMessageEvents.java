package cc.reconnected.chatbox.api.events;

import cc.reconnected.chatbox.packets.clientPackets.SayPacket;
import cc.reconnected.chatbox.packets.clientPackets.TellPacket;
import cc.reconnected.chatbox.ws.ChatboxClient;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public class ChatboxMessageEvents {

    public static final Event<Say> SAY = EventFactory.createArrayBacked(ChatboxMessageEvents.Say.class, callbacks -> (client, packet) -> {
        for(ChatboxMessageEvents.Say callback : callbacks) {
            callback.onSay(client, packet);
        }
    });

    public static final Event<Tell> TELL = EventFactory.createArrayBacked(ChatboxMessageEvents.Tell.class, callbacks -> (client, packet) -> {
        for(ChatboxMessageEvents.Tell callback : callbacks) {
            callback.onTell(client, packet);
        }
    });

    @FunctionalInterface
    public interface Say {
        void onSay(ChatboxClient client, SayPacket packet);
    }

    @FunctionalInterface
    public interface Tell {
        void onTell(ChatboxClient client, TellPacket packet);
    }
}
