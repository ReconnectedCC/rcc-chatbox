package cc.reconnected.chatbox.packets.serverPackets;

public class PingPacket extends PacketBase {
    public PingPacket() {
        this.type = "ping";
    }
}
