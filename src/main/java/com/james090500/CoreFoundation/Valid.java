package com.james090500.CoreFoundation;

import com.james090500.CoreFoundation.exception.FoException;
import lombok.experimental.UtilityClass;

/**
 * Utility class for checking conditions and throwing our safe exception that is
 * logged into file.
 */
@UtilityClass
public final class Valid {

	// ------------------------------------------------------------------------------------------------------------
	// Checking for validity and throwing errors if false or null
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Throw an error with a custom message if the given object is null
	 *
	 * @param toCheck
	 * @param falseMessage
	 */
	public void checkNotNull(final Object toCheck, final String falseMessage) {
		if (toCheck == null)
			throw new FoException(falseMessage);
	}

	/**
	 * Throw an error with a custom message if the given expression is false
	 *
	 * @param expression
	 * @param falseMessage
	 * @param replacements
	 */
	public void checkBoolean(final boolean expression, final String falseMessage, final Object... replacements) {
		if (!expression)
			throw new FoException(String.format(falseMessage, replacements));
	}

	// ------------------------------------------------------------------------------------------------------------
	// Checking for true without throwing errors
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if the array consists of null or empty string values only
	 *
	 * @param array
	 * @return
	 */
	public boolean isNullOrEmpty(final Object[] array) {
		if (array != null)
			for (final Object object : array)
				if (object instanceof String) {
					if (!((String) object).isEmpty())
						return false;

				} else if (object != null)
					return false;

		return true;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Equality checks
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if two strings are equal regardless of their colors
	 *
	 * @param first
	 * @param second
	 * @return
	 */
	public boolean colorlessEquals(final String first, final String second) {
		return Common.stripColors(first).equalsIgnoreCase(Common.stripColors(second));
	}

	// ------------------------------------------------------------------------------------------------------------
	// Matching in lists
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return true if any element in the given list contains (case ignored) your given element
	 *
	 * @param element
	 * @param list
	 * @return
	 *
	 * @deprecated can lead to unwanted matches such as when /time is in list, /t will also get caught
	 */
	@Deprecated
	public boolean isInListContains(final String element, final Iterable<String> list) {
		try {
			for (final String matched : list)
				if (removeSlash(element).toLowerCase().contains(removeSlash(matched).toLowerCase()))
					return true;

		} catch (final ClassCastException ex) { // for example when YAML translates "yes" to "true" to boolean (!) (#wontfix)
		}

		return false;
	}

	/**
	 * Prepare the message for isInList comparation - lowercases it and removes the initial slash /
	 *
	 * @param message
	 * @return
	 */
	private String removeSlash(String message) {
		return message.startsWith("/") ? message.substring(1) : message;
	}
}
