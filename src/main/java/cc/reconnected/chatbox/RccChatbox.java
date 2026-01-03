package cc.reconnected.chatbox;

import cc.reconnected.chatbox.command.ChatboxCommand;
import cc.reconnected.chatbox.listeners.ChatboxEvents;
import cc.reconnected.chatbox.packets.serverPackets.PingPacket;
import cc.reconnected.chatbox.state.StateSaverAndLoader;
import cc.reconnected.chatbox.license.LicenseManager;
import cc.reconnected.chatbox.ws.WsServer;
import cc.reconnected.library.config.ConfigManager;
import com.google.gson.Gson;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RccChatbox implements ModInitializer {

    public static final String MOD_ID = "rcc-chatbox";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static RccChatboxConfig CONFIG;
    public static final Gson GSON = new Gson();
    private static LicenseManager licenseManager;

    private static RccChatbox INSTANCE;

    public static RccChatbox getInstance() {
        return INSTANCE;
    }

    public RccChatbox() {
        INSTANCE = this;
    }

    private WsServer wss;

    public static LicenseManager licenseManager() {
        return licenseManager;
    }

    public void wss(WsServer wss) {
        this.wss = wss;
    }

    public WsServer wss() {
        return wss;
    }

    private static Path dataDirectory;

    public static Path dataDirectory() {
        return dataDirectory;
    }

    private StateSaverAndLoader serverState;

    public StateSaverAndLoader serverState() {
        return serverState;
    }

    public static boolean isSolsticeLoaded() {
        return FabricLoader.getInstance().isModLoaded("solstice");
    }

    public static boolean isRccDiscordLoaded() {
        return FabricLoader.getInstance().isModLoaded("rcc-discord");
    }

    public static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Override
    public void onInitialize() {

        try {
            CONFIG = ConfigManager.load(RccChatboxConfig.class);
        } catch (Exception e) {
            LOGGER.error("Failed to load config. Refusing to continue.", e);
            return;
        }

        CommandRegistrationCallback.EVENT.register(ChatboxCommand::register);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            dataDirectory = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(MOD_ID);
            licenseManager = new LicenseManager();
            if (!dataDirectory.toFile().isDirectory()) {
                if (!dataDirectory.toFile().mkdir()) {
                    LOGGER.error("Failed to create rcc-chatbox data directory");
                }
            }

            ChatboxEvents.register(server);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverState = StateSaverAndLoader.getServerState(server);

            scheduler.scheduleAtFixedRate(() -> {
                var pingPacket = new PingPacket();
                wss.broadcastEvent(pingPacket, null);
            }, 0, 1, TimeUnit.MINUTES);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            scheduler.shutdown();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            scheduler.shutdownNow();
        });
    }
}
