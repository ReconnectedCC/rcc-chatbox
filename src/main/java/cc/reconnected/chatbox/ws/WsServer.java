package cc.reconnected.chatbox.ws;

import cc.reconnected.chatbox.RccChatbox;
import cc.reconnected.chatbox.api.events.ChatboxMessageEvents;
import cc.reconnected.chatbox.api.events.ClientConnectionEvents;
import cc.reconnected.chatbox.license.Capability;
import cc.reconnected.chatbox.license.License;
import cc.reconnected.chatbox.license.LicenseManager;
import cc.reconnected.chatbox.packets.clientPackets.ClientPacketBase;
import cc.reconnected.chatbox.packets.clientPackets.SayPacket;
import cc.reconnected.chatbox.packets.clientPackets.TellPacket;
import cc.reconnected.chatbox.packets.serverPackets.ErrorPacket;
import cc.reconnected.chatbox.parsers.Formats;
import joptsimple.util.InetAddressConverter;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class WsServer extends WebSocketServer {
    public static final Pattern PATH_LICENSE = Pattern.compile("^/([0-9a-z-]+)$", Pattern.CASE_INSENSITIVE);
    private final ConcurrentHashMap<WebSocket, ChatboxClient> clients = new ConcurrentHashMap<>();
    private final InetAddress guestAddress;
    public static final int messageMaxLength = 1024;
    public static final int nameMaxLength = 64;

    public ConcurrentHashMap<WebSocket, ChatboxClient> clients() {
        return clients;
    }

    public WsServer(InetSocketAddress address) {
        super(address);
        this.guestAddress = new InetAddressConverter().convert(RccChatbox.CONFIG.guestAllowedAddress);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        InetAddress clientAddress = conn.getRemoteSocketAddress().getAddress();
        if (handshake.hasFieldValue("X-Forwarded-For")) {
            clientAddress = new InetAddressConverter().convert(handshake.getFieldValue("X-Forwarded-For"));
        }

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

        UUID licenseUuid;

        if (licenseString.equals("guest")) {
            if (!clientAddress.equals(guestAddress)) {
                conn.close(CloseCodes.EXTERNAL_GUESTS_NOT_ALLOWED.code, CloseCodes.EXTERNAL_GUESTS_NOT_ALLOWED.getErrorString());
                return;
            }

            licenseUuid = LicenseManager.guestLicenseUuid;
        } else {
            try {
                licenseUuid = UUID.fromString(licenseString);
            } catch (IllegalArgumentException e) {
                conn.close(CloseCodes.INVALID_LICENSE_KEY.code, CloseCodes.INVALID_LICENSE_KEY.getErrorString());
                return;
            }
        }

        License license;
        try {
            license = RccChatbox.licenseManager().getLicense(licenseUuid);
        } catch (Exception e) {
            conn.close(CloseCodes.FATAL_ERROR.code, CloseCodes.FATAL_ERROR.getErrorString());
            RccChatbox.LOGGER.error("Failed to load license", e);
            return;
        }

        if (license == null) {
            conn.close(CloseCodes.UNKNOWN_LICENSE_KEY.code, CloseCodes.UNKNOWN_LICENSE_KEY.getErrorString());
            return;
        }

        clients.put(conn, new ChatboxClient(license, conn, clientAddress));

        RccChatbox.LOGGER.info("[{}] New connection with license {} ({})", clientAddress, license.uuid(), license.userId());

        ClientConnectionEvents.CONNECT.invoker().onConnect(conn, license, license.userId().equals(LicenseManager.guestLicenseUuid));
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (!clients.containsKey(conn)) {
            return;
        }
        var client = clients.remove(conn);
        ClientConnectionEvents.DISCONNECT.invoker().onDisconnect(conn, client.license, code, reason, remote);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        RccChatbox.LOGGER.debug(message);
        ClientPacketBase packet;
        try {
            packet = RccChatbox.GSON.fromJson(message, ClientPacketBase.class);
        } catch (Exception e) {
            var err = ClientErrors.UNKNOWN_ERROR;
            conn.send(RccChatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, -1)));
            return;
        }

        if (packet == null) {
            var err = ClientErrors.UNKNOWN_ERROR;
            conn.send(RccChatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, -1)));
            return;
        }

        var client = clients.get(conn);
        var id = packet.id != null ? packet.id : -1;

        if (packet.type == null) {
            packet.type = "unknown";
        }

        try {
            switch (packet.type) {
                case "say":
                    sendSayMessage(conn, message, id, client);

                    break;
                case "tell":
                    sendTellMessage(conn, message, id, client);

                    break;
                default:
                    var err = ClientErrors.UNKNOWN_TYPE;
                    conn.send(RccChatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id)));
                    break;
            }
        } catch (WebsocketNotConnectedException e) {
            RccChatbox.LOGGER.warn("Was unable to send message confirmation to a disconnected websocket (UUID: {})", client.license.uuid());
        }
    }

    private static void sendSayMessage(WebSocket conn, String message, int id, ChatboxClient client) {
        var sayPacket = RccChatbox.GSON.fromJson(message, SayPacket.class);
        sayPacket.id = id;
        if (!client.license.capabilities().contains(Capability.SAY)) {
            var err = ClientErrors.MISSING_CAPABILITY;
            conn.send(RccChatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id)));
            return;
        }

        if (sayPacket.text == null) {
            var err = ClientErrors.MISSING_TEXT;
            conn.send(RccChatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id)));
            return;
        }

        if (sayPacket.mode == null)
            sayPacket.mode = "markdown";

        if (!Formats.available.contains(sayPacket.mode)) {
            var err = ClientErrors.INVALID_MODE;
            conn.send(RccChatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id)));
            return;
        }

        sayPacket.text = sayPacket.text.substring(0, Math.min(sayPacket.text.length(), messageMaxLength));
        if (sayPacket.name != null) {
            sayPacket.name = sayPacket.name.trim().replace("\n", "");
            sayPacket.name = sayPacket.name.substring(0, Math.min(sayPacket.name.length(), nameMaxLength));
        }

        ChatboxMessageEvents.SAY.invoker().onSay(client, sayPacket);
    }

    private static void sendTellMessage(WebSocket conn, String message, int id, ChatboxClient client) {
        var tellPacket = RccChatbox.GSON.fromJson(message, TellPacket.class);
        tellPacket.id = id;
        if (!client.license.capabilities().contains(Capability.TELL)) {
            var err = ClientErrors.MISSING_CAPABILITY;
            conn.send(RccChatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id)));
            return;
        }

        if (tellPacket.user == null) {
            var err = ClientErrors.MISSING_USER;
            conn.send(RccChatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id)));
            return;
        }

        if (tellPacket.text == null) {
            var err = ClientErrors.MISSING_TEXT;
            conn.send(RccChatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id)));
            return;
        }

        if (tellPacket.mode == null)
            tellPacket.mode = "markdown";

        if (!Formats.available.contains(tellPacket.mode)) {
            var err = ClientErrors.INVALID_MODE;
            conn.send(RccChatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id)));
            return;
        }

        tellPacket.text = tellPacket.text.substring(0, Math.min(tellPacket.text.length(), messageMaxLength));
        if (tellPacket.name != null) {
            tellPacket.name = tellPacket.name.trim().replace("\n", "");
            tellPacket.name = tellPacket.name.substring(0, Math.min(tellPacket.name.length(), nameMaxLength));
        }

        ChatboxMessageEvents.TELL.invoker().onTell(client, tellPacket);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            var address = conn.getRemoteSocketAddress().getAddress();
            var client = clients.get(conn);
            if (client != null) {
                address = client.address;
            }

            RccChatbox.LOGGER.error("WebSocket client failure {}", address);
            RccChatbox.LOGGER.error("Exception thrown:", ex);
        } else {
            RccChatbox.LOGGER.error("WebSocket failure", ex);
        }
    }

    @Override
    public void onStart() {
        RccChatbox.LOGGER.info("WebSocket server listening on port {}", getPort());
    }

    public void broadcastEvent(Object packet, @Nullable Capability capability) {
        var msg = RccChatbox.GSON.toJson(packet);

        if (capability == null) {
            broadcast(msg);
        } else {
            var recipients = clients
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue().license.capabilities().contains(capability))
                    .map(Map.Entry::getKey)
                    .toList();

            for (var conn : recipients) {
                conn.send(msg);
            }
        }


    }

    public void broadcastOwnerEvent(Object packet, @Nullable Capability capability, UUID ownerId) {
        var msg = RccChatbox.GSON.toJson(packet);

        List<WebSocket> recipients;
        var ownerClients = clients
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().license.userId().equals(ownerId))
                .toList();

        if (capability == null) {
            recipients = ownerClients.stream().map(Map.Entry::getKey).toList();
        } else {
            recipients = ownerClients
                    .stream()
                    .filter(e -> e.getValue().license.capabilities().contains(capability))
                    .map(Map.Entry::getKey)
                    .toList();
        }

        for (var conn : recipients) {
            conn.send(msg);
        }
    }

    public void closeAllClients(CloseCodes closeCode) {
        clients.forEach((conn, client) -> conn.close(closeCode.code, closeCode.message));
    }

    public void closeLicenseClients(UUID license, CloseCodes closeCode) {
        clients.entrySet().stream().filter(entry -> entry.getValue().license.uuid().equals(license))
                .forEach(entry -> {
                    var conn = entry.getKey();
                    conn.close(closeCode.code, closeCode.message);
                });
    }
}
