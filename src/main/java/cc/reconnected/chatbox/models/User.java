package cc.reconnected.chatbox.models;

import cc.reconnected.discordbridge.RccDiscord;
import cc.reconnected.essentials.core.AfkTracker;
import cc.reconnected.library.data.PlayerMeta;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

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

    private static void fillInData(User user, @Nullable ServerPlayerEntity entity, boolean resolveDiscord) {
        PlayerMeta playerData;
        if(entity != null) {
            playerData = PlayerMeta.getPlayer(entity);
            user.afk = AfkTracker.getInstance().isPlayerAfk(entity.getUuid());
        } else {
            playerData = PlayerMeta.getPlayer(UUID.fromString(user.uuid));
            user.afk = false;
        }

        user.group = playerData.getPrimaryGroup();
        user.pronouns = playerData.get(PlayerMeta.KEYS.pronouns);
        user.alt = playerData.getBoolean(PlayerMeta.KEYS.isAlt);
        user.bot = playerData.getBoolean(PlayerMeta.KEYS.isBot);

        user.supporter = 0;
        var supporterStr = playerData.get(PlayerMeta.KEYS.supporterLevel);
        if (supporterStr != null) {
            user.supporter = Integer.parseInt(supporterStr);
        }

        user.linkedUser = null;
        if(resolveDiscord) {
            var discordId = playerData.get(PlayerMeta.KEYS.discordId);
            if (discordId != null) {
                var member = RccDiscord.getInstance().getClient().guild().getMember(UserSnowflake.fromId(discordId));
                if (member != null) {
                    user.linkedUser = DiscordUser.fromMember(member, false);
                }
            }
        }
    }

    public static User create(ServerPlayerEntity player, boolean resolveDiscord) {
        var user = new User();

        user.name = player.getEntityName();
        user.uuid = player.getUuidAsString();
        user.displayName = player.getDisplayName().getString();
        user.world = player.getWorld().getRegistryKey().getValue().toString();

        fillInData(user, player, resolveDiscord);

        return user;
    }

    public static User create(ServerPlayerEntity player) {
        return create(player, false);
    }


    public static User tryGet(UUID playerUuid, boolean resolveDiscord) {
        var user = new User();

        var playerData = PlayerMeta.getPlayer(playerUuid);

        user.uuid = playerUuid.toString();
        user.name = playerData.getEffectiveName();
        user.displayName = playerData.getEffectiveName();
        user.world = null;

        fillInData(user, null, resolveDiscord);

        return user;
    }

    public static User tryGet(UUID playerUuid) {
        return tryGet(playerUuid, false);
    }
}
