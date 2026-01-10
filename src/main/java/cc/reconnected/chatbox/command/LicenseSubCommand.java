package cc.reconnected.chatbox.command;

import cc.reconnected.chatbox.RccChatbox;
import cc.reconnected.chatbox.license.Capability;
import cc.reconnected.chatbox.license.License;
import cc.reconnected.chatbox.ws.CloseCodes;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.*;

import static net.minecraft.commands.Commands.literal;

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
            capabilitiesComponent = capabilitiesComponent.append(Component.literal(cap.name()).withStyle(ChatFormatting.GOLD));

            // is last element
            if (i < capabilities.length - 1) {
                capabilitiesComponent = capabilitiesComponent.append(Component.literal(", "));
            }
        }
        capabilitiesComponent = capabilitiesComponent.append(Component.literal("."));


        MutableComponent output = Component.empty().append(ChatboxCommand.prefix);

        if (isNew) {
            output = output
                    .append(Component.literal("Your license has been created!").withStyle(ChatFormatting.GREEN))
                    .append("\n").append("\n");
        }

        output = output
                .append(Component.literal("Your chatbox license key is:"))
                .append("\n")
                .append(Component.literal("  "))
                .append(Component.literal(license.uuid().toString())
                        .withStyle(Style.EMPTY.applyFormat(ChatFormatting.AQUA)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,Component.literal("Click to copy")))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, license.uuid().toString())))
                )
                .append("\n").append("\n")
                .append(Component.literal("Register the key in a computer by running:"))
                .append("\n")
                .append(Component.literal("  "))
                .append(Component.literal("chatbox register " + license.uuid().toString())
                        .withStyle(Style.EMPTY.applyFormat(ChatFormatting.AQUA)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,Component.literal("Click to copy")))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "chatbox register " + license.uuid().toString())))
                )
                .append("\n").append("\n")
                .append(Component.literal("Your license capabilities are: "))
                .append(capabilitiesComponent);

        return output;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register(CommandDispatcher<CommandSourceStack> dispatcher,
                                                                       CommandBuildContext registryAccess,
                                                                       Commands.CommandSelection environment) {
        return literal("license")
                .executes(context -> {
                    if(!context.getSource().isPlayer()) {
                        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("This command can only be executed by players!"), false);
                        return 0;
                    }
                    var manager = RccChatbox.licenseManager();
                    final var userId = context.getSource().getPlayer().getUUID();
                    var userLicense = manager.getLicenseFromUser(userId);
                    if (userLicense == null) {
                        var text = Component.empty()
                                .append(Component.literal("You currently do not have a license!").withStyle(ChatFormatting.RED))
                                .append("\n").append("\n")
                                .append(Component.literal("Register a new license by running:"))
                                .append("\n")
                                .append(Component.literal("  "))
                                .append(Component.literal("/chatbox license register")
                                        .withStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)
                                            .withUnderlined(true)
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to suggest")))
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "/chatbox license register ")))
                                );
                        context.getSource().sendSuccess(() ->text,false);
                        return 1;
                    }

                    context.getSource().sendSuccess(() ->getLicenseRegistrationOutput(userLicense, false),false);
                    return 1;
                })
                .then(literal("register")
                        .requires(Permissions.require("chatbox.register", true))
                        .executes(context -> {
                            if(!context.getSource().isPlayer()) {
                                context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("This command can only be executed by players!"), false);
                                return 0;
                            }
                            var manager = RccChatbox.licenseManager();
                            final var userId = context.getSource().getPlayer().getUUID();
                            var userLicense = manager.getLicenseFromUser(userId);
                            var createNew = userLicense == null;
                            if (createNew) {
                                userLicense = manager.createLicense(userId, Capability.DEFAULT);
                            }

                            context.getSource().sendSystemMessage(getLicenseRegistrationOutput(userLicense, createNew));

                            return 1;
                        }))
                .then(literal("revoke")
                        .requires(Permissions.require("chatbox.revoke", true))
                        .executes(context -> {
                            if(!context.getSource().isPlayer()) {
                                context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("This command can only be executed by players!"), false);
                                return 0;
                            }
                            var manager = RccChatbox.licenseManager();
                            var userLicense = manager.getLicenseFromUser(context.getSource().getPlayer().getUUID());
                            if (userLicense == null) {
                                var text = Component.empty().append(ChatboxCommand.prefix)
                                        .append(Component.literal("You already do not have a license!").withStyle(ChatFormatting.RED));

                                context.getSource().sendFailure(text);
                                return 1;
                            }

                            var licenseUuid = userLicense.uuid();

                            var success = manager.deleteLicense(userLicense.uuid());
                            if (success) {
                                var text = Component.empty().append(ChatboxCommand.prefix)
                                        .append(Component.literal("Your license has been revoked!").withStyle(ChatFormatting.GREEN));
                                context.getSource().sendSystemMessage(text);
                                RccChatbox.getInstance().wss().closeLicenseClients(licenseUuid, CloseCodes.CHANGED_LICENSE_KEY);
                            } else {
                                var text = Component.empty().append(ChatboxCommand.prefix)
                                        .append(Component.literal("There was an error revoking your license!").withStyle(ChatFormatting.RED));
                                context.getSource().sendSystemMessage(text);
                            }

                            return 1;
                        })
                );
    }
}
