package cc.reconnected.chatbox;

import cc.reconnected.chatbox.command.ChatboxCommand;
import cc.reconnected.chatbox.listeners.ChatboxEvents;
import cc.reconnected.chatbox.packets.serverPackets.PingPacket;
import cc.reconnected.chatbox.state.StateSaverAndLoader;
import cc.reconnected.chatbox.license.LicenseManager;
import cc.reconnected.chatbox.ws.WsServer;
import cc.reconnected.library.config.ConfigManager;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.MinecraftServer;
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
    private static MinecraftServer server;

    private static RccChatbox INSTANCE;

    public static RccChatbox getInstance() {
        return INSTANCE;
    }

    public static MinecraftServer server() {
        return server;
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
            RccChatbox.server = server;
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

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> scheduler.shutdown());

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> scheduler.shutdownNow());
    }

    public static JsonElement serializeComponent(Component component, HolderLookup.Provider provider) {
        var element = ComponentSerialization.CODEC.encodeStart(provider.createSerializationContext(JsonOps.INSTANCE), component).getOrThrow(JsonParseException::new);
        return expand(element);
    }

    public static JsonElement serializeComponent(net.kyori.adventure.text.Component component, HolderLookup.Provider provider) {
        var json = JSONComponentSerializer.json().serialize(component);
        var element = JsonParser.parseString(json);
        return expand(element);
    }


    private static JsonElement expand(JsonElement e) {
        if (e.isJsonPrimitive()) {
            var object = new JsonObject();
            object.add("text", e);
            return object;
        }
        if (e.isJsonArray()) {
            var array = e.getAsJsonArray();
            JsonObject object = expand(array.get(0)).getAsJsonObject();
            var extra = object.has("extra") ? object.getAsJsonArray("extra") : new JsonArray();
            for (int i = 1; i < array.size(); i++) extra.add(expand(array.get(i)));
            object.add("extra", extra);
            return object;
        }
        return e;
    }
}
