package cc.reconnected.chatbox.packets.clientPackets;

import org.jetbrains.annotations.Nullable;

public class SayPacket extends ClientPacketBase {
    public String text;
    @Nullable
    public String name;
    @Nullable
    public String mode = "markdown";
}
