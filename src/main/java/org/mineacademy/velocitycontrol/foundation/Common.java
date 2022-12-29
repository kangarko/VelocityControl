package org.mineacademy.velocitycontrol.foundation;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.mineacademy.velocitycontrol.VelocityControl;
import org.mineacademy.velocitycontrol.foundation.exception.VCException;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Our main utility class hosting a large variety of different convenience functions
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Common {

	// ------------------------------------------------------------------------------------------------------------
	// Constants
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Pattern used to match colors with & or {@link LegacyComponentSerializer#SECTION_CHAR}
	 */
	private static final Pattern COLOR_AND_DECORATION_REGEX = Pattern.compile("(&|" + LegacyComponentSerializer.SECTION_CHAR + ")[0-9a-fk-orA-FK-OR]");

	/**
	 * Pattern used to match colors with #HEX code for MC 1.16+
	 *
	 * Matches {#CCCCCC} or &#CCCCCC or #CCCCCC
	 */
	public static final Pattern HEX_COLOR_REGEX = Pattern.compile("(?<!\\\\)(\\{|&|)#((?:[0-9a-fA-F]{3}){2})(\\}|)");

	/**
	 * Pattern used to match colors with #HEX code for MC 1.16+
	 */
	private static final Pattern RGB_X_COLOR_REGEX = Pattern.compile("(" + LegacyComponentSerializer.SECTION_CHAR + "x)(" + LegacyComponentSerializer.SECTION_CHAR + "[0-9a-fA-F]){6}");

	/**
	 * We use this to send messages with colors to your console
	 */
	private static final ConsoleCommandSource CONSOLE_SENDER = VelocityControl.getServer().getConsoleCommandSource();

	// ------------------------------------------------------------------------------------------------------------
	// Tell prefix
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Should we add a prefix to the messages we send to the console?
	 * <p>
	 * True by default
	 */
	public static boolean ADD_LOG_PREFIX = true;

	/**
	 * The tell prefix applied on tell() methods
	 */
	@Getter
	private static final String tellPrefix = "[VelocityControl]";

	/**
	 * The log prefix applied on log() methods
	 */
	@Getter
	private static final String logPrefix = "[VelocityControl]";

	// ------------------------------------------------------------------------------------------------------------
	// Broadcasting
	// ------------------------------------------------------------------------------------------------------------

	// ------------------------------------------------------------------------------------------------------------
	// Messaging
	// ------------------------------------------------------------------------------------------------------------

	// Remove first spaces from the given message
	private static String removeFirstSpaces(String message) {
		message = getOrEmpty(message);

		while (message.startsWith(" "))
			message = message.substring(1);

		return message;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Colorizing messages
	// ------------------------------------------------------------------------------------------------------------

	// Remove first and last spaces from the given message
	private static String removeSurroundingSpaces(String message) {
		message = getOrEmpty(message);

		while (message.endsWith(" "))
			message = message.substring(0, message.length() - 1);

		return removeFirstSpaces(message);
	}

	/**
	 * Replaces the {@link LegacyComponentSerializer#SECTION_CHAR} colors with & letters
	 *
	 * @param message
	 * @return
	 */
	public static String revertColorizing(final String message) {
		return message.replaceAll("(?i)" + LegacyComponentSerializer.SECTION_CHAR + "([0-9a-fk-or])", "&$1");
	}

	/**
	 * Remove all {@link LegacyComponentSerializer#SECTION_CHAR} as well as & letter colors from the message
	 *
	 * @param message
	 * @return
	 */
	public static String stripColors(String message) {

		if (message == null || message.isEmpty())
			return message;

		// Replace & color codes
		Matcher matcher = COLOR_AND_DECORATION_REGEX.matcher(message);

		while (matcher.find())
			message = matcher.replaceAll("");

		// Replace hex colors, both raw and parsed
		matcher = HEX_COLOR_REGEX.matcher(message);

		while (matcher.find())
			message = matcher.replaceAll("");

		matcher = RGB_X_COLOR_REGEX.matcher(message);

		while (matcher.find())
			message = matcher.replaceAll("");

		message = message.replace(LegacyComponentSerializer.SECTION_CHAR + "x", "");

		return message;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Aesthetics
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Returns a long ------ console line
	 *
	 * @return
	 */
	public static String consoleLine() {
		return "!-----------------------------------------------------!";
	}

	/**
	 * Prepends the given string with either "a" or "an" (does a dummy syllable check)
	 *
	 * @param ofWhat
	 * @return
	 * @deprecated only a dummy syllable check, e.g. returns a hour
	 */
	@Deprecated
	public static String article(final String ofWhat) {
		Preconditions.checkArgument(ofWhat.length() > 0, "String cannot be empty");
		final List<String> syllables = Arrays.asList("a", "e", "i", "o", "u", "y");

		return (syllables.contains(ofWhat.toLowerCase().trim().substring(0, 1)) ? "an" : "a") + " " + ofWhat;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Logging and error handling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Logs a bunch of messages to the console, & colors are supported
	 *
	 * @param messages
	 */
	public static void log(final String... messages) {
		log(true, messages);
	}

	/*
	 * Logs a bunch of messages to the console, & colors are supported
	 */
	private static void log(final boolean addLogPrefix, final String... messages) {
		if (messages == null)
			return;

		if (CONSOLE_SENDER == null)
			throw new VCException("Failed to initialize Console Sender, are you running Foundation under a Bukkit/Spigot server?");

		for (String message : messages) {
			if (message.equals("none"))
				continue;

			if (stripColors(message).replace(" ", "").isEmpty()) {
				CONSOLE_SENDER.sendMessage(Component.text("  "));

				continue;
			}

			if (message.startsWith("[JSON]")) {
				final String stripped = message.replaceFirst("\\[JSON\\]", "").trim();

				if (!stripped.isEmpty())
					log(stripped);

			} else
				for (final String part : message.split("\n")) {
					final String log = ((addLogPrefix && ADD_LOG_PREFIX ? removeSurroundingSpaces(logPrefix) + " " : "") + getOrEmpty(part).replace("\n", "\n&r")).trim();

					CONSOLE_SENDER.sendMessage(Component.text(log));
				}
		}
	}

	/**
	 * Logs a bunch of messages to the console in a {@link #consoleLine()} frame.
	 * <p>
	 * Used when an error occurs, can also disable the plugin
	 *
	 * @param disablePlugin
	 * @param messages
	 * @deprecated BungeeCord cannot disable plugins
	 */
	@Deprecated
	public static void logFramed(final boolean disablePlugin, final String... messages) {
		if (messages != null && messages != null) {
			log("&7" + consoleLine());
			for (final String msg : messages)
				log(" &c" + msg);

			if (disablePlugin)
				log(" &cPlugin is now disabled.");

			log("&7" + consoleLine());
		}
	}

	/**
	 * Saves the error, prints the stack trace and logs it in frame.
	 * Possible to use %error variable
	 *
	 * @param t
	 * @param messages
	 */
	public static void error(final Throwable t, final String... messages) {
		if (!(t instanceof VCException))
			Debugger.saveError(t, messages);

		Debugger.printStackTrace(t);
		logFramed(false, replaceErrorVariable(t, messages));
	}

	/**
	 * Logs the messages in frame (if not null),
	 * saves the error to errors.log and then throws it
	 * <p>
	 * Possible to use %error variable
	 *
	 * @param t
	 * @param messages
	 */
	public static void throwError(Throwable t, final String... messages) {

		// Get to the root cause of this problem
		while (t.getCause() != null)
			t = t.getCause();

		// Delegate to only print out the relevant stuff
		if (t instanceof VCException)
			throw (VCException) t;

		if (messages != null)
			logFramed(false, replaceErrorVariable(t, messages));

		Debugger.saveError(t, messages);
	}

	/*
	 * Replace the %error variable with a smart error info, see above
	 */
	private static String[] replaceErrorVariable(Throwable throwable, final String... msgs) {
		while (throwable.getCause() != null)
			throwable = throwable.getCause();

		final String throwableName = throwable == null ? "Unknown error." : throwable.getClass().getSimpleName();
		final String throwableMessage = throwable == null || throwable.getMessage() == null || throwable.getMessage().isEmpty() ? "" : ": " + throwable.getMessage();

		for (int i = 0; i < msgs.length; i++) {
			final String error = throwableName + throwableMessage;

			msgs[i] = msgs[i]
					.replace("%error%", error)
					.replace("%error", error);
		}

		return msgs;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Joining strings and lists
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Joins an array together using spaces from the given start index
	 *
	 * @param startIndex
	 * @param array
	 * @return
	 */
	public static String joinRange(final int startIndex, final String[] array) {
		return joinRange(startIndex, array.length, array);
	}

	/**
	 * Join an array together using spaces using the given range
	 *
	 * @param startIndex
	 * @param stopIndex
	 * @param array
	 * @return
	 */
	public static String joinRange(final int startIndex, final int stopIndex, final String[] array) {
		return joinRange(startIndex, stopIndex, array, " ");
	}

	/**
	 * Join an array together using the given deliminer
	 *
	 * @param start
	 * @param stop
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static String joinRange(final int start, final int stop, final String[] array, final String delimiter) {
		String joined = "";

		for (int i = start; i < Math.min(Math.max(stop, 0), array.length); i++)
			joined += (joined.isEmpty() ? "" : delimiter) + array[i];

		return joined;
	}

	/**
	 * A convenience method for converting array of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> String join(final T[] array) {
		return array == null ? "null" : join(Arrays.asList(array));
	}

	/**
	 * A convenience method for converting list of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> String join(final Iterable<T> array) {
		return array == null ? "null" : join(array, ", ");
	}

	/**
	 * A convenience method for converting list of objects into array of strings
	 * We invoke "toString" for each object given it is not null, or return "" if it is
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @return
	 */
	public static <T> String join(final Iterable<T> array, final String delimiter) {
		return join(array, delimiter, object -> object == null ? "" : simplify(object));
	}

	/**
	 * Joins a list of a given type using the given delimiter and a helper interface
	 * to convert each element in the array into string
	 *
	 * @param <T>
	 * @param array
	 * @param delimiter
	 * @param stringer
	 * @return
	 */
	public static <T> String join(final Iterable<T> array, final String delimiter, final Stringer<T> stringer) {
		final Iterator<T> it = array.iterator();
		String message = "";

		while (it.hasNext()) {
			final T next = it.next();

			if (next != null)
				message += stringer.toString(next) + (it.hasNext() ? delimiter : "");
		}

		return message;
	}

	/**
	 * Replace some common classes such as entity to name automatically
	 *
	 * @param arg
	 * @return
	 */
	public static String simplify(Object arg) {
		if (arg instanceof Player)
			return ((Player) arg).getUsername();

		else if (arg.getClass() == double.class || arg.getClass() == float.class)
			return new DecimalFormat("#.##").format((double) arg);

		else if (arg instanceof Collection)
			return Common.join((Collection<?>) arg, ", ", Common::simplify);

		else if (arg instanceof TextComponent)
			return ((Enum<?>) arg).name().toLowerCase();

		else if (arg instanceof Enum)
			return arg.toString().toLowerCase();

		return arg.toString();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Converting and retyping
	// ------------------------------------------------------------------------------------------------------------

	// ------------------------------------------------------------------------------------------------------------
	// Misc message handling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return an empty String if the String is null or equals to none.
	 *
	 * @param input
	 * @return
	 */
	public static String getOrEmpty(final String input) {
		return input == null || "none".equalsIgnoreCase(input) ? "" : input;
	}

	/**
	 * Returns the value or its default counterpart in case it is null
	 *
	 * PSA: If values are strings, we return default if the value is empty or equals to "none"
	 *
	 * @param value the primary value
	 * @param def   the default value
	 * @return the value, or default it the value is null
	 */
	public static <T> T getOrDefault(final T value, final T def) {
		if (value instanceof String && ("none".equalsIgnoreCase((String) value) || "".equals(value)))
			return def;

		return getOrDefaultStrict(value, def);
	}

	/**
	 * Returns the value or its default counterpart in case it is null
	 *
	 * @param <T>
	 * @param value
	 * @param def
	 * @return
	 */
	public static <T> T getOrDefaultStrict(final T value, final T def) {
		return value != null ? value : def;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Scheduling
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Runs the task even if the plugin is disabled for some reason.
	 *
	 * @param delayTicks
	 * @param task
	 * @return the task or null
	 */
	public static ScheduledTask runLater(final int delayTicks, Runnable task) {
		final Scheduler scheduler = VelocityControl.getServer().getScheduler();
		return scheduler.buildTask(VelocityControl.getInstance(), task).delay(Duration.ofMillis(delayTicks * 50L)).schedule();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * A simple interface from converting objects into strings
	 *
	 * @param <T>
	 */
	public interface Stringer<T> {

		/**
		 * Convert the given object into a string
		 *
		 * @param object
		 * @return
		 */
		String toString(T object);
	}

}
