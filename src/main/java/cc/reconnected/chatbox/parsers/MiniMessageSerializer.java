package cc.reconnected.chatbox.parsers;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

import java.util.Collection;
import java.util.Set;

public class MiniMessageSerializer {

    public static final Set<TagResolver> defaultTags = Set.of(
            StandardTags.color(),
            StandardTags.decorations(),
            StandardTags.gradient(),
            StandardTags.hoverEvent(),
            StandardTags.newline(),
            StandardTags.rainbow(),
            StandardTags.reset(),
            StandardTags.transition(),
            StandardTags.keybind(),
            StandardTags.translatable(),
            StandardTags.insertion()
    );

    public static final MiniMessage defaultSerializer = createSerializer(defaultTags);

    public static MiniMessage createSerializer(Collection<TagResolver> tags) {
        return MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolvers(tags)
                        .build())
                .build();
    }
}
