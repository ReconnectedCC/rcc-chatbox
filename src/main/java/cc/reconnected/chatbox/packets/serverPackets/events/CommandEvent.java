package cc.reconnected.chatbox.packets.serverPackets.events;

import cc.reconnected.chatbox.models.User;

public class CommandEvent extends EventBase {
    public User user;
    public String command;
    public String[] args;
    public boolean ownerOnly;
    public String time;

    public CommandEvent() {
        this.event = "command";
    }
}
