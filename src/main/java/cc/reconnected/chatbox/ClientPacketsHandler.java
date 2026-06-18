package cc.reconnected.chatbox;

import cc.reconnected.chatbox.api.events.ChatboxMessageEvents;
import cc.reconnected.chatbox.license.Capability;
import cc.reconnected.chatbox.models.User;
import cc.reconnected.chatbox.packets.clientPackets.SayPacket;
import cc.reconnected.chatbox.packets.serverPackets.ErrorPacket;
import cc.reconnected.chatbox.packets.serverPackets.SuccessPacket;
import cc.reconnected.chatbox.packets.serverPackets.events.ChatboxChatEvent;
import cc.reconnected.chatbox.utils.DateUtils;
import cc.reconnected.chatbox.utils.TextComponents;
import cc.reconnected.chatbox.utils.Webhook;
import cc.reconnected.chatbox.ws.ClientErrors;
import cc.reconnected.library.data.PlayerMeta;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class ClientPacketsHandler {
    public static final int maxMessageQueueSize = 5;

    // License UUID = queue
    private static final ConcurrentHashMap<UUID, ConcurrentLinkedQueue<ClientMessage>> messageQueue = new ConcurrentHashMap<>();

    private static boolean tryEnqueue(UUID licenseId, ClientMessage message) {
        var queue = messageQueue.computeIfAbsent(licenseId, id -> new ConcurrentLinkedQueue<>());
        if (queue.size() >= maxMessageQueueSize)
            return false;

        return queue.offer(message);
    }

    private static void tickQueue(MinecraftServer server) {
        // 10 ticks = 0.5 seconds
        if (server.getTickCount() % 10 != 0)
            return;

        for (var entry : messageQueue.entrySet()) {
            var uuid = entry.getKey();
            var queue = entry.getValue();
            var msg = queue.poll();
            if (msg == null || msg.conn.isClosed()) {
                messageQueue.remove(uuid);
                continue;
            }

            if (msg.type == MessageTypes.SAY) {
                Webhook.send(uuid, msg, null);
                server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(msg.message));
                msg.conn.send(RccChatbox.GSON.toJson(new SuccessPacket("message_sent", msg.id)));

                // Emit chat_chatbox event
                if(msg.sayPacket == null)
                    continue;

                var chatboxChatPacket = new ChatboxChatEvent();
                chatboxChatPacket.text = msg.content != null ? msg.content.getString() : null;
                chatboxChatPacket.name = msg.label != null ? msg.label.getString() : null;
                chatboxChatPacket.rawText = msg.sayPacket.text;
                chatboxChatPacket.rawName = msg.sayPacket.name != null ? msg.sayPacket.name : chatboxChatPacket.name;
                // funky stuff is no longer required.
                chatboxChatPacket.renderedText = RccChatbox.serializeComponent(msg.content,server.registryAccess());

                chatboxChatPacket.time = DateUtils.getTime(new Date());
                chatboxChatPacket.user = msg.ownerUser;

                RccChatbox.getInstance().wss().broadcastEvent(chatboxChatPacket, Capability.READ);
            } else if (msg.type == MessageTypes.TELL) {
                var player = server.getPlayerList().getPlayer(msg.player);
                if (player == null) {
                    var err = ClientErrors.UNKNOWN_USER;
                    msg.conn.send(RccChatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, msg.id)));
                    continue;
                }
                Webhook.send(uuid, msg, player);
                player.sendSystemMessage(msg.message);
                // Last line of defense ~~against qrmcat/bomber's wonderful software~~
                try {
                    msg.conn.send(RccChatbox.GSON.toJson(new SuccessPacket("message_sent", msg.id)));
                } catch(WebsocketNotConnectedException e) {
                    RccChatbox.LOGGER.warn("Was unable to send message confirmation to a disconnected websocket (UUID: {})", uuid);
                }
            }
        }
    }

    private static String enqueueAndResult(UUID licenseId, ClientMessage message, int id) {
        if (tryEnqueue(licenseId, message)) {
            return RccChatbox.GSON.toJson(new SuccessPacket("message_queued", id));
        } else {
            var err = ClientErrors.RATE_LIMITED;
            return RccChatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id));
        }
    }

    public static void register(final MinecraftServer mcServer) {
        RccChatbox.scheduler.scheduleAtFixedRate(() -> tickQueue(mcServer),0,500, TimeUnit.MILLISECONDS);

        ChatboxMessageEvents.SAY.register((client, packet) -> {
            var ownerId = client.license.userId();
            var owner = PlayerMeta.getPlayer(ownerId);

            var name = packet.name != null ? packet.name : owner.getEffectiveName();
            var label = TextComponents.formatLabel(name);
            var content = TextComponents.formatContent(packet.text, packet.mode);
            var message = Component.empty()
                    .append(TextComponents.sayPrefix)
                    .append(TextComponents.buildChatbotMessage(label, content, owner));

            var fullMessage = new ClientMessage(
                    client.webSocket,
                    packet.id != null ? packet.id : -1,
                    MessageTypes.SAY,
                    message,
                    null,
                    client.license.user,
                    packet,
                    label,
                    content
            );

            client.webSocket.send(enqueueAndResult(client.license.uuid(), fullMessage, packet.id));
        });

        ChatboxMessageEvents.TELL.register((client, packet) -> {
            var ownerId = client.license.userId();
            var owner = PlayerMeta.getPlayer(ownerId);

            var player = mcServer.getPlayerList().getPlayerByName(packet.user);
            if (player == null) {
                var err = ClientErrors.UNKNOWN_USER;
                client.webSocket.send(RccChatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, packet.id)));
                return;
            }

            var name = packet.name != null ? packet.name : owner.getEffectiveName();
            var label = TextComponents.formatLabel(name);
            var content = TextComponents.formatContent(packet.text, packet.mode);

            var message = Component.empty()
                    .append(TextComponents.tellPrefix)
                    .append(TextComponents.buildChatbotMessage(label, content, owner));

            var fullMessage = new ClientMessage(
                    client.webSocket,
                    packet.id != null ? packet.id : -1,
                    MessageTypes.TELL,
                    message,
                    player.getUUID(),
                    client.license.user,
                    null, null, null
            );
            client.webSocket.send(enqueueAndResult(client.license.uuid(), fullMessage, packet.id));
        });
    }

    public enum MessageTypes {
        TELL,
        SAY,
    }

    public record ClientMessage(
            WebSocket conn,
            int id,
            MessageTypes type,
            Component message,
            @Nullable UUID player,

            // for chat_chatbox event purposes
            User ownerUser,
            @Nullable SayPacket sayPacket,
            @Nullable Component label,
            @Nullable Component content
    ) {
    }
}
