package cc.reconnected.chatbox.packets.serverPackets;

import cc.reconnected.chatbox.Utils;
import cc.reconnected.chatbox.models.User;

import java.util.Date;

public class PlayersPacket extends PacketBase {
    public String time = Utils.getTime(new Date());
    public User[] players;

    public PlayersPacket() {
        this.type = "players";
    }
}
