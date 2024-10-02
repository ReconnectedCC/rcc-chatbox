package cc.reconnected.chatbox.ws;

import cc.reconnected.chatbox.license.License;
import org.java_websocket.WebSocket;


public class ChatboxClient {
    public WebSocket webSocket;
    public License license;
    public ChatboxClient(License license, WebSocket webSocket) {
        this.license = license;
        this.webSocket = webSocket;
    }
}
