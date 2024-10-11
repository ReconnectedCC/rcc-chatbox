package cc.reconnected.chatbox;

import cc.reconnected.chatbox.data.StateSaverAndLoader;
import com.google.gson.Gson;
import cc.reconnected.chatbox.license.LicenseManager;
import cc.reconnected.chatbox.ws.WsServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Chatbox implements ModInitializer {

    public static final String MOD_ID = "rcc-chatbox";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final cc.reconnected.chatbox.ChatboxConfig CONFIG = cc.reconnected.chatbox.ChatboxConfig.createAndLoad();
    public static final Gson GSON = new Gson();
    public static LicenseManager LicenseManager = new LicenseManager();

    private static Chatbox INSTANCE;

    public static Chatbox getInstance() {
        return INSTANCE;
    }

    public Chatbox() {
        INSTANCE = this;
    }

    private WsServer wss;

    public void wss(WsServer wss) {
        this.wss = wss;
    }

    public WsServer wss() {
        return wss;
    }

    private StateSaverAndLoader serverState;
    public StateSaverAndLoader serverState() {
        return serverState;
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(ChatboxCommand::register);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverState = StateSaverAndLoader.getServerState(server);
        });

        ChatboxEvents.register();
    }
}
