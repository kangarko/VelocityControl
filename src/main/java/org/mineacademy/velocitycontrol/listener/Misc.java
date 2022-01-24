package org.mineacademy.velocitycontrol.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Misc {
    public static Component colorize(String str) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(str);
    }
}
