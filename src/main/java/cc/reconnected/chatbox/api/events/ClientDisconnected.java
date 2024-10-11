package cc.reconnected.chatbox.api.events;

import cc.reconnected.chatbox.license.License;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.java_websocket.WebSocket;

public interface ClientDisconnected {
    Event<ClientDisconnected> EVENT = EventFactory.createArrayBacked(ClientDisconnected.class, (listeners) -> (conn, license,code, reason, remote) -> {
        for (ClientDisconnected listener : listeners) {
            listener.disconnect(conn, license, code, reason, remote);
        }
    });

    void disconnect(WebSocket conn, License license, int code, String reason, boolean remote);
}
