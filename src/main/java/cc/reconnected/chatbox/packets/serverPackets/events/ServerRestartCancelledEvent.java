package cc.reconnected.chatbox.packets.serverPackets.events;

public class ServerRestartCancelledEvent extends EventBase {
    public String restartType;
    public int restartSeconds;
    public String restartAt;
    public String time;
    public ServerRestartCancelledEvent() {
        this.event = "server_restart_cancelled";
    }
}
