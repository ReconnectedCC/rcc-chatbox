package cc.reconnected.chatbox.packets.serverPackets.events;

import cc.reconnected.chatbox.models.User;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.Nullable;

public class DeathEvent extends EventBase {
    public String text;
    public String rawText;
    public JsonElement renderedText;
    public User user;
    @Nullable
    public User source;
    public String time;

    public DeathEvent() {
        this.event = "death";
    }
}
