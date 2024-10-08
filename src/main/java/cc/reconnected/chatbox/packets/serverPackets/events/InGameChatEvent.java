package cc.reconnected.chatbox.packets.serverPackets.events;

import cc.reconnected.chatbox.models.User;

public class InGameChatEvent extends EventBase {
    public String text;
    public String rawText;
    public String renderedText;
    public User user;
    public String time;

    public InGameChatEvent() {
        this.event = "chat_ingame";
    }
}
