package cc.reconnected.chatbox.packets.serverPackets.events;

// TODO: Add proper restart scheduling
public class ServerRestartScheduledEvent extends EventBase {
    public String restartType;
    public int restartSeconds;
    public String restartAt;
    public String time;
    public ServerRestartScheduledEvent() {
        this.event = "server_restart_scheduled";
    }
}
