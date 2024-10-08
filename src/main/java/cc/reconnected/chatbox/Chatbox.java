package cc.reconnected.chatbox;

import com.google.gson.Gson;
import cc.reconnected.chatbox.license.LicenseManager;
import cc.reconnected.chatbox.ws.WsServer;
//import cc.reconnected.discordbridge.events.DiscordMessage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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

    private final ChatboxDatabase database = new ChatboxDatabase();
    public ChatboxDatabase database() {
        return database;
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(ChatboxCommand::register);
        GameEvents.register();
    }
}
