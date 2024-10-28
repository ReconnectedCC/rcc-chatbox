package cc.reconnected.chatbox.packets.serverPackets.events;

public class ServerRestartCancelledEvent extends EventBase {
    public String restartType;
    public String time;
    public ServerRestartCancelledEvent() {
        this.event = "server_restart_cancelled";
    }
}
