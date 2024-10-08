package cc.reconnected.chatbox.models;

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class User {
    public String type = "ingame";
    public String name;
    public String uuid;
    public String displayName;
    public String group;
    public String pronouns;
    public String world;
    public boolean afk;
    public boolean alt;
    public boolean bot;
    public int supporter;
    @Nullable
    public DiscordUser linkedUser;

    public static User create(ServerPlayerEntity player) {
        var user = new User();

        user.name = player.getEntityName();
        user.uuid = player.getUuidAsString();
        user.displayName = player.getDisplayName().getString();
        user.group = "player";
        user.pronouns = "none";
        user.world = player.getWorld().getRegistryKey().getValue().toString();
        user.afk = false;
        user.alt = false;
        user.bot = false;
        user.supporter = 0;
        user.linkedUser = null;

        return user;
    }
}
