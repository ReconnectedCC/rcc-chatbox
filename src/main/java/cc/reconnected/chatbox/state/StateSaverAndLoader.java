package cc.reconnected.chatbox.state;

import cc.reconnected.chatbox.RccChatbox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.UUID;

public class StateSaverAndLoader extends SavedData {
    public final HashMap<UUID, ChatboxPlayerState> players = new HashMap<>();

    /**
     * LicenseUUID -> PlayerUUID
     * <p>
     * Contains mappings of license tokens UUID to players UUID
     */
    public final HashMap<UUID, UUID> licenses = new HashMap<>();

    @Override
    public CompoundTag save(CompoundTag nbt) {
        var playersNbt = new CompoundTag();
        players.forEach((uuid, data) -> {
            var playerNbt = new CompoundTag();
            playerNbt.putBoolean("spy", data.enableSpy);
            playersNbt.put(uuid.toString(), playerNbt);
        });
        nbt.put("players", playersNbt);

        var licensesNbt = new CompoundTag();
        licenses.forEach((license, playerId) -> licensesNbt.putUUID(license.toString(), playerId));
        nbt.put("licenses", licensesNbt);
        return nbt;
    }

    public static StateSaverAndLoader createFromNbt(CompoundTag nbt) {
        var state = new StateSaverAndLoader();

        var playersNbt = nbt.getCompound("players");
        playersNbt.getAllKeys().forEach(key -> {
            var playerData = new ChatboxPlayerState();

            playerData.enableSpy = playersNbt.getCompound(key).getBoolean("spy");
            UUID uuid = UUID.fromString(key);
            state.players.put(uuid, playerData);
        });

        var licensesNbt = nbt.getCompound("licenses");
        licensesNbt.getAllKeys().forEach(license -> {
            var playerUuid = licensesNbt.getUUID(license);
            state.licenses.put(UUID.fromString(license), playerUuid);
        });
        return state;
    }

    public static StateSaverAndLoader getServerState(MinecraftServer server) {
        var persistentStateManager = server.getLevel(Level.OVERWORLD).getDataStorage();
        var state = persistentStateManager.computeIfAbsent(
                StateSaverAndLoader::createFromNbt,
                StateSaverAndLoader::new,
                RccChatbox.MOD_ID
        );
        state.setDirty();
        return state;
    }

    public static ChatboxPlayerState getPlayerState(LivingEntity player) {
        var serverState = getServerState(player.level().getServer());
        return serverState.players.computeIfAbsent(player.getUUID(), uuid -> new ChatboxPlayerState());
    }
}
