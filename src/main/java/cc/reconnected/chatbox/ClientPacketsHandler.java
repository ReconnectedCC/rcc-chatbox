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
import cc.reconnected.chatbox.ws.ClientErrors;
import cc.reconnected.server.database.PlayerData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClientPacketsHandler {
    public static final int maxMessageQueSize = 5;

    // License UUID = queue
    private static final HashMap<UUID, Queue<ClientMessage>> messageQueue = new HashMap<>();

    private static MinecraftServer mcServer;

    private static boolean tryEnqueue(UUID licenseId, ClientMessage message) {
        var queue = messageQueue.computeIfAbsent(licenseId, id -> new LinkedList<>());
        if (queue.size() >= maxMessageQueSize)
            return false;

        return queue.offer(message);
    }

    private static void tickQueue(MinecraftServer server) {
        // 10 ticks = 0.5 seconds
        if (server.getTicks() % 10 != 0)
            return;

        for (var entry : messageQueue.entrySet()) {
            var uuid = entry.getKey();
            var queue = entry.getValue();
            var msg = queue.poll();
            if (msg == null) {
                messageQueue.remove(uuid);
                continue;
            }

            if (msg.type == MessageTypes.SAY) {
                mcServer.getPlayerManager().getPlayerList().forEach(player -> player.sendMessage(msg.message));
                msg.conn.send(Chatbox.GSON.toJson(new SuccessPacket("message_sent", msg.id)));

                // Emit chat_chatbox event
                if(msg.sayPacket == null)
                    continue;

                var chatboxChatPacket = new ChatboxChatEvent();
                chatboxChatPacket.text = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(msg.content));
                chatboxChatPacket.name = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(msg.label));
                chatboxChatPacket.rawText = msg.sayPacket.text;
                chatboxChatPacket.rawName = msg.sayPacket.name != null ? msg.sayPacket.name : chatboxChatPacket.name;
                // funky stuff
                var json = JSONComponentSerializer.json().serialize(msg.content);
                var mcText = Text.Serializer.fromJson(json);
                chatboxChatPacket.renderedText = Text.Serializer.toJsonTree(mcText);

                chatboxChatPacket.time = DateUtils.getTime(new Date());
                chatboxChatPacket.user = msg.ownerUser;

                Chatbox.getInstance().wss().broadcastEvent(chatboxChatPacket, Capability.READ);
            } else if (msg.type == MessageTypes.TELL) {
                var player = server.getPlayerManager().getPlayer(msg.player);
                if (player == null) {
                    var err = ClientErrors.UNKNOWN_USER;
                    msg.conn.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, msg.id)));
                    continue;
                }
                player.sendMessage(msg.message);

                msg.conn.send(Chatbox.GSON.toJson(new SuccessPacket("message_sent", msg.id)));
            }
        }
    }

    private static String enqueueAndResult(UUID licenseId, ClientMessage message, int id) {
        if (tryEnqueue(licenseId, message)) {
            return Chatbox.GSON.toJson(new SuccessPacket("message_queued", id));
        } else {
            var err = ClientErrors.RATE_LIMITED;
            return Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, id));
        }
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> mcServer = server);
        ServerTickEvents.END_SERVER_TICK.register(ClientPacketsHandler::tickQueue);

        ChatboxMessageEvents.SAY.register((client, packet) -> {
            var ownerId = client.license.userId();
            var owner = PlayerData.getPlayer(ownerId);

            var name = packet.name != null ? packet.name : owner.getEffectiveName();
            var label = TextComponents.formatLabel(name);
            var content = TextComponents.formatContent(packet.text, packet.mode);
            var message = Component.empty()
                    .append(TextComponents.sayPrefix)
                    .appendSpace()
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
            var owner = PlayerData.getPlayer(ownerId);

            var player = mcServer.getPlayerManager().getPlayer(packet.user);
            if (player == null) {
                var err = ClientErrors.UNKNOWN_USER;
                client.webSocket.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, packet.id)));
                return;
            }

            var name = packet.name != null ? packet.name : owner.getEffectiveName();
            var label = TextComponents.formatLabel(name);
            var content = TextComponents.formatContent(packet.text, packet.mode);

            var message = Component.empty()
                    .append(TextComponents.tellPrefix)
                    .appendSpace()
                    .append(TextComponents.buildChatbotMessage(label, content, owner));

            var fullMessage = new ClientMessage(
                    client.webSocket,
                    packet.id != null ? packet.id : -1,
                    MessageTypes.TELL,
                    message,
                    player.getUuid(),
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
