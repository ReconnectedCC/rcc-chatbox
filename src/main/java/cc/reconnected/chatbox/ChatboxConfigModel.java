package cc.reconnected.chatbox;

import io.wispforest.owo.config.annotation.Config;

@Config(name = "rcc-chatbox-config", wrapperName = "ChatboxConfig")
public class ChatboxConfigModel {
    public String hostname = "127.0.0.1";
    public short port = 25580;
    public String guestAllowedAddress = "127.0.0.1";
}
