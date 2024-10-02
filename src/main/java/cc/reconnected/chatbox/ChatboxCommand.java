package cc.reconnected.chatbox;

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
                                        .executes(context -> {
                                            final var userId = context.getSource().getPlayer().getUuid();
                                            var userLicense = manager.getLicenseFromUser(userId);
                                            if (userLicense != null) {
                                                context.getSource().sendMessage(Text.literal("Oi you already a loicense for that"));
                                                return 1;
                                            }

                                            final var license = manager.createLicense(userId, Capability.DEFAULT);
                                            context.getSource().sendFeedback(() -> Text.literal("Your loicense is " + license.uuid().toString()), true);

                                            return 1;
                                        })))
        );
    }
}
