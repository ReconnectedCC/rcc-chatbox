package cc.reconnected.chatbox.events;


import cc.reconnected.chatbox.license.License;
import cc.reconnected.chatbox.models.client.SayPacket;
import cc.reconnected.chatbox.models.client.TellPacket;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface Tell {
    Event<Tell> EVENT = EventFactory.createArrayBacked(Tell.class, (listeners) -> (license, packet) -> {
        for (Tell listener : listeners) {
            listener.tell(license, packet);
        }
    });

    void tell(License license, TellPacket packet);
}
