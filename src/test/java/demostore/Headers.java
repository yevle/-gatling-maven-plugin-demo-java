package demostore;

import java.util.Map;

public class Headers {
    public static Map<CharSequence, String> authorization = Map.ofEntries(
            Map.entry("authorization", "Bearer #{jwt}")
    );
}
