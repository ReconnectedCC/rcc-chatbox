package cc.reconnected.chatbox.license;

import cc.reconnected.chatbox.Chatbox;
import cc.reconnected.server.api.PlayerMeta;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LicenseManager {
    public static final UUID guestLicenseUuid = new UUID(0, 0);
    public static final License guestLicense = new License(guestLicenseUuid, guestLicenseUuid);
    public static final String nodePrefix = "chatbox";

    public static class KEYS {
        public static final String licenseUuid = nodePrefix + ".license_uuid";
        public static final String capabilities = nodePrefix + ".capabilities";
    }

    private final Path licensesPath;
    private ConcurrentHashMap<UUID, License> licenses;
    public List<String> getLicenseList() {
        return licenses.keySet().stream().map(UUID::toString).toList();
    }

    public LicenseManager() {
        guestLicense.capabilities().clear();
        guestLicense.capabilities().add(Capability.READ);

        // Manage dedicated data for licenses.
        // Minecraft's persistent state loses data on crashes, the chatbox licenses should not depend on a stable state of the world data
        licensesPath = Chatbox.dataDirectory().resolve("licenses.json");

        if (licensesPath.toFile().exists()) {
            try (var stream = new BufferedReader(new FileReader(licensesPath.toFile(), StandardCharsets.UTF_8))) {
                var type = new TypeToken<ConcurrentHashMap<UUID, License>>() {
                }.getType();
                licenses = Chatbox.GSON.fromJson(stream, type);
            } catch (FileNotFoundException e) {
                Chatbox.LOGGER.error("If you read this I messed up", e);
            } catch (IOException e) {
                Chatbox.LOGGER.error("Exception reading licenses data", e);
            }
        } else {
            licenses = new ConcurrentHashMap<>();
        }
    }

    private void saveData() {
        var output = Chatbox.GSON.toJson(licenses);
        try (var stream = new FileWriter(licensesPath.toFile(), StandardCharsets.UTF_8)) {
            stream.write(output);
        } catch (IOException e) {
            Chatbox.LOGGER.error("Exception saving licenses data", e);
        }
    }

    private License buildLicense(String uuid, String userId, int packedCapabilities) {
        var license = new License(UUID.fromString(uuid), UUID.fromString(userId));
        license.capabilities().addAll(Capability.unpack(packedCapabilities));
        return license;
    }

    @Nullable
    public License getLicense(UUID licenseId) {
        if (licenseId.equals(guestLicenseUuid)) {
            return guestLicense;
        }

        if (licenses.containsKey(licenseId)) {
            return licenses.get(licenseId);
        }

        var serverState = Chatbox.getInstance().serverState();
        if (!serverState.licenses.containsKey(licenseId))
            return null;

        // Migrate from LP
        var ownerUuid = serverState.licenses.get(licenseId);
        var playerData = PlayerMeta.getPlayer(ownerUuid);
        var licenseUuid = playerData.get(KEYS.licenseUuid);
        var capabilitiesStr = playerData.get(KEYS.capabilities);
        int packedCapabilities = 0;
        if (capabilitiesStr != null) {
            try {
                packedCapabilities = Integer.parseInt(capabilitiesStr);
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        serverState.licenses.remove(licenseId);

        var license = buildLicense(licenseUuid, ownerUuid.toString(), packedCapabilities);
        licenses.put(license.uuid(), license);

        saveData();

        playerData.delete(KEYS.licenseUuid).join();
        playerData.delete(KEYS.capabilities).join();

        return license;
    }

    @Nullable
    public License getLicenseFromUser(UUID userId) {
        if (userId.equals(guestLicenseUuid)) {
            return guestLicense;
        }

        var license = licenses.values().stream().filter(l -> l.userId().equals(userId))
                .findFirst().orElse(null);
        if (license != null) {
            return license;
        }

        var playerData = PlayerMeta.getPlayer(userId);
        var licenseUuid = playerData.get(KEYS.licenseUuid);
        if (licenseUuid == null)
            return null;

        return getLicense(UUID.fromString(licenseUuid));
    }

    public License createLicense(UUID userId, Set<Capability> capabilities) {
        if (userId.equals(guestLicenseUuid)) {
            return guestLicense;
        }
        var license = getLicenseFromUser(userId);
        if (license != null) {
            return license;
        }

        var uuid = UUID.randomUUID();

        license = new License(uuid, userId);
        license.capabilities().addAll(capabilities);

        licenses.put(uuid, license);

        saveData();

        return license;
    }

    public boolean deleteLicense(UUID licenseId) {
        if (licenseId.equals(guestLicenseUuid)) {
            return false;
        }

        var license = getLicense(licenseId);
        if (license == null) {
            return false;
        }

        var playerData = PlayerMeta.getPlayer(license.userId());
        playerData.delete(KEYS.licenseUuid).join();
        playerData.delete(KEYS.capabilities).join();

        Chatbox.getInstance().serverState().licenses.remove(license.uuid());
        licenses.remove(license.uuid());

        saveData();

        return true;
    }

    public boolean updateLicense(UUID licenseId, Set<Capability> capabilities) {
        if (licenseId.equals(guestLicenseUuid)) {
            return false;
        }
        var license = getLicense(licenseId);
        if (license == null) {
            return false;
        }

        license.capabilities().clear();
        license.capabilities().addAll(capabilities);

        saveData();

        return true;
    }
}
