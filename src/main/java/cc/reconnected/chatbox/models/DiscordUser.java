package cc.reconnected.chatbox.models;

import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.Nullable;

public class DiscordUser {
    public String type = "discord";
    public String id;
    public String name;
    public String displayName;
    public String discriminator;
    public String avatar;
    public DiscordRole[] roles;
    @Nullable
    public User linkedUser;

    public static DiscordUser fromMember(Member member) {
        var user = new DiscordUser();

        user.id = member.getUser().getId();
        user.discriminator = member.getUser().getDiscriminator();
        user.name = member.getUser().getName();
        user.displayName = member.getEffectiveName();
        user.avatar = member.getUser().getAvatarUrl();

        var roles = member.getRoles();
        user.roles = new DiscordRole[roles.size()];
        for (int i = 0; i < roles.size(); i++) {
            var role = roles.get(i);
            var lRole = new DiscordRole();
            user.roles[i] = lRole;
            lRole.id = role.getId();
            lRole.name = role.getName();
            lRole.colour = role.getColorRaw();
        }

        // TODO: add linkedUser, requires rcc-discord to support linking

        return user;
    }
}
