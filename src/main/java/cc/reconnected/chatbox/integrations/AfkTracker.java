package cc.reconnected.chatbox.integrations;

import cc.reconnected.chatbox.RccChatbox;
import net.minecraft.server.level.ServerPlayer;

public class AfkTracker {
    public static boolean isPlayerAfk(ServerPlayer player) {
        if (!RccChatbox.isSolsticeLoaded())
            return false;

        return SolsticeIntegration.isPlayerAfk(player);
    }

}
