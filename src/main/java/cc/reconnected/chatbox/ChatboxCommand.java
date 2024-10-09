package cc.reconnected.chatbox;

import cc.reconnected.chatbox.data.StateSaverAndLoader;
import cc.reconnected.chatbox.license.License;
import cc.reconnected.chatbox.ws.CloseCodes;
import com.mojang.brigadier.CommandDispatcher;
import cc.reconnected.chatbox.license.Capability;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.*;

public class ChatboxCommand {
    private static final Text prefix = Text.empty()
            .append(Text.literal("[").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
            .append(Text.literal("Chatbox").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
            .append(Text.literal("]").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
            .append(" ");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        var manager = Chatbox.LicenseManager;
        dispatcher.register(
                literal("chatbox")
                        .requires(Permissions.require("chatbox.command", true))
                        .executes(context -> {
                            var commands = new String[]{
                                    "license",
                                    "license register",
                                    "license revoke",
                                    "spy"
                            };

                            var text = Text.empty()
                                    .append(prefix)
                                    .append("Manage your Chatbox license:");

                            for (var command : commands) {
                                text = text.append(Text.of("\n - "))
                                        .append(Text.literal("/chatbox " + command)
                                                .setStyle(Style.EMPTY.withColor(Formatting.BLUE).withUnderline(true)
                                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Click to suggest command")))
                                                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/chatbox " + command))
                                                )
                                        );
                            }

                            final var finalText = text;
                            context.getSource().sendFeedback(() -> finalText, false);
                            return 1;
                        })
                        .then(literal("license")
                                .executes(context -> {
                                    final var userId = context.getSource().getPlayer().getUuid();
                                    var userLicense = manager.getLicenseFromUser(userId);
                                    if (userLicense == null) {
                                        var text = Text.empty().append(prefix)
                                                .append(Text.literal("You do not have a license!"))
                                                .append(Text.literal("\n"))
                                                .append(Text
                                                        .literal("Register a new license by running ")
                                                        .append(Text.literal("/chatbox license register")
                                                                .setStyle(Style.EMPTY
                                                                        .withColor(Formatting.BLUE).withUnderline(true)
                                                                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/chatbox license register"))
                                                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Click to suggest")))
                                                                )));
                                        context.getSource().sendFeedback(() -> text, false);
                                        return 1;
                                    }

                                    var text = getLicenseRegistrationOutput(userLicense);
                                    context.getSource().sendFeedback(() -> text, false);
                                    return 1;
                                })
                                .then(literal("register")
                                        .requires(Permissions.require("chatbox.register", true))
                                        .executes(context -> {
                                            final var userId = context.getSource().getPlayer().getUuid();
                                            var userLicense = manager.getLicenseFromUser(userId);
                                            if (userLicense == null) {
                                                userLicense = manager.createLicense(userId, Capability.DEFAULT);
                                            }

                                            final var license = userLicense;

                                            context.getSource().sendFeedback(() -> getLicenseRegistrationOutput(license), false);

                                            return 1;
                                        }))
                                .then(literal("revoke")
                                        .requires(Permissions.require("chatbox.revoke", true))
                                        .executes(context -> {
                                            var userLicense = manager.getLicenseFromUser(context.getSource().getPlayer().getUuid());
                                            if (userLicense == null) {
                                                var text = Text.empty().append(prefix)
                                                        .append("You do not have a license!");

                                                context.getSource().sendFeedback(() -> text, false);
                                                return 1;
                                            }

                                            var licenseUuid = userLicense.uuid();

                                            var success = manager.deleteLicense(userLicense.uuid());
                                            if (success) {
                                                var text = Text.empty().append(prefix)
                                                        .append("Your license has been revoked!");
                                                context.getSource().sendFeedback(() -> text, false);
                                                Chatbox.getInstance().wss().closeLicenseClients(licenseUuid, CloseCodes.CHANGED_LICENSE_KEY);
                                            } else {
                                                var text = Text.empty().append(prefix)
                                                        .append("There was an error revoking your license!");
                                                context.getSource().sendFeedback(() -> text, false);
                                            }

                                            return 1;
                                        })
                                ))
                        .then(literal("spy")
                                .requires(Permissions.require("chatbox.spy", true))
                                .executes(context -> {
                                    final var player = context.getSource().getPlayer();
                                    var playerState = StateSaverAndLoader.getPlayerState(player);
                                    playerState.enableSpy = !playerState.enableSpy;
                                    if (playerState.enableSpy) {
                                        context.getSource().sendFeedback(() -> Text.empty().append(prefix).append("You are now spying chatbox commands!"), true);
                                    } else {
                                        context.getSource().sendFeedback(() -> Text.empty().append(prefix).append("You are no longer spying chatbox commands!"), true);
                                    }
                                    return 1;
                                })
                        )

        );
    }

    private static MutableText getLicenseRegistrationOutput(License userLicense) {
        var licenseKey = userLicense.uuid();
        return Text.empty().append(prefix)
                .append(Text.literal("Your license is \"")
                        .append(Text.literal(licenseKey.toString())
                                .setStyle(Style.EMPTY
                                        .withColor(Formatting.GRAY)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, licenseKey.toString()))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Click to copy")))
                                )
                        )
                )
                .append(Text.of("\"\n\n"))
                .append(Text.literal("Run \"")
                        .append(Text.literal("chatbox register " + licenseKey)
                                .setStyle(Style.EMPTY
                                        .withColor(Formatting.GRAY)
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Click to copy")))
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, licenseKey.toString()))
                                )
                        )
                        .append(Text.of("\" "))
                        .append(Text.literal("on a computer to register the license!"))
                );
    }
}
