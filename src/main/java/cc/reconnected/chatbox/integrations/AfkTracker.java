package cc.reconnected.chatbox.integrations;

import cc.reconnected.chatbox.RccChatbox;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class AfkTracker {
    public static boolean isPlayerAfk(ServerPlayerEntity player) {
        if (!RccChatbox.isSolsticeLoaded())
            return false;

        return SolsticeIntegration.isPlayerAfk(player);
    }

}
