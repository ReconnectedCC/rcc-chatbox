package cc.reconnected.chatbox.license;

import cc.reconnected.chatbox.Chatbox;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LicenseManager {
    public static final UUID guestLicenseUuid = new UUID(0, 0);
    public static final License guestLicense = new License(guestLicenseUuid, guestLicenseUuid);

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

        try {
            var conn = Chatbox.getInstance().database().connection();
            var stmt = conn.prepareStatement("SELECT * FROM chatbox_licenses WHERE uuid = ?");
            stmt.setObject(1, licenseId);
            var rs = stmt.executeQuery();
            if(!rs.next()) {
                return null;
            }

            var uuid = rs.getString(1);
            var userId = rs.getString(2);
            var packedCapabilities = rs.getInt(3);

            var license = buildLicense(uuid, userId, packedCapabilities);
            cache.put(license.uuid(), license);

            stmt.close();
            return license;
        } catch(SQLException e) {
            Chatbox.LOGGER.error("Could not fetch license", e);
            return null;
        }
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

        try{
            var conn = Chatbox.getInstance().database().connection();
            var stmt = conn.prepareStatement("SELECT * FROM chatbox_licenses WHERE userId = ?");
            stmt.setObject(1, userId);
            var rs = stmt.executeQuery();
            if(!rs.next()) {
                return null;
            }

            var uuid = rs.getString(1);
            var dbUserId = rs.getString(2);
            var packedCapabilities = rs.getInt(3);

            license = buildLicense(uuid, dbUserId, packedCapabilities);
            cache.put(license.uuid(), license);

            stmt.close();
            return license;
        } catch(SQLException e) {
            Chatbox.LOGGER.error("Could not fetch license", e);
            return null;
        }
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

        try {
            var conn = Chatbox.getInstance().database().connection();
            var stmt = conn.prepareStatement("INSERT INTO chatbox_licenses(uuid, userId, capabilities) VALUES (?, ?, ?);");
            stmt.setObject(1, uuid);
            stmt.setObject(2, userId);
            stmt.setInt(3, Capability.pack(capabilities));
            stmt.execute();
            stmt.close();

            cache.put(uuid, license);
            return license;
        } catch (SQLException e) {
            Chatbox.LOGGER.error("Could not create license", e);
            return null;
        }
    }

    public boolean deleteLicense(UUID licenseId) {
        if(licenseId.equals(guestLicenseUuid)) {
            return false;
        }
        try {
            var conn = Chatbox.getInstance().database().connection();
            var stmt = conn.prepareStatement("DELETE FROM chatbox_licenses WHERE uuid = ?");
            stmt.setObject(1, licenseId);
            stmt.execute();
            stmt.close();

            cache.remove(licenseId);
            return true;
        } catch (SQLException e) {
            Chatbox.LOGGER.error("Could not delete license", e);
            return false;
        }
    }

    public boolean updateLicense(UUID licenseId, Set<Capability> capabilities) {
        if(licenseId.equals(guestLicenseUuid)) {
            return false;
        }
        try {
            var conn = Chatbox.getInstance().database().connection();
            var stmt = conn.prepareStatement("UPDATE chatbox_licenses SET capabilities = ? WHERE uuid = ?");
            stmt.setInt(1, Capability.pack(capabilities));
            stmt.setObject(2, licenseId);
            stmt.execute();
            stmt.close();

            if(cache.containsKey(licenseId)) {
                var license = cache.get(licenseId);
                license.capabilities().clear();
                license.capabilities().addAll(capabilities);
            }

            return true;
        } catch (SQLException e) {
            Chatbox.LOGGER.error("Could not update license", e);
            return false;
        }
    }
}
