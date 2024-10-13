package cc.reconnected.chatbox.ws;

import cc.reconnected.chatbox.Chatbox;
import cc.reconnected.chatbox.api.events.*;
import cc.reconnected.chatbox.license.Capability;
import cc.reconnected.chatbox.license.License;
import cc.reconnected.chatbox.license.LicenseManager;
import cc.reconnected.chatbox.packets.serverPackets.ErrorPacket;
import cc.reconnected.chatbox.packets.clientPackets.ClientPacketBase;
import cc.reconnected.chatbox.packets.clientPackets.SayPacket;
import cc.reconnected.chatbox.packets.clientPackets.TellPacket;
import cc.reconnected.chatbox.parsers.Formats;
import joptsimple.util.InetAddressConverter;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class WsServer extends WebSocketServer {
    public static final Pattern PATH_LICENSE = Pattern.compile("^/([0-9a-z-]+)$", Pattern.CASE_INSENSITIVE);
    private final HashMap<WebSocket, ChatboxClient> clients = new HashMap<>();
    private final InetAddress guestAddress;
    public static int messageMaxLength = 1024;
    public static int nameMaxLength = 64;

    public HashMap<WebSocket, ChatboxClient> clients() {
        return clients;
    }

    public WsServer(InetSocketAddress address) {
        super(address);
        this.guestAddress = new InetAddressConverter().convert(Chatbox.CONFIG.guestAllowedAddress());
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
            license = Chatbox.LicenseManager.getLicense(licenseUuid);
        } catch (Exception e) {
            conn.close(CloseCodes.FATAL_ERROR.code, CloseCodes.FATAL_ERROR.getErrorString());
            Chatbox.LOGGER.error("Failed to load license", e);
            return;
        }

        if (license == null) {
            conn.close(CloseCodes.UNKNOWN_LICENSE_KEY.code, CloseCodes.UNKNOWN_LICENSE_KEY.getErrorString());
            return;
        }

        clients.put(conn, new ChatboxClient(license, conn, clientAddress));

        Chatbox.LOGGER.info("[{}] New connection with license {} ({})", clientAddress, license.uuid(), license.userId());

        ClientConnectionEvents.CONNECT.invoker().onConnect(conn, license, license.userId().equals(LicenseManager.guestLicenseUuid));
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if(!clients.containsKey(conn)) {
            return;
        }
        var client = clients.remove(conn);
        ClientConnectionEvents.DISCONNECT.invoker().onDisconnect(conn, client.license, code, reason, remote);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Chatbox.LOGGER.debug(message);
        ClientPacketBase packet;
        try {
            packet = Chatbox.GSON.fromJson(message, ClientPacketBase.class);
        } catch (Exception e) {
            var err = ClientErrors.UNKNOWN_ERROR;
            conn.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, -1)));
            return;
        }

        if (packet == null) {
            var err = ClientErrors.UNKNOWN_ERROR;
            conn.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, -1)));
            return;
        }

        var client = clients.get(conn);
        var id = packet.id != null ? packet.id : -1;

        if (packet.type == null) {
            packet.type = "unknown";
        }

        switch (packet.type) {
            case "say":
                var sayPacket = Chatbox.GSON.fromJson(message, SayPacket.class);
                sayPacket.id = id;
                if (!client.license.capabilities().contains(Capability.SAY)) {
                    var err = ClientErrors.MISSING_CAPABILITY;
                    conn.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id)));
                    return;
                }

                if (sayPacket.text == null) {
                    var err = ClientErrors.MISSING_TEXT;
                    conn.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id)));
                    return;
                }

                if (sayPacket.mode == null)
                    sayPacket.mode = "markdown";

                if (!Formats.available.contains(sayPacket.mode)) {
                    var err = ClientErrors.INVALID_MODE;
                    conn.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id)));
                    return;
                }

                sayPacket.text = sayPacket.text.substring(0, Math.min(sayPacket.text.length(), messageMaxLength));
                if (sayPacket.name != null) {
                    sayPacket.name = sayPacket.name.trim().replace("\n", "");
                    sayPacket.name = sayPacket.name.substring(0, Math.min(sayPacket.name.length(), nameMaxLength));
                }

                ChatboxMessageEvents.SAY.invoker().onSay(client, sayPacket);

                break;
            case "tell":
                var tellPacket = Chatbox.GSON.fromJson(message, TellPacket.class);
                tellPacket.id = id;
                if (!client.license.capabilities().contains(Capability.TELL)) {
                    var err = ClientErrors.MISSING_CAPABILITY;
                    conn.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id)));
                    return;
                }

                if (tellPacket.user == null) {
                    var err = ClientErrors.MISSING_USER;
                    conn.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id)));
                    return;
                }

                if (tellPacket.text == null) {
                    var err = ClientErrors.MISSING_TEXT;
                    conn.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id)));
                    return;
                }

                if (tellPacket.mode == null)
                    tellPacket.mode = "markdown";

                if (!Formats.available.contains(tellPacket.mode)) {
                    var err = ClientErrors.INVALID_MODE;
                    conn.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id)));
                    return;
                }

                tellPacket.text = tellPacket.text.substring(0, Math.min(tellPacket.text.length(), messageMaxLength));
                if (tellPacket.name != null) {
                    tellPacket.name = tellPacket.name.trim().replace("\n", "");
                    tellPacket.name = tellPacket.name.substring(0, Math.min(tellPacket.name.length(), nameMaxLength));
                }

                ChatboxMessageEvents.TELL.invoker().onTell(client, tellPacket);

                break;
            default:
                var err = ClientErrors.UNKNOWN_TYPE;
                conn.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id)));
                break;
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            var address = conn.getRemoteSocketAddress().getAddress();
            var client = clients.get(conn);
            if (client != null) {
                address = client.address;
            }

            Chatbox.LOGGER.error("WebSocket client failure {}", address);
            Chatbox.LOGGER.error("Exception thrown:", ex);
        } else {
            Chatbox.LOGGER.error("WebSocket failure", ex);
        }
    }

    @Override
    public void onStart() {
        Chatbox.LOGGER.info("WebSocket server listening on port {}", getPort());
    }

    public void broadcastEvent(Object packet, @Nullable Capability capability) {
        var msg = Chatbox.GSON.toJson(packet);

        List<WebSocket> recipients;
        if (capability == null) {
            recipients = clients.keySet().stream().toList();
        } else {
            recipients = clients
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue().license.capabilities().contains(capability))
                    .map(Map.Entry::getKey)
                    .toList();
        }

        for (var conn : recipients) {
            conn.send(msg);
        }
    }

    public void broadcastOwnerEvent(Object packet, @Nullable Capability capability, UUID ownerId) {
        var msg = Chatbox.GSON.toJson(packet);

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
