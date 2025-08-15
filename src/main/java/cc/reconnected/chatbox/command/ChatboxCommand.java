package cc.reconnected.chatbox.command;

import com.mojang.brigadier.CommandDispatcher;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import static net.minecraft.commands.Commands.*;

public class ChatboxCommand {
    public static final Component prefix = Component.empty()
            .append(Component.literal("[").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
            .append(Component.literal("Chatbox").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
            .append(Component.literal("]").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
            .append(" ");

    public static MutableComponent buildHelpMessage(String base, String[] subs) {
        var text = Component.empty();
        for (var command : subs) {
            text = text.append(Component.nullToEmpty("\n - "))
                    .append(Component.literal("/" + base + " " + command)
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.BLUE).withUnderlined(true)
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.nullToEmpty("Click to suggest command")))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + base + " " + command))
                            )
                    );
        }

        return text;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext registryAccess,
                                Commands.CommandSelection environment) {
        var rootCommand = literal("chatbox")
                .requires(Permissions.require("chatbox.command", true))
                .executes(context -> {
                    var commands = new String[]{
                            "license",
                            "license register",
                            "license revoke",
                            "spy"
                    };

                    final var text = Component.empty()
                            .append(prefix)
                            .append("Manage your Chatbox license:")
                            .append(buildHelpMessage("chatbox", commands));


                    context.getSource().sendSuccess(() -> text, false);
                    return 1;
                })
                .then(LicenseSubCommand.register(dispatcher, registryAccess, environment))
                .then(SpySubCommand.register(dispatcher, registryAccess, environment))
                .then(AdminSubCommand.register(dispatcher, registryAccess, environment));

        dispatcher.register(rootCommand);
    }
}
