package cc.reconnected.chatbox.utils;

import cc.reconnected.chatbox.parsers.MarkdownParser;
import cc.reconnected.chatbox.parsers.MiniMessageSerializer;
import cc.reconnected.server.database.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.text.Text;

public class TextComponents {
    public static final Component tellPrefix = Component.empty()
            .append(Component.text("[", NamedTextColor.GRAY))
            .append(Component.text("CB PM").color(NamedTextColor.DARK_GRAY).hoverEvent(HoverEvent.showText(Component.text("This message was privately sent to you by an automated chatbot."))))
            .append(Component.text("]", NamedTextColor.GRAY));

    public static final Component sayPrefix = Component.empty()
            .append(Component.text("[", NamedTextColor.GRAY))
            .append(Component.text("CB").color(NamedTextColor.DARK_GRAY).hoverEvent(HoverEvent.showText(Component.text("This message was publicly sent by an automated chatbot."))))
            .append(Component.text("]", NamedTextColor.GRAY));

    public static Component getChatbotName(Component name, PlayerData owner) {
        var ownerMeta = Component.text("Owned by " + owner.getEffectiveName());
        return name.hoverEvent(HoverEvent.showText(ownerMeta));
    }

    public static Component buildChatbotMessage(String label, String content, String type, PlayerData owner) {
        content = content.trim();
        Component formattedLabel = LegacyComponentSerializer.legacyAmpersand().deserialize(label.trim());
        Component formattedContent;
        if ("format".equals(type)) {
            formattedContent = LegacyComponentSerializer.legacyAmpersand().deserialize(content);
        } else if ("markdown".equals(type)) {
            var rawContent = MarkdownParser.contentParser.parseNode(content).toText();
            var json = JSONComponentSerializer.json();
            formattedContent = json.deserialize(Text.Serializer.toJson(rawContent));
        } else if("minimessage".equals(type)) {
            formattedContent = MiniMessageSerializer.defaultSerializer.deserialize(content);
        } else {
            formattedContent = Component.text(content);
        }
        return Component.empty()
                .append(getChatbotName(formattedLabel, owner))
                .append(Component.text(':', NamedTextColor.GRAY))
                .appendSpace()
                .append(formattedContent);

    }
}
