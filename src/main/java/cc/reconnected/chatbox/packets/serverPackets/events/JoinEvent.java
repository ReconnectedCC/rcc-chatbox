package cc.reconnected.chatbox.packets.serverPackets.events;

import cc.reconnected.chatbox.models.User;

public class JoinEvent extends EventBase {
    public User user;
    public String time;

    public JoinEvent() {
        this.event = "join";
    }
}
