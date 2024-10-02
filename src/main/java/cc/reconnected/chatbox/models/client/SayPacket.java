package cc.reconnected.chatbox.models.client;

import org.jetbrains.annotations.Nullable;

public class SayPacket extends ClientPacketBase {
    public String type = "say";
    public String text;
    @Nullable
    public String name;
    @Nullable
    public String mode = "markdown";
}
