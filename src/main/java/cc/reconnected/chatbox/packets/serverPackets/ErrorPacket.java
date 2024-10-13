package cc.reconnected.chatbox.packets.serverPackets;

import org.jetbrains.annotations.Nullable;

public class ErrorPacket extends PacketBase {
    public final String error;
    public final String message;

    public ErrorPacket(String error, String message, @Nullable Integer id) {
        this.type = "error";
        this.ok = false;
        this.error = error;
        this.message = message;
        if (id != null) {
            this.id = id;
        }
    }
}
