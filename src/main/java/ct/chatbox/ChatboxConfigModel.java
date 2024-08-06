package ct.chatbox;

import io.wispforest.owo.config.annotation.Config;

@Config(name = "ct-chatbox-config", wrapperName = "ChatboxConfig")
public class ChatboxConfigModel {
    public String hostname = "127.0.0.1";
    public short port = 25580;
}
