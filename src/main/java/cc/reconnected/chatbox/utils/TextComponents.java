package cc.reconnected.chatbox.utils;

import cc.reconnected.chatbox.RccChatbox;
import cc.reconnected.chatbox.parsers.MiniMessageSerializer;
import cc.reconnected.library.data.PlayerMeta;
import cc.reconnected.library.text.parser.MarkdownParser;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class TextComponents {
    public static final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();
    public static final Component tellPrefix = Component.empty()
            .append(Component.text("[", NamedTextColor.GRAY))
            .append(Component.text("CB PM").color(NamedTextColor.DARK_GRAY).hoverEvent(HoverEvent.showText(Component.text("This message was privately sent to you by an automated chatbot."))))
            .append(Component.text("]", NamedTextColor.GRAY));

    public static final Component sayPrefix = Component.empty()
            .append(Component.text("[", NamedTextColor.GRAY))
            .append(Component.text("CB").color(NamedTextColor.DARK_GRAY).hoverEvent(HoverEvent.showText(Component.text("This message was publicly sent by an automated chatbot."))))
            .append(Component.text("]", NamedTextColor.GRAY));

    public static Component addLabelInfo(Component name, PlayerMeta owner) {
        var ownerMeta = Component.text("Owned by " + owner.getEffectiveName());
        return name.hoverEvent(HoverEvent.showText(ownerMeta));
    }

    public static Component formatLabel(String name) {
        return legacySerializer.deserialize(name.trim());
    }

    public static Component formatContent(String content, @Nullable String type) {
        content = content.trim();
        Component formattedContent;
        type = type != null ? type : "unknown";
        switch (type) {
            case "format" -> formattedContent = legacySerializer.deserialize(content);
            case "markdown" -> {
                var rawContent = MarkdownParser.defaultParser.parseNode(content).toText();
                var json = JSONComponentSerializer.json();
                formattedContent = json.deserialize(Text.Serialization.toJsonString(rawContent, RccChatbox.getInstance().server.getRegistryManager()));
            }
            case "minimessage" -> formattedContent = MiniMessageSerializer.defaultSerializer.deserialize(content);
            default -> formattedContent = Component.text(content);
        }

        return formattedContent;
    }

    public static Component buildChatbotMessage(Component label, Component content, PlayerMeta owner) {
        return Component.empty()
                .append(addLabelInfo(label, owner))
                .append(Component.text(':', NamedTextColor.GRAY))
                .appendSpace()
                .append(content);

    }
}
