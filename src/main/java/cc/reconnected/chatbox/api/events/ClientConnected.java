package cc.reconnected.chatbox.api.events;

import cc.reconnected.chatbox.license.License;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import org.java_websocket.WebSocket;

public interface ClientConnected {
    Event<ClientConnected> EVENT = EventFactory.createArrayBacked(ClientConnected.class, (listeners) -> (conn, license, isGuest) -> {
        for (ClientConnected listener : listeners) {
            listener.connect(conn, license, isGuest);
        }
    });

    void connect(WebSocket conn, License license, boolean isGuest);
}
