package cc.reconnected.chatbox.ws;

import cc.reconnected.chatbox.license.License;
import org.java_websocket.WebSocket;

import java.net.InetAddress;


public class ChatboxClient {
    public final WebSocket webSocket;
    public final License license;
    public final InetAddress address;

    public ChatboxClient(License license, WebSocket webSocket, InetAddress address) {
        this.license = license;
        this.webSocket = webSocket;
        this.address = address;
    }
}
