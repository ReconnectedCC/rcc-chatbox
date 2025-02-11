package cc.reconnected.chatbox;

import cc.reconnected.library.config.Config;
import org.jetbrains.annotations.Nullable;

@Config(RccChatbox.MOD_ID)
public class RccChatboxConfig {
    public String hostname = "127.0.0.1";
    public short port = 25580;
    public String guestAllowedAddress = "127.0.0.1";
    @Nullable
    public String webhook = null;
}
