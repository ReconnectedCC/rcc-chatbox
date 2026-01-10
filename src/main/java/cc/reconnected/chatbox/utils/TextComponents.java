package cc.reconnected.chatbox.utils;

import cc.reconnected.library.data.PlayerMeta;
import cc.reconnected.library.text.parser.MarkdownParser;
import eu.pb4.placeholders.api.parsers.LegacyFormattingParser;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.TextParserV1;
import me.alexdevs.solstice.api.text.tag.PhaseGradientTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

public class TextComponents {

    private static final NodeParser MINIMESSAGE_PARSER;
    static {
        var parser = TextParserV1.createSafe();
        parser.register(PhaseGradientTag.createTag());
        MINIMESSAGE_PARSER = parser;
    }
    private static Component ChatboxHoverText = Component.literal("This message was privately sent to you by an automated chatbot.");

    public static final Component tellPrefix = Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.GRAY)
            .append(Component.literal("CB PM").withStyle(ChatFormatting.DARK_GRAY).withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,ChatboxHoverText)))))
            .append(Component.literal("] ").withStyle(ChatFormatting.GRAY));

    public static final Component sayPrefix = Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.GRAY)
            .append(Component.literal("CB").withStyle(ChatFormatting.DARK_GRAY).withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,ChatboxHoverText)))))
            .append(Component.literal("] ").withStyle(ChatFormatting.GRAY));

    public static Component addLabelInfo(Component name, PlayerMeta owner) {
        var ownerMeta = Component.literal("Owned by " + owner.getEffectiveName());
        return Component.empty().withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,ownerMeta)));
    }

    public static Component formatLabel(String name) {
        return LegacyFormattingParser.ALL.parseNode(name).toText(null, true);
    }

    public static Component formatContent(String content, @Nullable String type) {
        content = content.trim();
        Component formattedContent;
        type = type != null ? type : "unknown";
        NodeParser parser;
        switch (type) {
            case "format" -> parser = LegacyFormattingParser.ALL;
            case "markdown" -> parser = MarkdownParser.defaultParser;
            case "minimessage" -> parser =MINIMESSAGE_PARSER;
            default -> parser = null;
        }
        if (parser == null ) {
            formattedContent = Component.literal(content);
        }
        else {
            formattedContent = parser.parseNode(content).toText(null, true);
        }

        return formattedContent;
    }

    public static Component buildChatbotMessage(Component label, Component content, PlayerMeta owner) {
        return Component.empty()
                .append(addLabelInfo(label, owner))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(content);

    }
}
