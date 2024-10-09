package cc.reconnected.chatbox.models;

import cc.reconnected.discordbridge.Bridge;
import cc.reconnected.server.RccServer;
import net.dv8tion.jda.api.entities.UserSnowflake;
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
        // TODO: find a way to get rank of user
        // possible values: default, admin
        user.group = "default";
        user.world = player.getWorld().getRegistryKey().getValue().toString();

        var playerData = RccServer.getInstance().playerTable().getPlayerData(player.getUuid());
        if (playerData != null) {
            user.pronouns = playerData.pronouns();
            user.afk = false;
            user.alt = playerData.isAlt();
            user.bot = playerData.isBot();

            // TODO: link with supporter
            user.supporter = 0;

            user.linkedUser = null;
            var discordId = playerData.discordId();
            if (discordId != null) {
                var member = Bridge.getInstance().getClient().guild().getMember(UserSnowflake.fromId(discordId));
                if (member != null) {
                    user.linkedUser = DiscordUser.fromMember(member);
                }
            }
        }

        return user;
    }
}
