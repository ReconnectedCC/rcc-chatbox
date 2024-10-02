package cc.reconnected.chatbox.events;


import cc.reconnected.chatbox.license.License;
import cc.reconnected.chatbox.models.client.SayPacket;
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
