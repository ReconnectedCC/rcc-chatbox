package ct.chatbox.models;

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
}
