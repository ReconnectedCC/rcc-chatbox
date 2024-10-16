package cc.reconnected.chatbox;

import cc.reconnected.chatbox.command.ChatboxCommand;
import cc.reconnected.chatbox.state.StateSaverAndLoader;
import com.google.gson.Gson;
import cc.reconnected.chatbox.license.LicenseManager;
import cc.reconnected.chatbox.ws.WsServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Chatbox implements ModInitializer {

    public static final String MOD_ID = "rcc-chatbox";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final cc.reconnected.chatbox.ChatboxConfig CONFIG = cc.reconnected.chatbox.ChatboxConfig.createAndLoad();
    public static final Gson GSON = new Gson();
    private static LicenseManager licenseManager;

    private static Chatbox INSTANCE;

    public static Chatbox getInstance() {
        return INSTANCE;
    }

    public Chatbox() {
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

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(ChatboxCommand::register);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            dataDirectory = server.getSavePath(WorldSavePath.ROOT).resolve("data").resolve(MOD_ID);
            licenseManager = new LicenseManager();
            if (!dataDirectory.toFile().isDirectory()) {
                if (!dataDirectory.toFile().mkdir()) {
                    LOGGER.error("Failed to create rcc-chatbox data directory");
                }
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverState = StateSaverAndLoader.getServerState(server);
        });


        ChatboxEvents.register();
    }
}
