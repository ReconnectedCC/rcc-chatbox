package cc.reconnected.chatbox.command;

import cc.reconnected.chatbox.ChatboxEvents;
import cc.reconnected.chatbox.data.StateSaverAndLoader;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class SpySubCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> register(CommandDispatcher<ServerCommandSource> dispatcher,
                                                                       CommandRegistryAccess registryAccess,
                                                                       CommandManager.RegistrationEnvironment environment) {
        return literal("spy")
                .requires(Permissions.require("chatbox.spy", true))
                .executes(context -> {
                    if (!context.getSource().isExecutedByPlayer()) {
                        context.getSource().sendFeedback(() -> Text.literal("This command can only be executed by players!"), false);
                        return 0;
                    }
                    final var player = context.getSource().getPlayer();
                    // checked for player presence above
                    //noinspection DataFlowIssue
                    var playerState = StateSaverAndLoader.getPlayerState(player);
                    playerState.enableSpy = !playerState.enableSpy;
                    ChatboxEvents.spyingPlayers.put(player.getUuid(), playerState.enableSpy);
                    if (playerState.enableSpy) {
                        context.getSource().sendFeedback(() ->
                                Text.empty().append(ChatboxCommand.prefix).append("You are now spying chatbox commands!"), true);
                    } else {
                        context.getSource().sendFeedback(() ->
                                Text.empty().append(ChatboxCommand.prefix).append("You are no longer spying chatbox commands!"), true);
                    }
                    return 1;
                });
    }
}
