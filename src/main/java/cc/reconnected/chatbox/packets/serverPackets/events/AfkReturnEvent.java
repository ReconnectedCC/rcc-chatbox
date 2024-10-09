package cc.reconnected.chatbox.packets.serverPackets.events;

import cc.reconnected.chatbox.models.User;

public class AfkReturnEvent extends EventBase {
    public User user;
    public String time;

    public AfkReturnEvent() {
        this.event = "afk_return";
    }
}
