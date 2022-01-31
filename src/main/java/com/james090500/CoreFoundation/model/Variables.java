package com.james090500.CoreFoundation.model;

import com.james090500.CoreFoundation.Common;
import com.james090500.CoreFoundation.TimeUtil;
import com.james090500.CoreFoundation.Valid;
import com.james090500.CoreFoundation.collection.StrictMap;
import com.james090500.CoreFoundation.collection.expiringmap.ExpiringMap;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple engine that replaces variables in a message.
 */
public final class Variables {

	/**
	 * The pattern to find simple {} placeholders
	 */
	protected static final Pattern BRACKET_PLACEHOLDER_PATTERN = Pattern.compile("[{]([^{}]+)[}]");

	// ------------------------------------------------------------------------------------------------------------
	// Changing variables for loading
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Variables added to Foundation by you or other plugins
	 *
	 * You take in a command sender (may/may not be a player) and output a replaced string.
	 * The variable name (the key) is automatically surrounded by {} brackets
	 */
	private static final Map<String, Function<Player, String>> customVariables = new HashMap<>();

	/**
	 * Player, Their Cached Variables
	 */
	private static final StrictMap<String, Map<String, String>> cache = new StrictMap<>();

	/**
	 * Player, Original Message, Translated Message
	 */
	private static final Map<String, Map<String, TextComponent>> fastCache = makeNewFastCache();

	// ------------------------------------------------------------------------------------------------------------
	// Custom variables
	// ------------------------------------------------------------------------------------------------------------

	// ------------------------------------------------------------------------------------------------------------
	// Replacing
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Replaces variables in the message using the message sender as an object to replace
	 * player-related placeholders.
	 *
	 * @param message
	 * @param sender
	 * @return
	 */
	public static TextComponent replace(String message, Player sender) {
		Valid.checkNotNull(sender, "Sender cannot be null!");

		if (message == null || message.isEmpty())
			return Component.text("");

		final String original = message;

		{
			// Already cached ? Return.
			final Map<String, TextComponent> cached = fastCache.get(sender.getUsername());

			if (cached != null && cached.containsKey(original))
				return cached.get(original);
		}

		// Default
		message = replaceVariables0(sender, message);

		// Support the & color system
		TextComponent textComponent = Common.colorize(message);

		{
			final Map<String, TextComponent> map = fastCache.get(sender.getUsername());

			if (map != null)
				map.put(original, textComponent);
			else
				fastCache.put(sender.getUsername(), Common.newHashMap(original, textComponent));
		}

		return textComponent;
	}

	/**
	 * Replaces our hardcoded variables in the message, using a cache for better performance
	 *
	 * @param sender
	 * @param message
	 * @return
	 */
	private static String replaceVariables0(Player sender, String message) {
		final Matcher matcher = Variables.BRACKET_PLACEHOLDER_PATTERN.matcher(message);
		final Player player = sender instanceof Player ? (Player) sender : null;

		while (matcher.find()) {
			final String variable = matcher.group(1);

			final boolean isSenderCached = cache.containsKey(sender.getUsername());
			boolean makeCache = true;

			String value = null;

			// Player is cached
			if (isSenderCached) {
				final Map<String, String> senderCache = cache.get(sender.getUsername());
				final String storedVariable = senderCache.get(variable);

				// This specific variable is cached
				if (storedVariable != null) {
					value = storedVariable;
					makeCache = false;
				}
			}

			if (makeCache) {
				value = replaceVariable0(variable, player, sender);

				if (value != null) {
					final Map<String, String> speciCache = cache.getOrPut(sender.getUsername(), makeNewCache());

					speciCache.put(variable, value);
				}
			}
		}

		return message;
	}

	/**
	 * Replaces the given variable with a few hardcoded within the plugin, see below
	 *
	 * Also, if the variable ends with +, we insert a space after it if it is not empty
	 *
	 * @param variable
	 * @param player
	 * @param console
	 * @return
	 */
	private static String replaceVariable0(String variable, Player player, Player console) {
		final boolean insertSpace = variable.endsWith("+");

		if (insertSpace)
			variable = variable.substring(0, variable.length() - 1); // Remove the + symbol

		final String found = lookupVariable0(player, console, variable);

		return found == null ? null : found + (insertSpace && !found.isEmpty() ? " " : "");
	}

	/**
	 * Replaces the given variable with a few hardcoded within the plugin, see below
	 *
	 * @param player
	 * @param console
	 * @param variable
	 * @return
	 */
	private static String lookupVariable0(Player player, Player console, String variable) {
		{ // Replace custom variables
			final Function<Player, String> customReplacer = customVariables.get(variable);

			if (customReplacer != null)
				return customReplacer.apply(console);
		}

		switch (variable) {
			//todo
			case "plugin_name":
				return "VelocityControl";
			case "plugin_version":
				return "1.0.0";

			case "timestamp":
				return TimeUtil.getFormattedDate();

			case "player":
			case "player_display_name":
				return player == null ? console.getUsername() : player.getUsername();
			case "player_server_name":
				return player == null ? "" : player.getCurrentServer().get().getServerInfo().getName();
			case "player_address":
				return player == null ? "" : formatIp0(player);
		}

		return null;
	}

	/**
	 * Formats the {pl_address} variable for the player
	 *
	 * @param player
	 * @return
	 */
	private static String formatIp0(Player player) {
		try {
			return player.getRemoteAddress().toString().split("\\:")[0];
		} catch (final Throwable t) {
			return player.getRemoteAddress() != null ? player.getRemoteAddress().toString() : "";
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Cache making
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Create a new expiring map with 10 millisecond expiration
	 *
	 * @return
	 */
	private static Map<String, Map<String, TextComponent>> makeNewFastCache() {
		return ExpiringMap.builder()
				.maxSize(300)
				.expiration(10, TimeUnit.MILLISECONDS)
				.build();
	}

	/**
	 * Create a new expiring map with 1 second expiration, used to cache player-related
	 * variables that are called 10x after each other to save performance
	 *
	 * @return
	 */
	private static Map<String, String> makeNewCache() {
		return ExpiringMap.builder()
				.maxSize(300)
				.expiration(1, TimeUnit.SECONDS)
				.build();
	}
}