package cc.reconnected.chatbox;

import cc.reconnected.chatbox.license.Capability;
import cc.reconnected.chatbox.models.DiscordUser;
import cc.reconnected.chatbox.packets.serverPackets.PlayersPacket;
import cc.reconnected.chatbox.packets.serverPackets.events.CommandEvent;
import cc.reconnected.chatbox.models.User;
import cc.reconnected.chatbox.packets.serverPackets.events.DiscordChatEvent;
import cc.reconnected.chatbox.packets.serverPackets.events.InGameChatEvent;
import cc.reconnected.chatbox.ws.WsServer;
import cc.reconnected.discordbridge.events.DiscordMessage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.MinecraftServer;

import java.net.InetSocketAddress;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

public class GameEvents {
    private static MinecraftServer mcServer;
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            mcServer = server;
            var wss = new WsServer(new InetSocketAddress(Chatbox.CONFIG.hostname(), Chatbox.CONFIG.port()));

            var wssThread = new Thread(wss::start);
            wssThread.start();

            Chatbox.getInstance().wss(wss);

            Chatbox.getInstance().database().ensureDatabaseCreated();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // TODO: closing packet
            try {
                Chatbox.getInstance().wss().stop();
            } catch (InterruptedException e) {
                Chatbox.LOGGER.error("Failed to stop WebSocket server", e);
            }
        });

        // discord chat events
        DiscordMessage.MESSAGE_CREATE.register((message, member, isEdited) -> {
            var user = DiscordUser.fromMember(member);
            var packet = new DiscordChatEvent();
            packet.text = message.getContentStripped();
            packet.rawText = message.getContentRaw();
            packet.renderedText = message.getContentDisplay();
            packet.discordId = message.getId();
            packet.discordUser = user;
            packet.edited = isEdited;
            packet.time = Utils.getTime(isEdited ? message.getTimeEdited() : message.getTimeCreated());

            Chatbox.getInstance().wss().broadcastEvent(packet, Capability.READ);
        });

        // chat messages
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            var packet = new InGameChatEvent();

            packet.text = message.getContent().getString();
            packet.rawText = message.getSignedContent();
            packet.renderedText = message.getContent().getString();
            packet.time = Utils.getTime(new Date());
            packet.user = User.create(sender);

            Chatbox.getInstance().wss().broadcastEvent(packet, Capability.READ);
        });

        // chatbox commands
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (!message.getContent().getString().startsWith("\\"))
                return true;

            var content = message.getContent().getString();
            Chatbox.LOGGER.info("{}: {}", sender.getName().getString(), content);

            var tokens = content.split(" +");

            if (tokens.length == 0) {
                return false;
            }

            var command = tokens[0];
            var args = Arrays.copyOfRange(tokens, 1, tokens.length);

            var packet = new CommandEvent();
            packet.command = command.substring(1);
            packet.args = args;
            packet.ownerOnly = false;
            packet.user = User.create(sender);
            packet.time = Utils.getTime(message.getTimestamp().atZone(ZoneId.of("UTC")));

            Chatbox.getInstance().wss().broadcastEvent(packet, Capability.COMMAND);

            return false;
        });
    }

    public static PlayersPacket createPlayersPacket() {
        var list = mcServer.getPlayerManager().getPlayerList();
        var packet = new PlayersPacket();
        packet.time = Utils.getTime(new Date());

        packet.players = new User[list.size()];
        for (int i = 0; i < list.size(); i++) {
            var player = list.get(i);
            var user = User.create(player);
            packet.players[i] = user;
        }

        return packet;
    }
}
