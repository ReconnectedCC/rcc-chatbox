package cc.reconnected.chatbox.api.events;


import cc.reconnected.chatbox.packets.clientPackets.TellPacket;
import cc.reconnected.chatbox.ws.ChatboxClient;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface ChatboxTell {
    Event<ChatboxTell> EVENT = EventFactory.createArrayBacked(ChatboxTell.class, (listeners) -> (client, packet) -> {
        for (ChatboxTell listener : listeners) {
            listener.tell(client, packet);
        }
    });

    void tell(ChatboxClient client, TellPacket packet);
}
