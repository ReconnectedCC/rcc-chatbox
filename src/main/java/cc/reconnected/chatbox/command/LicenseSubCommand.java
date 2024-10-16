package cc.reconnected.chatbox.command;

import cc.reconnected.chatbox.Chatbox;
import cc.reconnected.chatbox.license.Capability;
import cc.reconnected.chatbox.license.License;
import cc.reconnected.chatbox.ws.CloseCodes;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class LicenseSubCommand {
    /*
     * <green>Your license has been created!</green> // only when new
     *
     * Your chatbox license key is:
     *   <aqua>XXXXXX</aqua>
     *
     * Register the key in a computer by running:
     *   <aqua>chatbox register XXXXXX</aqua>
     *
     * Your license capabilities are: <green>read</green>, <green>command</green>, <green>tell</green>.
     */

    private static Component getLicenseRegistrationOutput(License license, boolean isNew) {
        var capabilitiesComponent = Component.empty();
        var capabilities = license.capabilities().toArray(new Capability[0]);
        for (int i = 0; i < capabilities.length; i++) {
            var cap = capabilities[i];
            capabilitiesComponent = capabilitiesComponent.append(Component.text(cap.name()).color(NamedTextColor.GOLD));

            // is last element
            if (i < capabilities.length - 1) {
                capabilitiesComponent = capabilitiesComponent.append(Component.text(", "));
            }
        }
        capabilitiesComponent = capabilitiesComponent.append(Component.text("."));


        Component output = Component.empty().append(ChatboxCommand.prefix);

        if (isNew) {
            output = output
                    .append(Component.text("Your license has been created!").color(NamedTextColor.GREEN))
                    .appendNewline().appendNewline();
        }

        output = output
                .append(Component.text("Your chatbox license key is:"))
                .appendNewline()
                .append(Component.text("  "))
                .append(Component.text(license.uuid().toString())
                        .style(Style.style(NamedTextColor.AQUA)
                                .hoverEvent(HoverEvent.showText(Component.text("Click to copy")))
                                .clickEvent(ClickEvent.copyToClipboard(license.uuid().toString())))
                )
                .appendNewline().appendNewline()
                .append(Component.text("Register the key in a computer by running:"))
                .appendNewline()
                .append(Component.text("  "))
                .append(Component.text("chatbox register " + license.uuid().toString())
                        .style(Style.style(NamedTextColor.AQUA)
                                .hoverEvent(HoverEvent.showText(Component.text("Click to copy")))
                                .clickEvent(ClickEvent.copyToClipboard("chatbox register " + license.uuid().toString())))
                )
                .appendNewline().appendNewline()
                .append(Component.text("Your license capabilities are: "))
                .append(capabilitiesComponent);

        return output;
    }

    public static LiteralArgumentBuilder<ServerCommandSource> register(CommandDispatcher<ServerCommandSource> dispatcher,
                                                                       CommandRegistryAccess registryAccess,
                                                                       CommandManager.RegistrationEnvironment environment) {
        return literal("license")
                .executes(context -> {
                    if(!context.getSource().isExecutedByPlayer()) {
                        context.getSource().sendFeedback(() -> Text.literal("This command can only be executed by players!"), false);
                        return 0;
                    }
                    var manager = Chatbox.licenseManager();
                    final var userId = context.getSource().getPlayer().getUuid();
                    var userLicense = manager.getLicenseFromUser(userId);
                    if (userLicense == null) {
                        var text = Component.empty()
                                .append(Component.text("You currently do not have a license!").color(NamedTextColor.RED))
                                .appendNewline().appendNewline()
                                .append(Component.text("Register a new license by running:"))
                                .appendNewline()
                                .append(Component.text("  "))
                                .append(Component.text("/chatbox license register")
                                        .color(NamedTextColor.BLUE).decorate(TextDecoration.UNDERLINED)
                                        .hoverEvent(HoverEvent.showText(Component.text("Click to suggest")))
                                        .clickEvent(ClickEvent.copyToClipboard("/chatbox license register "))
                                );
                        context.getSource().sendMessage(text);
                        return 1;
                    }

                    context.getSource().sendMessage(getLicenseRegistrationOutput(userLicense, false));
                    return 1;
                })
                .then(literal("register")
                        .requires(Permissions.require("chatbox.register", true))
                        .executes(context -> {
                            if(!context.getSource().isExecutedByPlayer()) {
                                context.getSource().sendFeedback(() -> Text.literal("This command can only be executed by players!"), false);
                                return 0;
                            }
                            var manager = Chatbox.licenseManager();
                            final var userId = context.getSource().getPlayer().getUuid();
                            var userLicense = manager.getLicenseFromUser(userId);
                            var createNew = userLicense == null;
                            if (createNew) {
                                userLicense = manager.createLicense(userId, Capability.DEFAULT);
                            }

                            context.getSource().sendMessage(getLicenseRegistrationOutput(userLicense, createNew));

                            return 1;
                        }))
                .then(literal("revoke")
                        .requires(Permissions.require("chatbox.revoke", true))
                        .executes(context -> {
                            if(!context.getSource().isExecutedByPlayer()) {
                                context.getSource().sendFeedback(() -> Text.literal("This command can only be executed by players!"), false);
                                return 0;
                            }
                            var manager = Chatbox.licenseManager();
                            var userLicense = manager.getLicenseFromUser(context.getSource().getPlayer().getUuid());
                            if (userLicense == null) {
                                var text = Component.empty().append(ChatboxCommand.prefix)
                                        .append(Component.text("You already do not have a license!").color(NamedTextColor.RED));

                                context.getSource().sendMessage(text);
                                return 1;
                            }

                            var licenseUuid = userLicense.uuid();

                            var success = manager.deleteLicense(userLicense.uuid());
                            if (success) {
                                var text = Component.empty().append(ChatboxCommand.prefix)
                                        .append(Component.text("Your license has been revoked!").color(NamedTextColor.GREEN));
                                context.getSource().sendMessage(text);
                                Chatbox.getInstance().wss().closeLicenseClients(licenseUuid, CloseCodes.CHANGED_LICENSE_KEY);
                            } else {
                                var text = Component.empty().append(ChatboxCommand.prefix)
                                        .append(Component.text("There was an error revoking your license!").color(NamedTextColor.RED));
                                context.getSource().sendMessage(text);
                            }

                            return 1;
                        })
                );
    }
}
