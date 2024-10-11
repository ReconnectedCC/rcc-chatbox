package cc.reconnected.chatbox.parsers;

import java.util.HashSet;
import java.util.Set;

public class Formats {

    public static final HashSet<String> available = new HashSet<>(Set.of(
            "markdown",
            "format",
            "minimessage"
    ));
}
