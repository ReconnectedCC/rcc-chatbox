package cc.reconnected.chatbox.packets.serverPackets;

public class SuccessPacket extends PacketBase {
    public String reason;

    public SuccessPacket() {
        this.type = "success";
        this.ok = true;
    }

    public SuccessPacket(String reason, int id) {
        this.type = "success";
        this.ok = true;
        this.reason = reason;
        this.id = id;
    }
}
