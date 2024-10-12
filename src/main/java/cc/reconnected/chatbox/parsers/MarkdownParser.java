package cc.reconnected.chatbox.parsers;

import eu.pb4.placeholders.api.parsers.MarkdownLiteParserV1;
import static eu.pb4.placeholders.api.parsers.MarkdownLiteParserV1.MarkdownFormat;
import eu.pb4.placeholders.api.parsers.NodeParser;

public class MarkdownParser {
    public static final MarkdownFormat[] ALL = new MarkdownFormat[] {
            MarkdownLiteParserV1.MarkdownFormat.QUOTE,
            MarkdownLiteParserV1.MarkdownFormat.BOLD,
            MarkdownLiteParserV1.MarkdownFormat.ITALIC,
            MarkdownLiteParserV1.MarkdownFormat.UNDERLINE,
            MarkdownLiteParserV1.MarkdownFormat.STRIKETHROUGH,
            MarkdownLiteParserV1.MarkdownFormat.SPOILER,
            MarkdownLiteParserV1.MarkdownFormat.URL
    };

    public static final NodeParser contentParser = createParser(ALL);

    public static NodeParser createParser(MarkdownFormat[] capabilities) {
        return new MarkdownLiteParserV1(
                MarkdownComponentParser::spoilerFormatting,
                MarkdownComponentParser::quoteFormatting,
                MarkdownComponentParser::urlFormatting,
                capabilities
        );
    }
}
