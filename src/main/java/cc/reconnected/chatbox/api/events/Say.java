package cc.reconnected.chatbox.api.events;


import cc.reconnected.chatbox.license.License;
import cc.reconnected.chatbox.packets.clientPackets.SayPacket;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface Say {
    Event<Say> EVENT = EventFactory.createArrayBacked(Say.class, (listeners) -> (license, packet) -> {
        for (Say listener : listeners) {
            listener.say(license, packet);
        }
    });

    void say(License license, SayPacket packet);
}
