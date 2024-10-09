package cc.reconnected.chatbox.packets.serverPackets.events;

import cc.reconnected.chatbox.models.DiscordUser;
import com.google.gson.JsonElement;

public class DiscordChatEvent extends EventBase {
    public String text;
    public String rawText;
    public JsonElement renderedText;
    public String discordId;
    public DiscordUser discordUser;
    public boolean edited;
    public String time;

    public DiscordChatEvent() {
        this.event = "chat_discord";
    }
}
