package cc.reconnected.chatbox.utils;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class DateUtils {
    public static String getTime(Object obj) {
        var tz = TimeZone.getTimeZone("UTC");
        var df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(tz);
        return df.format(obj);
    }
}
