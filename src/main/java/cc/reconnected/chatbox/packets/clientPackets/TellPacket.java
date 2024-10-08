package cc.reconnected.chatbox.packets.clientPackets;

import org.jetbrains.annotations.Nullable;

public class TellPacket extends ClientPacketBase {
    public String user;
    public String text;
    @Nullable
    public String name;
    @Nullable
    public String mode = "markdown";
}
