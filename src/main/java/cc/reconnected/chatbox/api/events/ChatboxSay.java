package cc.reconnected.chatbox.api.events;


import cc.reconnected.chatbox.packets.clientPackets.SayPacket;
import cc.reconnected.chatbox.ws.ChatboxClient;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface ChatboxSay {
    Event<ChatboxSay> EVENT = EventFactory.createArrayBacked(ChatboxSay.class, (listeners) -> (client, packet) -> {
        for (ChatboxSay listener : listeners) {
            listener.say(client, packet);
        }
    });

    void say(ChatboxClient client, SayPacket packet);
}
