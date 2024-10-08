package cc.reconnected.chatbox.packets.serverPackets.events;

import cc.reconnected.chatbox.models.DiscordUser;

public class DiscordChatEvent extends EventBase {
    public String text;
    public String rawText;
    public String renderedText;
    public String discordId;
    public DiscordUser discordUser;
    public boolean edited;
    public String time;

    public DiscordChatEvent() {
        this.event = "chat_discord";
    }
}
