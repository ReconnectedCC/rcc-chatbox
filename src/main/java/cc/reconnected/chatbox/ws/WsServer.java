package cc.reconnected.chatbox.ws;

import cc.reconnected.chatbox.Chatbox;
import cc.reconnected.chatbox.events.Say;
import cc.reconnected.chatbox.license.Capability;
import cc.reconnected.chatbox.models.Error;
import cc.reconnected.chatbox.models.client.ClientPacketBase;
import cc.reconnected.chatbox.models.client.SayPacket;
import cc.reconnected.chatbox.models.client.TellPacket;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Pattern;

public class WsServer extends WebSocketServer {
    public static final Pattern PATH_LICENSE = Pattern.compile("^/([0-9a-f-]+)$", Pattern.CASE_INSENSITIVE);
    private HashMap<WebSocket, ChatboxClient> clients = new HashMap<>();

    public WsServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        var path = conn.getResourceDescriptor();
        var matcher = PATH_LICENSE.matcher(path);
        if (!matcher.find()) {
            conn.close(CloseCodes.INVALID_LICENSE_KEY.code, CloseCodes.INVALID_LICENSE_KEY.getErrorString());
            return;
        }
        var licenseString = matcher.group(1);
        if (licenseString == null) {
            conn.close(CloseCodes.INVALID_LICENSE_KEY.code, CloseCodes.INVALID_LICENSE_KEY.getErrorString());
            return;
        }

        var licenseUuid = UUID.fromString(licenseString);

        var license = Chatbox.LicenseManager.getLicense(licenseUuid);
        if (license == null) {
            conn.close(CloseCodes.UNKNOWN_LICENSE_KEY.code, CloseCodes.UNKNOWN_LICENSE_KEY.getErrorString());
            return;
        }

        clients.put(conn, new ChatboxClient(license, conn));

        Chatbox.LOGGER.info("New connection with license {} ({})", license.uuid(), license.userId());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Chatbox.LOGGER.info(message);
        ClientPacketBase packet;
        try {
            packet = Chatbox.GSON.fromJson(message, ClientPacketBase.class);
        } catch (Exception e) {
            var err = ClientErrors.UNKNOWN_ERROR;
            conn.send(Chatbox.GSON.toJson(new Error(err.getErrorMessage(), err.message, -1)));
            return;
        }

        var client = clients.get(conn);
        var id = packet.id != null ? packet.id : -1;

        switch (packet.type) {
            case "say":
                var sayPacket = Chatbox.GSON.fromJson(message, SayPacket.class);
                if (!client.license.capabilities().contains(Capability.SAY)) {
                    var err = ClientErrors.MISSING_CAPABILITY;
                    conn.send(Chatbox.GSON.toJson(new Error(err.getErrorMessage(), err.message, id)));
                    return;
                }

                if (!("markdown".equals(sayPacket.mode) || "raw".equals(sayPacket.mode)))
                    sayPacket.mode = "markdown";

                Say.EVENT.invoker().say(client.license, sayPacket);

                break;
            case "tell":
                var tellPacket = Chatbox.GSON.fromJson(message, TellPacket.class);
                if (!client.license.capabilities().contains(Capability.TELL)) {
                    var err = ClientErrors.MISSING_CAPABILITY;
                    conn.send(Chatbox.GSON.toJson(new Error(err.getErrorMessage(), err.message, id)));
                    return;
                }

                break;
            default:
                var err = ClientErrors.UNKNOWN_TYPE;
                conn.send(Chatbox.GSON.toJson(new Error(err.getErrorMessage(), err.message, id)));
                break;
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            Chatbox.LOGGER.error("WebSocket client failure " + conn.getRemoteSocketAddress().getHostString(), ex.getMessage());
        } else {
            Chatbox.LOGGER.error("WebSocket failure", ex);
        }
    }

    @Override
    public void onStart() {
        Chatbox.LOGGER.info("WebSocket server listening on port {}", getPort());
    }

}
