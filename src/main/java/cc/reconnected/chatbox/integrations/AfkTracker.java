package cc.reconnected.chatbox.integrations;

import cc.reconnected.chatbox.RccChatbox;

import java.util.UUID;

public class AfkTracker {
    public static boolean isPlayerAfk(UUID uuid) {
        if (!RccChatbox.isSolsticeLoaded())
            return false;

        return SolsticeIntegration.isPlayerAfk(uuid);
    }

}
