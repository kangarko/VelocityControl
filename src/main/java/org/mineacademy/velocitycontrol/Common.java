package org.mineacademy.velocitycontrol;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class Common {
	// This was actually just concatenating strings in the original. Now it uses StringBuilders.
	public static String joinRange(int start, int stop, String[] array, String delimiter) {
		StringBuilder joined = new StringBuilder();

		for (int i = start; i < clamp(stop, 0, array.length); i++)
			joined.append(joined.length() <= 0 ? "" : delimiter).append(array[i]);

		return joined.toString();
	}

	public static int clamp(int number, int min, int max) {
		return Math.min(Math.max(number, min), max);
	}
	public static String stripColors(String message) {
		return message == null ? "" : message.replaceAll("(" + LegacyComponentSerializer.SECTION_CHAR + "|&)([0-9a-fk-or])", "");
	}
	public static <T> T getOrDefaultStrict(final @Nullable T value, final @Nonnull T def) {
		return value == null ? def : value;
	}

}