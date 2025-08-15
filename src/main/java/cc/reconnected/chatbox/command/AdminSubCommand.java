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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static cc.reconnected.chatbox.command.ChatboxCommand.prefix;
import static net.minecraft.commands.Commands.*;

public class AdminSubCommand {
    private static @Nullable License getLicenseFromArgument(String id, PlayerList playerManager) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);

        } catch (IllegalArgumentException e) {
            var player = playerManager.getPlayerByName(id);
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

    public static LiteralArgumentBuilder<CommandSourceStack> register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        return literal("admin")
                .requires(Permissions.require("chatbox.admin", 3))
                .executes(context -> {
                    var commands = new String[]{
                            "<user/license>",
                            "<user/license> revoke",
                            //"<user/license> disable",
                            //"<user/license> enable",
                            "<user/license> capabilities",
                            "<user/license> capabilities <capability> <true/false>",
                    };

                    final var text =
                            Component.empty()
                                    .append(prefix)
                                    .append("Chatbox admin commands:")
                                    .append(ChatboxCommand.buildHelpMessage("chatbox admin", commands));

                    context.getSource().sendSuccess(() -> text, false);
                    return 1;
                })
                .then(argument("user/license", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            var playerManager = context.getSource().getServer().getPlayerList();
                            var list = new ArrayList<String>();
                            list.addAll(playerManager.getPlayers()
                                    .stream()
                                    .map(player -> player.getGameProfile().getName())
                                    .toList());
                            list.addAll(RccChatbox.licenseManager().getLicenseList());
                            return SharedSuggestionProvider.suggest(
                                    list,
                                    builder
                            );
                        })
                        .executes(context -> {
                            var id = StringArgumentType.getString(context, "user/license");
                            var playerManager = context.getSource().getServer().getPlayerList();

                            var license = getLicenseFromArgument(id, playerManager);
                            if (license == null) {
                                context.getSource().sendSuccess(() -> Component.empty().append(prefix).append(Component.literal("Player or license not found").setStyle(Style.EMPTY.withColor(ChatFormatting.RED))), false);
                                return 1;
                            }

                            String playerName = license.userId().toString();

                            ServerPlayer player = playerManager.getPlayer(license.userId());
                            if (player == null) {
                                player = playerManager.getPlayer(license.userId());
                            }
                            if (player != null) {
                                playerName = player.getGameProfile().getName();
                            }

                            var text = Component.empty()
                                    .append(prefix)
                                    .append(Component.literal("This license belongs to ")
                                            .append(Component.literal(playerName)));

                            context.getSource().sendSuccess(() -> text, false);

                            return 1;
                        })
                        .then(literal("revoke")
                                .executes(context -> {
                                    var id = StringArgumentType.getString(context, "user/license");
                                    var playerManager = context.getSource().getServer().getPlayerList();
                                    var license = getLicenseFromArgument(id, playerManager);
                                    if (license == null) {
                                        context.getSource().sendSuccess(() -> Component.empty().append(prefix).append(Component.literal("Player or license not found").setStyle(Style.EMPTY.withColor(ChatFormatting.RED))), false);
                                        return 1;
                                    }

                                    String playerName = license.userId().toString();
                                    ServerPlayer player = playerManager.getPlayer(license.userId());
                                    if (player == null) {
                                        player = playerManager.getPlayer(license.userId());
                                    }
                                    if (player != null) {
                                        playerName = player.getGameProfile().getName();
                                    }

                                    RccChatbox.licenseManager().deleteLicense(license.uuid());
                                    RccChatbox.getInstance().wss().closeLicenseClients(license.uuid(), CloseCodes.CHANGED_LICENSE_KEY);

                                    final var finalPlayerName = playerName;
                                    context.getSource().sendSuccess(() -> Component.empty().append(prefix).append(Component.literal("Revoked " + finalPlayerName + " license!").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN))), true);

                                    return 1;
                                })
                        )
                        .then(literal("capabilities")
                                .executes(context -> {
                                    var id = StringArgumentType.getString(context, "user/license");
                                    var playerManager = context.getSource().getServer().getPlayerList();
                                    var license = getLicenseFromArgument(id, playerManager);
                                    if (license == null) {
                                        context.getSource().sendSuccess(() -> Component.empty().append(prefix).append(Component.literal("Player or license not found").setStyle(Style.EMPTY.withColor(ChatFormatting.RED))), false);
                                        return 1;
                                    }

                                    var text = Component.empty().append(prefix).append("License capabilities: ");
                                    license.capabilities().forEach(c -> {
                                        text.append(Component.nullToEmpty(c.name())).append(Component.nullToEmpty(";"));
                                    });
                                    context.getSource().sendSuccess(() -> text, false);
                                    return 1;
                                })
                                .then(argument("capability", StringArgumentType.word())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(Arrays.stream(Capability.values()).map(Enum::name), builder)
                                        )
                                        .then(argument("toggle", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    var capabilityName = StringArgumentType.getString(context, "capability");
                                                    var toggle = BoolArgumentType.getBool(context, "toggle");
                                                    var id = StringArgumentType.getString(context, "user/license");
                                                    var playerManager = context.getSource().getServer().getPlayerList();
                                                    var license = getLicenseFromArgument(id, playerManager);
                                                    if (license == null) {
                                                        context.getSource().sendSuccess(() -> Component.empty().append(prefix).append(Component.literal("Player or license not found").setStyle(Style.EMPTY.withColor(ChatFormatting.RED))), false);
                                                        return 1;
                                                    }

                                                    String playerName = license.userId().toString();
                                                    ServerPlayer player = playerManager.getPlayer(license.userId());
                                                    if (player == null) {
                                                        player = playerManager.getPlayer(license.userId());
                                                    }
                                                    if (player != null) {
                                                        playerName = player.getGameProfile().getName();
                                                    }

                                                    var licenseManager = RccChatbox.licenseManager();

                                                    var capability = Capability.valueOf(capabilityName);

                                                    Component text;
                                                    var capabilities = new HashSet<>(license.capabilities());
                                                    if (toggle) {
                                                        capabilities.add(capability);
                                                        text = Component.literal("Granted '" + capability + "' to " + playerName).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
                                                    } else {
                                                        capabilities.remove(capability);
                                                        text = Component.literal("Revoked '" + capability + "' from " + playerName).setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
                                                    }
                                                    // save to file
                                                    licenseManager.updateLicense(license.uuid(), capabilities);

                                                    final var finalText = Component.empty().append(prefix).append(text);
                                                    context.getSource().sendSuccess(() -> finalText, true);

                                                    return 1;
                                                })))
                        )

                );
    }
}
