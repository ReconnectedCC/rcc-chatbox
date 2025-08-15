package cc.reconnected.chatbox.command;

import cc.reconnected.chatbox.listeners.ChatboxEvents;
import cc.reconnected.chatbox.state.StateSaverAndLoader;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.literal;

public class SpySubCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register(CommandDispatcher<CommandSourceStack> dispatcher,
                                                                       CommandBuildContext registryAccess,
                                                                       Commands.CommandSelection environment) {
        return literal("spy")
                .requires(Permissions.require("chatbox.spy", true))
                .executes(context -> {
                    if (!context.getSource().isPlayer()) {
                        context.getSource().sendSuccess(() -> Component.literal("This command can only be executed by players!"), false);
                        return 0;
                    }
                    final var player = context.getSource().getPlayer();
                    // checked for player presence above
                    //noinspection DataFlowIssue
                    var playerState = StateSaverAndLoader.getPlayerState(player);
                    playerState.enableSpy = !playerState.enableSpy;
                    ChatboxEvents.spyingPlayers.put(player.getUUID(), playerState.enableSpy);
                    if (playerState.enableSpy) {
                        context.getSource().sendSuccess(() ->
                                Component.empty().append(ChatboxCommand.prefix).append("You are now spying chatbox commands!"), true);
                    } else {
                        context.getSource().sendSuccess(() ->
                                Component.empty().append(ChatboxCommand.prefix).append("You are no longer spying chatbox commands!"), true);
                    }
                    return 1;
                });
    }
}
