package cc.reconnected.chatbox.listeners;

import cc.reconnected.chatbox.RccChatbox;
import cc.reconnected.chatbox.api.events.ClientConnectionEvents;
import cc.reconnected.chatbox.license.Capability;
import cc.reconnected.chatbox.models.User;
import cc.reconnected.chatbox.packets.serverPackets.events.AfkEvent;
import cc.reconnected.chatbox.packets.serverPackets.events.ServerRestartCancelledEvent;
import cc.reconnected.chatbox.packets.serverPackets.events.ServerRestartScheduledEvent;
import cc.reconnected.chatbox.utils.DateUtils;
import me.alexdevs.solstice.api.events.PlayerActivityEvents;
import me.alexdevs.solstice.api.events.RestartEvents;
import me.alexdevs.solstice.modules.timeBar.TimeBar;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class SolsticeEvents {
    @Nullable
    private static TimeBar restartBar = null;

    @Nullable
    private static ServerRestartScheduledEvent restartScheduledEvent = null;

    public static void register() {

        ClientConnectionEvents.CONNECT.register((conn, license, isGuest) -> {
            if (license.capabilities().contains(Capability.READ)) {
                var playersPacket = RccChatbox.GSON.toJson(ChatboxEvents.createPlayersPacket());
                conn.send(playersPacket);

                if (restartBar != null) {
                    fixRestartTime();
                    var restartPacket = RccChatbox.GSON.toJson(restartScheduledEvent);
                    conn.send(restartPacket);
                }
            }
        });

        PlayerActivityEvents.AFK.register((player, server) -> {
            var packet = new AfkEvent();
            packet.user = User.create(player);
            packet.time = DateUtils.getTime(new Date());

            RccChatbox.getInstance().wss().broadcastEvent(packet, Capability.READ);
        });

        PlayerActivityEvents.AFK_RETURN.register((player, server) -> {
            var packet = new AfkEvent();
            packet.user = User.create(player);
            packet.time = DateUtils.getTime(new Date());

            RccChatbox.getInstance().wss().broadcastEvent(packet, Capability.READ);
        });

        RestartEvents.SCHEDULED.register(timeBar -> {
            restartBar = timeBar;
            var packet = new ServerRestartScheduledEvent();
            var now = new Date();
            var duration = Duration.ofSeconds(timeBar.getTime());
            var restartAt = duration.addTo(now.toInstant());
            var restartAtDate = Date.from(Instant.from(restartAt));

            packet.time = DateUtils.getTime(now);
            packet.restartAt = DateUtils.getTime(restartAtDate);
            packet.restartType = "automatic"; // manual currently not supported

            fixRestartTime();

            RccChatbox.getInstance().wss().broadcastEvent(packet, Capability.READ);
        });

        RestartEvents.CANCELED.register(timeBar -> {
            if (restartScheduledEvent == null)
                return;

            var packet = new ServerRestartCancelledEvent();
            packet.time = DateUtils.getTime(new Date());
            packet.restartType = restartScheduledEvent.restartType;

            restartBar = null;
            restartScheduledEvent = null;
            RccChatbox.getInstance().wss().broadcastEvent(packet, Capability.READ);
        });
    }

    private static void fixRestartTime() {
        if (restartScheduledEvent == null)
            return;

        restartScheduledEvent.restartSeconds = restartBar.getRemainingSeconds();
    }
}
