package com.james090500.CoreFoundation.model;

import com.james090500.CoreFoundation.Common;
import com.james090500.CoreFoundation.SerializeUtil;
import com.james090500.CoreFoundation.Valid;
import com.james090500.CoreFoundation.collection.SerializedMap;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

/**
 * An elegant way to find {variables} and replace them.
 * <p>
 * Use {@link #find(String...)} method to find {} variables such as find("health") and then
 * replace them with {@link #replace(Object...)} with the objects such as replace(20)
 * <p>
 * Something like {@link String#format(String, Object...)} but more useful for Minecraft
 */
@AllArgsConstructor
public final class Replacer {

	/**
	 * The internal deliminer used to separate variables
	 */
	private final static String DELIMITER = "%D3L1M1T3R%";

	/**
	 * The messages we are replacing variables in
	 */
	private final String[] messages;

	// ------------------------------------------------------------------------------------------------------------
	// Temporary variables
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * The variables we are looking for to replace
	 */
	private String[] variables;

	/**
	 * The finished replaced message
	 */
	@Getter
	private String[] replacedMessage;

	/**
	 * Create a new instance for the given messages
	 *
	 * @param messages
	 */
	private Replacer(String... messages) {
		this.messages = messages;
	}

	/**
	 * Find the {} variables
	 *
	 * @param variables
	 * @return
	 *
	 * @deprecated use {@link Replacer#replaceArray(String, Object...)} for simplicity
	 */
	@Deprecated
	public Replacer find(String... variables) {
		this.variables = variables;

		return this;
	}

	/**
	 * Attempts to replace key:value pairs automatically
	 * <p>
	 * See {@link SerializedMap#ofArray(Object...)}
	 *
	 * @deprecated use {@link Replacer#replaceArray(String, Object...)} for simplicity
	 *
	 * @param associativeArray
	 * @return
	 */
	@Deprecated
	public String replaceAll(Object... associativeArray) {
		final SerializedMap map = SerializedMap.ofArray(associativeArray);

		final List<String> find = new ArrayList<>();
		final List<Object> replaced = new ArrayList<>();

		for (final Map.Entry<String, Object> entry : map.entrySet()) {
			find.add(entry.getKey());
			replaced.add(entry.getValue());
		}

		find(find.toArray(new String[find.size()]));
		replace(replaced.toArray(new Object[replaced.size()]));

		return getReplacedMessageJoined();
	}

	/**
	 * Replaces stored variables with their counterparts
	 * <p>
	 * Call this after {@link #find(String...)}!
	 *
	 * @deprecated use {@link Replacer#replaceArray(String, Object...)} for simplicity
	 *
	 * @param replacements
	 * @return
	 */
	@Deprecated
	public Replacer replace(Object... replacements) {
		Valid.checkNotNull(variables, "call find() first");
		Valid.checkBoolean(replacements.length == variables.length, "Variables " + variables.length + " != replacements " + replacements.length);

		// Join and replace as 1 message for maximum performance
		String message = String.join(DELIMITER, messages);

		for (int i = 0; i < variables.length; i++) {
			String find = variables[i];

			{ // Auto insert brackets
				if (!find.startsWith("{"))
					find = "{" + find;

				if (!find.endsWith("}"))
					find = find + "}";
			}

			final Object rep = i < replacements.length ? replacements[i] : null;

			// Convert it into a human readable string
			String serialized;

			try {
				serialized = Objects.toString(SerializeUtil.serialize(rep));

			} catch (final Throwable ex) {
				// ignore and convert to string
				serialized = rep.toString();
			}

			message = message.replace(find, rep != null ? serialized : "");
		}

		this.replacedMessage = message.split(DELIMITER);

		return this;
	}

	/**
	 * Return the replaced message joined with spaces
	 *
	 * @deprecated use {@link Replacer#replaceArray(String, Object...)} for simplicity
	 *
	 * @return
	 */
	@Deprecated
	public String getReplacedMessageJoined() {
		Valid.checkNotNull(replacedMessage, "Replaced message not yet set, use find() and replace()");

		return String.join(" ", replacedMessage);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static access
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Create a new replacer instance in which you can easily find and replace variables and send the message to player
	 *
	  * @deprecated use {@link Replacer#replaceArray(String, Object...)} for simplicity
	 *
	 * @param messages
	 * @return
	 */
	@Deprecated
	public static Replacer of(String... messages) {
		return new Replacer(messages);
	}

	/**
	 * Replace key pairs in the message
	 *
	 * @param message
	 * @param variables
	 * @return
	 */
	public static String replaceVariables(String message, SerializedMap variables) {
		if (message == null)
			return null;

		if ("".equals(message))
			return "";

		final Matcher matcher = Variables.BRACKET_PLACEHOLDER_PATTERN.matcher(message);

		while (matcher.find()) {
			String variable = matcher.group(1);

			boolean frontSpace = false;
			boolean backSpace = false;

			if (variable.startsWith("+")) {
				variable = variable.substring(1);

				frontSpace = true;
			}

			if (variable.endsWith("+")) {
				variable = variable.substring(0, variable.length() - 1);

				backSpace = true;
			}

			String value = null;

			for (final Map.Entry<String, Object> entry : variables.entrySet()) {
				String variableKey = entry.getKey();

				variableKey = variableKey.startsWith("{") ? variableKey.substring(1) : variableKey;
				variableKey = variableKey.endsWith("}") ? variableKey.substring(0, variableKey.length() - 1) : variableKey;

				if (variableKey.equals(variable))
					value = entry.getValue() == null ? "null" : entry.getValue().toString();
			}

			if (value != null) {
				final boolean emptyColorless = Common.stripColors(value).isEmpty();
				value = value.isEmpty() ? "" : (frontSpace && !emptyColorless ? " " : "") + value + (backSpace && !emptyColorless ? " " : "");

				message = message.replace(matcher.group(), value);
			}
		}

		return message;
	}
}
