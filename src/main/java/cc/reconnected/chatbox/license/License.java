package cc.reconnected.chatbox.license;

import cc.reconnected.chatbox.models.User;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class License {
    private final UUID uuid;
    private final UUID userId;
    private final Set<Capability> capabilities = new HashSet<>();
    public User user;

    public UUID uuid() {
        return uuid;
    }

    public UUID userId() {
        return userId;
    }

    public Set<Capability> capabilities() {
        return capabilities;
    }

    License(UUID uuid, UUID userId) {
        this.uuid = uuid;
        this.userId = userId;
    }
}
