package cc.reconnected.chatbox.packets.serverPackets.events;

import cc.reconnected.chatbox.models.User;

public class WorldChangeEvent extends EventBase {
    public User user;
    public String origin;
    public String destination;
    public String time;

    public WorldChangeEvent() {
        this.event = "world_change";
    }
}
