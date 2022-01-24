package org.mineacademy.velocitycontrol.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.mineacademy.bfo.Common;

/**
 * Represents what mode the player is in the channel
 */
@RequiredArgsConstructor
public enum ChannelMode {

	/**
	 * Receive and send messages
	 */
	WRITE("write", NamedTextColor.GOLD),

	/**
	 * Receive messages but not write them
	 */
	READ("read", NamedTextColor.GREEN);

	/**
	 * The unobfuscated config key
	 */
	@Getter
	private final String key;

	/**
	 * The color associated with this mode
	 * Used in command channel listing
	 */
	@Getter
	private final TextColor color;

	/**
	 * Load the mode from the config key
	 *
	 * @param key
	 * @return
	 */
	public static ChannelMode fromKey(String key) {
		for (final ChannelMode mode : values())
			if (mode.key.equalsIgnoreCase(key))
				return mode;

		throw new IllegalArgumentException("No such channel mode: " + key + ". Available: " + Common.join(values(), ", ", ChannelMode::getKey));
	}

	@Override
	public String toString() {
		return this.key;
	}
}