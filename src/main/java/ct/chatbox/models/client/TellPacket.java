package ct.chatbox.models.client;

import org.jetbrains.annotations.Nullable;

public class TellPacket extends ClientPacketBase {
    public String type = "say";
    public String user;
    public String text;
    @Nullable
    public String name;
    @Nullable
    public String mode = "markdown";
}
