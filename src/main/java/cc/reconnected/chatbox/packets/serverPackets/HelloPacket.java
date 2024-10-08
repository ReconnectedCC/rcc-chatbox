package cc.reconnected.chatbox.packets.serverPackets;

import cc.reconnected.chatbox.models.User;

public class HelloPacket extends PacketBase {
    public boolean guest;
    public String licenseOwner;
    public User licenseOwnerUser;
    public String[] capabilities;

    public HelloPacket() {
        this.type = "hello";
    }
}
