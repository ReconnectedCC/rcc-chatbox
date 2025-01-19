package cc.reconnected.chatbox.integrations;

import me.alexdevs.solstice.Solstice;
import me.alexdevs.solstice.modules.afk.AfkModule;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class SolsticeIntegration {

    public static boolean isPlayerAfk(ServerPlayerEntity player) {
        var afkModule = Solstice.modules.getModule(AfkModule.class);
        return afkModule.isPlayerAfk(player);
    }
}
