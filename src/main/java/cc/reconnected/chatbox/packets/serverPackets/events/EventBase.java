package cc.reconnected.chatbox.packets.serverPackets.events;

import cc.reconnected.chatbox.packets.serverPackets.PacketBase;

public class EventBase extends PacketBase {
    public String event;

    public EventBase() {
        this.type = "event";
    }
}
