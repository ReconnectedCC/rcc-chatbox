package cc.reconnected.chatbox.listeners;

import cc.reconnected.chatbox.RccChatbox;
import cc.reconnected.chatbox.license.Capability;
import cc.reconnected.chatbox.models.DiscordUser;
import cc.reconnected.chatbox.packets.serverPackets.events.DiscordChatEvent;
import cc.reconnected.chatbox.utils.DateUtils;
import cc.reconnected.discordbridge.events.DiscordMessageEvents;
import cc.reconnected.library.text.parser.MarkdownParser;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.minecraft.text.Text;

import java.util.Date;

public class DiscordEvents {
    public static void register() {
        // discord chat events
        DiscordMessageEvents.MESSAGE_CREATE.register((message, member) -> emitDiscordChatEvent(message, member, false));
        DiscordMessageEvents.MESSAGE_EDIT.register((message, member) -> emitDiscordChatEvent(message, member, true));
    }

    private static void emitDiscordChatEvent(Message message, Member member, boolean isEdited) {
        var user = DiscordUser.fromMember(member, true);
        var packet = new DiscordChatEvent();
        packet.text = message.getContentStripped();
        packet.rawText = message.getContentRaw();
        var json = Text.Serialization.toJsonString(MarkdownParser.defaultParser.parseNode(message.getContentDisplay()).toText(), RccChatbox.getInstance().server.getRegistryManager());
        packet.renderedText = JsonParser.parseString(json);
        packet.discordId = message.getId();
        packet.discordUser = user;
        packet.edited = isEdited;

        var messageOffsetDate = isEdited ? message.getTimeEdited() : message.getTimeCreated();
        Date messageDate;
        if (messageOffsetDate != null) {
            messageDate = new Date(messageOffsetDate.toInstant().toEpochMilli());
        } else {
            messageDate = new Date();
        }
        packet.time = DateUtils.getTime(messageDate);

        RccChatbox.getInstance().wss().broadcastEvent(packet, Capability.READ);
    }
}
