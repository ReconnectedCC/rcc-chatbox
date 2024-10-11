package cc.reconnected.chatbox.command;

import com.mojang.brigadier.CommandDispatcher;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.*;

public class ChatboxCommand {
    public static final Text prefix = Text.empty()
            .append(Text.literal("[").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
            .append(Text.literal("Chatbox").setStyle(Style.EMPTY.withColor(Formatting.GOLD)))
            .append(Text.literal("]").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
            .append(" ");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        var rootCommand = literal("chatbox")
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
                .then(LicenseSubCommand.register(dispatcher, registryAccess, environment))
                .then(SpySubCommand.register(dispatcher, registryAccess, environment));

        dispatcher.register(rootCommand);
    }
}
