package cc.reconnected.chatbox.packets.serverPackets.events;

import cc.reconnected.chatbox.models.User;

// TODO: Add AFK events
public class AfkEvent extends EventBase {
    public User user;
    public String time;

    public AfkEvent() {
        this.event = "afk";
    }
}
