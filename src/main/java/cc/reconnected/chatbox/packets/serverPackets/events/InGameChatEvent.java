package cc.reconnected.chatbox.packets.serverPackets.events;

import cc.reconnected.chatbox.models.User;
import com.google.gson.JsonElement;

public class InGameChatEvent extends EventBase {
    public String text;
    public String rawText;
    public JsonElement renderedText;
    public User user;
    public String time;

    public InGameChatEvent() {
        this.event = "chat_ingame";
    }
}
