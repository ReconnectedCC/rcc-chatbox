package cc.reconnected.chatbox.integrations;

import me.alexdevs.solstice.Solstice;
import me.alexdevs.solstice.modules.afk.AfkModule;

import java.util.UUID;

public class SolsticeIntegration {

    public static boolean isPlayerAfk(UUID player) {
        var afkModule = Solstice.modules.getModule(AfkModule.class);
        return afkModule.isPlayerAfk(player);
    }
}
