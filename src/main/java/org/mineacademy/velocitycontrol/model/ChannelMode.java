package org.mineacademy.velocitycontrol.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

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

	@Override
	public String toString() {
		return this.key;
	}
}