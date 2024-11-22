package cc.reconnected.chatbox.command;

import cc.reconnected.chatbox.RccChatbox;
import cc.reconnected.chatbox.license.Capability;
import cc.reconnected.chatbox.license.License;
import cc.reconnected.chatbox.ws.CloseCodes;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static cc.reconnected.chatbox.command.ChatboxCommand.prefix;
import static net.minecraft.server.command.CommandManager.*;

public class AdminSubCommand {
    private static @Nullable License getLicenseFromArgument(String id, PlayerManager playerManager) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);

        } catch (IllegalArgumentException e) {
            var player = playerManager.getPlayer(id);
            if (player == null) {
                return null;
            }
            uuid = player.getGameProfile().getId();
        }

        var license = RccChatbox.licenseManager().getLicense(uuid);
        if (license == null) {
            license = RccChatbox.licenseManager().getLicenseFromUser(uuid);
        }

        return license;
    }

    public static LiteralArgumentBuilder<ServerCommandSource> register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        return literal("admin")
                .requires(Permissions.require("chatbox.admin", 3))
                .executes(context -> {
                    var commands = new String[]{
                            "<user/license>",
                            "<user/license> revoke",
                            //"<user/license> disable",
                            //"<user/license> enable",
                            "<user/license> capabilities",
                            "<user/license> capabilities enable <capability>",
                            "<user/license> capabilities disable <capability>",
                    };

                    final var text =
                            Text.empty()
                                    .append(prefix)
                                    .append("Chatbox admin commands:")
                                    .append(ChatboxCommand.buildHelpMessage("chatbox admin", commands));

                    context.getSource().sendFeedback(() -> text, false);
                    return 1;
                })
                .then(argument("user/license", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            var playerManager = context.getSource().getServer().getPlayerManager();
                            var list = new ArrayList<String>();
                            list.addAll(playerManager.getPlayerList()
                                    .stream()
                                    .map(player -> player.getGameProfile().getName())
                                    .toList());
                            list.addAll(RccChatbox.licenseManager().getLicenseList());
                            return CommandSource.suggestMatching(
                                    list,
                                    builder
                            );
                        })
                        .executes(context -> {
                            var id = StringArgumentType.getString(context, "user/license");
                            var playerManager = context.getSource().getServer().getPlayerManager();

                            var license = getLicenseFromArgument(id, playerManager);
                            if (license == null) {
                                context.getSource().sendFeedback(() -> Text.empty().append(prefix).append(Text.literal("Player or license not found").setStyle(Style.EMPTY.withColor(Formatting.RED))), false);
                                return 1;
                            }

                            String playerName = license.userId().toString();

                            ServerPlayerEntity player = playerManager.getPlayer(license.userId());
                            if (player == null) {
                                player = playerManager.getPlayer(license.userId());
                            }
                            if (player != null) {
                                playerName = player.getGameProfile().getName();
                            }

                            var text = Text.empty()
                                    .append(prefix)
                                    .append(Text.literal("This license belongs to ")
                                            .append(Text.literal(playerName)));

                            context.getSource().sendFeedback(() -> text, false);

                            return 1;
                        })
                        .then(literal("revoke")
                                .executes(context -> {
                                    var id = StringArgumentType.getString(context, "user/license");
                                    var playerManager = context.getSource().getServer().getPlayerManager();
                                    var license = getLicenseFromArgument(id, playerManager);
                                    if (license == null) {
                                        context.getSource().sendFeedback(() -> Text.empty().append(prefix).append(Text.literal("Player or license not found").setStyle(Style.EMPTY.withColor(Formatting.RED))), false);
                                        return 1;
                                    }

                                    String playerName = license.userId().toString();
                                    ServerPlayerEntity player = playerManager.getPlayer(license.userId());
                                    if (player == null) {
                                        player = playerManager.getPlayer(license.userId());
                                    }
                                    if (player != null) {
                                        playerName = player.getGameProfile().getName();
                                    }

                                    RccChatbox.licenseManager().deleteLicense(license.uuid());
                                    RccChatbox.getInstance().wss().closeLicenseClients(license.uuid(), CloseCodes.CHANGED_LICENSE_KEY);

                                    final var finalPlayerName = playerName;
                                    context.getSource().sendFeedback(() -> Text.empty().append(prefix).append(Text.literal("Revoked " + finalPlayerName + " license!").setStyle(Style.EMPTY.withColor(Formatting.GREEN))), true);

                                    return 1;
                                })
                        )
                        .then(literal("capabilities")
                                .executes(context -> {
                                    var id = StringArgumentType.getString(context, "user/license");
                                    var playerManager = context.getSource().getServer().getPlayerManager();
                                    var license = getLicenseFromArgument(id, playerManager);
                                    if (license == null) {
                                        context.getSource().sendFeedback(() -> Text.empty().append(prefix).append(Text.literal("Player or license not found").setStyle(Style.EMPTY.withColor(Formatting.RED))), false);
                                        return 1;
                                    }

                                    var text = Text.empty().append(prefix).append("License capabilities: ");
                                    license.capabilities().forEach(c -> {
                                        text.append(Text.of(c.name())).append(Text.of(";"));
                                    });
                                    context.getSource().sendFeedback(() -> text, false);
                                    return 1;
                                })
                                .then(argument("capability", StringArgumentType.word())
                                        .suggests((context, builder) ->
                                                CommandSource.suggestMatching(Arrays.stream(Capability.values()).map(Enum::name), builder)
                                        )
                                        .then(argument("toggle", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    var capabilityName = StringArgumentType.getString(context, "capability");
                                                    var toggle = BoolArgumentType.getBool(context, "toggle");
                                                    var id = StringArgumentType.getString(context, "user/license");
                                                    var playerManager = context.getSource().getServer().getPlayerManager();
                                                    var license = getLicenseFromArgument(id, playerManager);
                                                    if (license == null) {
                                                        context.getSource().sendFeedback(() -> Text.empty().append(prefix).append(Text.literal("Player or license not found").setStyle(Style.EMPTY.withColor(Formatting.RED))), false);
                                                        return 1;
                                                    }

                                                    String playerName = license.userId().toString();
                                                    ServerPlayerEntity player = playerManager.getPlayer(license.userId());
                                                    if (player == null) {
                                                        player = playerManager.getPlayer(license.userId());
                                                    }
                                                    if (player != null) {
                                                        playerName = player.getGameProfile().getName();
                                                    }

                                                    var licenseManager = RccChatbox.licenseManager();

                                                    var capability = Capability.valueOf(capabilityName);

                                                    Text text;
                                                    var capabilities = new HashSet<>(license.capabilities());
                                                    if (toggle) {
                                                        capabilities.add(capability);
                                                        text = Text.literal("Granted '" + capability + "' to " + playerName).setStyle(Style.EMPTY.withColor(Formatting.GREEN));
                                                    } else {
                                                        capabilities.remove(capability);
                                                        text = Text.literal("Revoked '" + capability + "' from " + playerName).setStyle(Style.EMPTY.withColor(Formatting.RED));
                                                    }
                                                    // save to file
                                                    licenseManager.updateLicense(license.uuid(), capabilities);

                                                    final var finalText = Text.empty().append(prefix).append(text);
                                                    context.getSource().sendFeedback(() -> finalText, true);

                                                    return 1;
                                                })))
                        )

                );
    }
}
