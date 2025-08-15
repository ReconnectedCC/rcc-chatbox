package cc.reconnected.chatbox.integrations;

import me.alexdevs.solstice.Solstice;
import me.alexdevs.solstice.modules.ModuleProvider;
import me.alexdevs.solstice.modules.afk.AfkModule;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class SolsticeIntegration {

    public static boolean isPlayerAfk(ServerPlayer player) {
        return ModuleProvider.AFK.isPlayerAfk(player);
    }
}
