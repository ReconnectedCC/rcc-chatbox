package cc.reconnected.chatbox.integrations;

import me.alexdevs.solstice.Solstice;

import java.util.UUID;

public class SolsticeIntegration {

    public static boolean isPlayerAfk(UUID player) {
        return Solstice.modules.afk.isPlayerAfk(player);
    }
}
