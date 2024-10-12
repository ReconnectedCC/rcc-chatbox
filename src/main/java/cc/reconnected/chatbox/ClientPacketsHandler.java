package cc.reconnected.chatbox;

import cc.reconnected.chatbox.api.events.ChatboxSay;
import cc.reconnected.chatbox.api.events.ChatboxTell;
import cc.reconnected.chatbox.packets.serverPackets.ErrorPacket;
import cc.reconnected.chatbox.packets.serverPackets.SuccessPacket;
import cc.reconnected.chatbox.utils.TextComponents;
import cc.reconnected.chatbox.ws.ClientErrors;
import cc.reconnected.server.RccServer;
import cc.reconnected.server.database.PlayerData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class ClientPacketsHandler {
    public static final int maxMessageQueSize = 5;

    // License UUID = queue
    private static final HashMap<UUID, Queue<ClientMessage>> messageQueue = new HashMap<>();

    private static MinecraftServer mcServer;

    private static boolean tryEnqueue(UUID licenseId, ClientMessage message) {
        var queue = messageQueue.computeIfAbsent(licenseId, id -> new LinkedList<>());
        if(queue.size() >= maxMessageQueSize)
            return false;

        return queue.offer(message);
    }

    private static void tickQueue(MinecraftServer server) {
        // 10 ticks = 0.5 seconds
        if(server.getTicks() % 10 != 0)
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
                mcServer.getPlayerManager().getPlayerList().forEach(player -> {
                    player.sendMessage(msg.content);
                });
                msg.conn.send(Chatbox.GSON.toJson(new SuccessPacket("message_sent", msg.id)));
            } else if (msg.type == MessageTypes.TELL) {
                var player = server.getPlayerManager().getPlayer(msg.player);
                if (player == null) {
                    var err = ClientErrors.UNKNOWN_USER;
                    msg.conn.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, msg.id)));
                    continue;
                }
                player.sendMessage(msg.content);

                msg.conn.send(Chatbox.GSON.toJson(new SuccessPacket("message_sent", msg.id)));
            }
        }
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> mcServer = server);
        ServerTickEvents.END_SERVER_TICK.register(ClientPacketsHandler::tickQueue);

        ChatboxSay.EVENT.register((client, packet) -> {
            var ownerId = client.license.userId();
            var owner = PlayerData.getPlayer(ownerId);

            var name = packet.name != null ? packet.name : owner.getEffectiveName();
            var message = Component.empty()
                    .append(TextComponents.sayPrefix)
                    .appendSpace()
                    .append(TextComponents.buildChatbotMessage(name, packet.text, packet.mode, owner));

            var fullMessage = new ClientMessage(client.webSocket, packet.id != null ? packet.id : -1, MessageTypes.SAY, message, null);
            if(tryEnqueue(client.license.uuid(), fullMessage)) {
                client.webSocket.send(Chatbox.GSON.toJson(new SuccessPacket("message_queued", packet.id)));
            } else {
                var err = ClientErrors.RATE_LIMITED;
                client.webSocket.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, packet.id)));
            }

        });

        ChatboxTell.EVENT.register((client, packet) -> {
            var ownerId = client.license.userId();
            var owner = PlayerData.getPlayer(ownerId);
            if (owner == null) {
                var err = ClientErrors.UNKNOWN_ERROR;
                client.webSocket.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, packet.id)));
                return;
            }

            var name = packet.name != null ? packet.name : owner.getEffectiveName();

            var player = mcServer.getPlayerManager().getPlayer(packet.user);
            if (player == null) {
                var err = ClientErrors.UNKNOWN_USER;
                client.webSocket.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, packet.id)));
                return;
            }

            var message = Component.empty()
                    .append(TextComponents.tellPrefix)
                    .appendSpace()
                    .append(TextComponents.buildChatbotMessage(name, packet.text, packet.mode, owner));

            var fullMessage = new ClientMessage(client.webSocket, packet.id != null ? packet.id : -1, MessageTypes.TELL, message, player.getUuid());
            if(tryEnqueue(client.license.uuid(), fullMessage)) {
                client.webSocket.send(Chatbox.GSON.toJson(new SuccessPacket("message_queued", packet.id)));
            } else {
                var err = ClientErrors.RATE_LIMITED;
                client.webSocket.send(Chatbox.GSON.toJson(new ErrorPacket(err.getErrorMessage(), err.message, packet.id)));
            }
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
            Component content,
            @Nullable UUID player
    ) {
    }
}
