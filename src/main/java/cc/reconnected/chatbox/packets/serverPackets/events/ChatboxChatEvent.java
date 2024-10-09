package cc.reconnected.chatbox.packets.serverPackets.events;

import cc.reconnected.chatbox.models.User;
import com.google.gson.JsonElement;

// TODO: Add chatbox message event

public class ChatboxChatEvent extends EventBase {
    public String text;
    public String rawText;
    public JsonElement renderedText;
    public User user;
    public String name;
    public String rawName;
    public String time;

    public ChatboxChatEvent() {
        this.event = "chat_chatbox";
    }
}
