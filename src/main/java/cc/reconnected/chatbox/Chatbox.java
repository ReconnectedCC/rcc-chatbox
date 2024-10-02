package cc.reconnected.chatbox;

import com.google.gson.Gson;
import cc.reconnected.chatbox.license.LicenseManager;
import cc.reconnected.chatbox.ws.WsServer;
import cc.reconnected.discordbridge.events.DiscordMessage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class Chatbox implements ModInitializer {

    public static final String MOD_ID = "rcc-chatbox";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final cc.reconnected.chatbox.ChatboxConfig CONFIG = cc.reconnected.chatbox.ChatboxConfig.createAndLoad();
    public static final Gson GSON = new Gson();
    public static LicenseManager LicenseManager = new LicenseManager();

    public Chatbox() {
        INSTANCE = this;
    }

    private static Chatbox INSTANCE;
    public static Chatbox getInstance() {
        return INSTANCE;
    }

    private WsServer wss;

    private ChatboxDatabase database;
    public ChatboxDatabase database() {
        return database;
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(ChatboxCommand::register);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            wss = new WsServer(new InetSocketAddress(CONFIG.hostname(), CONFIG.port()));
            var wssThread = new Thread(() -> wss.start());
            wssThread.start();

            database = new ChatboxDatabase();
            database.ensureDatabaseCreated();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            try {
                wss.stop();
            } catch (InterruptedException e) {
                LOGGER.error("Failed to stop WebSocket server", e);
            }
        });

        // discord chat events
        DiscordMessage.MESSAGE_CREATE.register((message, member, isEdited) -> {

        });

        // chatbox commands
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if(!message.getContent().getString().startsWith("\\")) {
                return true;
            }
            Chatbox.LOGGER.info("{}: {}", sender.getName().getString(), message.getContent().getString());



            return false;
        });
    }
}
