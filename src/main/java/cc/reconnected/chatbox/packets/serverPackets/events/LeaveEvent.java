package cc.reconnected.chatbox.packets.serverPackets.events;

import cc.reconnected.chatbox.models.User;

public class LeaveEvent extends EventBase {
    public User user;
    public String time;

    public LeaveEvent() {
        this.event = "leave";
    }
}
