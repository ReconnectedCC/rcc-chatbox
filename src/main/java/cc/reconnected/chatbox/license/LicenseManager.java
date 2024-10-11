package cc.reconnected.chatbox.license;

import cc.reconnected.chatbox.Chatbox;
import cc.reconnected.server.RccServer;
import cc.reconnected.server.database.PlayerData;
import com.sun.jdi.connect.spi.TransportService;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LicenseManager {
    public static final UUID guestLicenseUuid = new UUID(0, 0);
    public static final License guestLicense = new License(guestLicenseUuid, guestLicenseUuid);
    public static final String nodePrefix = "chatbox";
    public static class KEYS {
        public static final String licenseUuid = nodePrefix + ".license_uuid";
        public static final String capabilities = nodePrefix + ".capabilities";
    }

    private final ConcurrentHashMap<UUID, License> cache = new ConcurrentHashMap<>();

    public LicenseManager() {
        guestLicense.capabilities().clear();
        guestLicense.capabilities().add(Capability.READ);
        cache.put(guestLicenseUuid, guestLicense);
    }

    private License buildLicense(String uuid, String userId, int packedCapabilities) {
        var license = new License(UUID.fromString(uuid), UUID.fromString(userId));
        license.capabilities().addAll(Capability.unpack(packedCapabilities));
        return license;
    }

    @Nullable
    public License getLicense(UUID licenseId) {
        if(licenseId.equals(guestLicenseUuid)) {
            return guestLicense;
        }
        if (cache.containsKey(licenseId)) {
            return cache.get(licenseId);
        }

        var serverState = Chatbox.getInstance().serverState();
        if(!serverState.licenses.containsKey(licenseId))
            return null;

        var ownerUuid = serverState.licenses.get(licenseId);
        var playerData = PlayerData.getPlayer(ownerUuid);
        var licenseUuid = playerData.get(KEYS.licenseUuid);
        var capabilitiesStr = playerData.get(KEYS.capabilities);
        int packedCapabilities = 0;
        if(capabilitiesStr != null) {
            try {
                packedCapabilities = Integer.parseInt(capabilitiesStr);
            } catch(NumberFormatException e) {
                // do nothing
            }
        }

        var license = buildLicense(licenseUuid, ownerUuid.toString(), packedCapabilities);
        cache.put(license.uuid(), license);

        return license;
    }

    @Nullable
    public License getLicenseFromUser(UUID userId) {
        if(userId.equals(guestLicenseUuid)) {
            return guestLicense;
        }
        var license = cache.values().parallelStream().filter(l -> l.userId() == userId).findFirst().orElse(null);
        if (license != null) {
            return license;
        }

        var playerData = PlayerData.getPlayer(userId);
        var licenseUuid = playerData.get(KEYS.licenseUuid);
        if(licenseUuid == null)
            return null;
        var capabilitiesStr = playerData.get(KEYS.capabilities);
        int packedCapabilities = 0;
        if(capabilitiesStr != null) {
            try {
                packedCapabilities = Integer.parseInt(capabilitiesStr);
            } catch(NumberFormatException e) {
                // do nothing
            }
        }

        license = buildLicense(licenseUuid, userId.toString(), packedCapabilities);
        cache.put(license.uuid(), license);
        return license;
    }

    public License createLicense(UUID userId, Set<Capability> capabilities) {
        if(userId.equals(guestLicenseUuid)) {
            return guestLicense;
        }
        var license = getLicenseFromUser(userId);
        if (license != null) {
            return license;
        }

        var uuid = UUID.randomUUID();

        license = new License(uuid, userId);
        license.capabilities().addAll(capabilities);

        var playerData = PlayerData.getPlayer(userId);
        playerData.set(KEYS.licenseUuid, uuid.toString());
        playerData.set(KEYS.capabilities, String.valueOf(Capability.pack(capabilities)));
        Chatbox.getInstance().serverState().licenses.put(uuid, userId);

        cache.put(uuid, license);

        return license;
    }

    public boolean deleteLicense(UUID licenseId) {
        if(licenseId.equals(guestLicenseUuid)) {
            return false;
        }

        var license = getLicense(licenseId);
        if(license == null) {
            return false;
        }

        var playerData = PlayerData.getPlayer(license.userId());
        playerData.delete(KEYS.licenseUuid);
        playerData.delete(KEYS.capabilities);

        Chatbox.getInstance().serverState().licenses.remove(license.uuid());
        cache.remove(license.uuid());
        return true;
    }

    public boolean updateLicense(UUID licenseId, Set<Capability> capabilities) {
        if(licenseId.equals(guestLicenseUuid)) {
            return false;
        }
        var license = getLicense(licenseId);
        if(license == null) {
            return false;
        }

        license.capabilities().clear();
        license.capabilities().addAll(capabilities);
        var playerData = PlayerData.getPlayer(license.userId());

        playerData.set(KEYS.capabilities, String.valueOf(Capability.pack(capabilities)));
        cache.put(license.uuid(), license);

        return true;
    }

    public void clearCache(UUID licenseUuid) {
        cache.remove(licenseUuid);
    }
}
