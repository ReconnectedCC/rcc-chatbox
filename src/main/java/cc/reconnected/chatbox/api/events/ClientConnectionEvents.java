package cc.reconnected.chatbox.api.events;

import cc.reconnected.chatbox.license.License;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.java_websocket.WebSocket;

public class ClientConnectionEvents {
    public static final Event<ClientConnectionEvents.Connection> CONNECT = EventFactory.createArrayBacked(ClientConnectionEvents.Connection.class, callbacks -> (conn, license, isGuest) -> {
        for(ClientConnectionEvents.Connection callback : callbacks) {
            callback.onConnect(conn, license, isGuest);
        }
    });

    public static final Event<ClientConnectionEvents.Disconnection> DISCONNECT = EventFactory.createArrayBacked(ClientConnectionEvents.Disconnection.class, callbacks -> (conn, license,code, reason, remote) -> {
        for(ClientConnectionEvents.Disconnection callback : callbacks) {
            callback.onDisconnect(conn, license,code, reason, remote);
        }
    });

    @FunctionalInterface
    public interface Connection {
        void onConnect(WebSocket conn, License license, boolean isGuest);
    }

    @FunctionalInterface
    public interface Disconnection {
        void onDisconnect(WebSocket conn, License license, int code, String reason, boolean remote);
    }
}
