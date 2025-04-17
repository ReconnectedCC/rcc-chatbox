package cc.reconnected.chatbox.listeners;

import cc.reconnected.chatbox.ClientPacketsHandler;
import cc.reconnected.chatbox.RccChatbox;
import cc.reconnected.chatbox.api.events.ClientConnectionEvents;
import cc.reconnected.chatbox.api.events.PlayerCommandEvent;
import cc.reconnected.chatbox.license.Capability;
import cc.reconnected.chatbox.models.User;
import cc.reconnected.chatbox.packets.serverPackets.HelloPacket;
import cc.reconnected.chatbox.packets.serverPackets.PlayersPacket;
import cc.reconnected.chatbox.packets.serverPackets.events.*;
import cc.reconnected.chatbox.state.StateSaverAndLoader;
import cc.reconnected.chatbox.utils.DateUtils;
import cc.reconnected.chatbox.ws.CloseCodes;
import cc.reconnected.chatbox.ws.WsServer;
import cc.reconnected.library.data.PlayerMeta;
import cc.reconnected.library.text.parser.MarkdownParser;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.InetSocketAddress;
import java.util.*;

public class ChatboxEvents {
    public static final Set<Character> publicPrefixes = Set.of('\\');
    public static final Set<Character> ownerPrefixes = Set.of('|', '^');

    public static final HashMap<UUID, Boolean> spyingPlayers = new HashMap<>();

    private static MinecraftServer mcServer;

    public static void register() {
        ClientPacketsHandler.register();
        ClientConnectionEvents.CONNECT.register((conn, license, isGuest) -> {
            var playerData = PlayerMeta.getPlayer(license.userId());

            var helloPacket = new HelloPacket();
            helloPacket.capabilities = license.capabilities().stream().map(c -> c.toString().toLowerCase()).toArray(String[]::new);
            helloPacket.guest = isGuest;
            if (!isGuest) {
                helloPacket.licenseOwner = playerData.getEffectiveName();

                var mcPlayer = mcServer.getPlayerManager().getPlayer(license.userId());
                if (mcPlayer != null) {
                    helloPacket.licenseOwnerUser = User.create(mcPlayer);
                } else {
                    helloPacket.licenseOwnerUser = User.tryGet(license.userId(), true);
                }
                license.user = helloPacket.licenseOwnerUser;
            }

            var packetJson = RccChatbox.GSON.toJson(helloPacket);
            conn.send(packetJson);
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server ->
                mcServer = server);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var wss = new WsServer(new InetSocketAddress(RccChatbox.CONFIG.hostname, RccChatbox.CONFIG.port));

            var wssThread = new Thread(wss::start);
            wssThread.start();

            RccChatbox.getInstance().wss(wss);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (RccChatbox.getInstance().wss() != null) {
                try {
                    var wss = RccChatbox.getInstance().wss();
                    wss.closeAllClients(CloseCodes.SERVER_STOPPING);
                    wss.stop();
                } catch (InterruptedException e) {
                    RccChatbox.LOGGER.error("Failed to stop WebSocket server", e);
                }
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var joinPacket = new JoinEvent();
            joinPacket.user = User.create(handler.getPlayer());
            joinPacket.time = DateUtils.getTime(new Date());

            var playerState = StateSaverAndLoader.getPlayerState(handler.getPlayer());
            spyingPlayers.put(handler.getPlayer().getUuid(), playerState.enableSpy);

            RccChatbox.getInstance().wss().broadcastEvent(joinPacket, Capability.READ);

            var list = new ArrayList<>(server.getPlayerManager().getPlayerList());
            list.add(handler.getPlayer());
            RccChatbox.getInstance().wss().broadcastEvent(createPlayersPacket(list), Capability.READ);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var leavePacket = new LeaveEvent();
            leavePacket.user = User.create(handler.getPlayer());
            leavePacket.time = DateUtils.getTime(new Date());

            spyingPlayers.remove(handler.getPlayer().getUuid());

            RccChatbox.getInstance().wss().broadcastEvent(leavePacket, Capability.READ);

            var list = new ArrayList<>(server.getPlayerManager().getPlayerList());
            list.removeIf(p -> p.getUuid() == handler.getPlayer().getUuid());
            RccChatbox.getInstance().wss().broadcastEvent(createPlayersPacket(list), Capability.READ);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof ServerPlayerEntity player))
                return;

            var deathPacket = new DeathEvent();
            var source = damageSource.getSource();

            var message = damageSource.getDeathMessage(entity);
            deathPacket.text = message.getString();
            deathPacket.rawText = message.getString();
            var json = Text.Serialization.toJsonString(message, mcServer.getRegistryManager());
            deathPacket.renderedText = JsonParser.parseString(json);
            deathPacket.user = User.create(player);
            if (source instanceof ServerPlayerEntity) {
                deathPacket.source = User.create((ServerPlayerEntity) source);
            }
            deathPacket.time = DateUtils.getTime(new Date());

            RccChatbox.getInstance().wss().broadcastEvent(deathPacket, Capability.READ);
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            var worldChangePacket = new WorldChangeEvent();
            worldChangePacket.user = User.create(player);
            //getRegistryKey().getValue().toString();
            worldChangePacket.origin = origin.getRegistryKey().getValue().toString();
            worldChangePacket.destination = destination.getRegistryKey().getValue().toString();
            worldChangePacket.time = DateUtils.getTime(new Date());

            RccChatbox.getInstance().wss().broadcastEvent(worldChangePacket, Capability.READ);
        });

        // chat messages
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            var packet = new InGameChatEvent();

            var parsedMessage = MarkdownParser.defaultParser.parseNode(message.getContent().getString()).toText();
            packet.text = parsedMessage.getString();
            packet.rawText = message.getContent().getString();
            var json = Text.Serialization.toJsonString(message.getContent(), mcServer.getRegistryManager());
            packet.renderedText = JsonParser.parseString(json);
            packet.time = DateUtils.getTime(new Date());
            packet.user = User.create(sender);

            RccChatbox.getInstance().wss().broadcastEvent(packet, Capability.READ);
        });

        // Handle chatbox command packet sending
        PlayerCommandEvent.EVENT.register((player, command, args, ownerOnly) -> {
            var packet = new CommandEvent();
            packet.command = command;
            packet.args = args;
            packet.ownerOnly = ownerOnly;
            packet.user = User.create(player);
            packet.time = DateUtils.getTime(new Date());

            if (ownerOnly) {
                RccChatbox.getInstance().wss().broadcastOwnerEvent(packet, Capability.COMMAND, player.getUuid());
            } else {
                RccChatbox.getInstance().wss().broadcastEvent(packet, Capability.COMMAND);
            }
        });

        // listen to chatbox commands
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            var content = message.getContent().getString();
            if (content.isEmpty())
                return true;

            var prefix = content.charAt(0);
            if (!publicPrefixes.contains(prefix) && !ownerPrefixes.contains(prefix))
                return true;

            var isOwnerOnly = ownerPrefixes.contains(prefix);

            var cbLog = String.format("%s: %s", sender.getName().getString(), content);
            RccChatbox.LOGGER.info(cbLog);

            var tokens = content.split(" +");

            if (tokens.length == 0) {
                return false;
            }

            var command = tokens[0].substring(1);
            var args = Arrays.copyOfRange(tokens, 1, tokens.length);

            PlayerCommandEvent.EVENT.invoker().onCommand(sender, command, args, isOwnerOnly);

            if (!isOwnerOnly) {
                var server = sender.getServer();
                if (server == null)
                    return true;

                var playerManager = server.getPlayerManager();
                var text = Text
                        .literal(sender.getName().getString() + ": ").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY))
                        .append(Text.literal(content).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));

                playerManager.getPlayerList().forEach(player -> {
                    if (spyingPlayers.containsKey(player.getUuid()) && spyingPlayers.get(player.getUuid())) {
                        player.sendMessage(text, false);
                    }
                });
            }

            return false;
        });
    }

    public static PlayersPacket createPlayersPacket(List<ServerPlayerEntity> list) {
        var packet = new PlayersPacket();
        packet.time = DateUtils.getTime(new Date());

        packet.players = new User[list.size()];
        for (int i = 0; i < list.size(); i++) {
            var player = list.get(i);
            var user = User.create(player, true);
            packet.players[i] = user;
        }

        return packet;
    }

    public static PlayersPacket createPlayersPacket() {
        var list = mcServer.getPlayerManager().getPlayerList();

        return createPlayersPacket(list);
    }
}
