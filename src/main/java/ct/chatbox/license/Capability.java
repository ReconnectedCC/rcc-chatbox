package ct.chatbox.license;

import java.util.HashSet;
import java.util.Set;

public enum Capability {
    TELL(1),
    READ(2),
    COMMAND(4),
    SAY(8);

    public static final Set<Capability> DEFAULT = Set.of(TELL, READ, COMMAND);
    public final int value;
    Capability(int value) {
        this.value = value;
    }

    public static Set<Capability> unpack(int packed) {
        Set<Capability> set = new HashSet<>();
        for (Capability c : Capability.values()) {
            if((packed & c.value) != 0)
                set.add(c);
        }

        return set;
    }

    public static int pack(Set<Capability> capabilities) {
        int packed = 0;
        for (Capability capability : capabilities) {
            packed += capability.value;
        }
        return packed;
    }
}
