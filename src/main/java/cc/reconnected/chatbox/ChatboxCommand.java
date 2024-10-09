package cc.reconnected.chatbox;

import cc.reconnected.chatbox.data.StateSaverAndLoader;
import com.mojang.brigadier.CommandDispatcher;
import cc.reconnected.chatbox.license.Capability;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

public class ChatboxCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        var manager = Chatbox.LicenseManager;
        dispatcher.register(
                literal("chatbox")
                        .requires(Permissions.require("chatbox.command", true))
                        .executes(context -> {
                            return 1;
                        })
                        .then(literal("help"))
                        .then(literal("license")
                                .executes(context -> {
                                    return 1;
                                })
                                .then(literal("register")
                                        .requires(Permissions.require("chatbox.register", true))
                                        .executes(context -> {
                                            final var userId = context.getSource().getPlayer().getUuid();
                                            var userLicense = manager.getLicenseFromUser(userId);
                                            if (userLicense != null) {
                                                context.getSource().sendMessage(Text.literal("Oi you already got a license for that"));
                                                return 1;
                                            }

                                            final var license = manager.createLicense(userId, Capability.DEFAULT);
                                            context.getSource().sendFeedback(() -> Text.literal("Your license is " + license.uuid().toString()), true);

                                            return 1;
                                        }))
                                .then(literal("revoke")
                                        .requires(Permissions.require("chatbox.revoke", true))
                                        .executes(context -> {
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
                                        context.getSource().sendFeedback(() -> Text.literal("You are now spying chatbox commands!"), true);
                                    } else {
                                        context.getSource().sendFeedback(() -> Text.literal("You are no longer spying chatbox commands!"), true);
                                    }
                                    return 1;
                                })
                        )

        );
    }
}
