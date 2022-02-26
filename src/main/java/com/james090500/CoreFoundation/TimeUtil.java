package com.james090500.CoreFoundation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Utility class for calculating time from ticks and back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeUtil {

	/**
	 * The date format in dd.MM.yyy HH:mm:ss
	 */
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	// ------------------------------------------------------------------------------------------------------------
	// Current time
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Converts the time from a human readable format like "10 minutes"
	 * to seconds.
	 *
	 * @param humanReadableTime the human readable time format: {time} {period}
	 * 		  	   example: 5 seconds, 10 ticks, 7 minutes, 12 hours etc..
	 *
	 * @return the converted human time to seconds
	 */
	public static long toTicks(String humanReadableTime) {
		Valid.checkNotNull(humanReadableTime, "Time is null");

		long seconds = 0L;

		final String[] split = humanReadableTime.split(" ");

		for (int i = 1; i < split.length; i++) {
			final String sub = split[i].toLowerCase();
			int multiplier = 0; // e.g 2 hours = 2
			long unit = 0; // e.g hours = 3600
			boolean isTicks = false;

			try {
				multiplier = Integer.parseInt(split[i - 1]);
			} catch (final NumberFormatException e) {
				continue;
			}

			// attempt to match the unit time
			if (sub.startsWith("tick"))
				isTicks = true;

			else if (sub.startsWith("second"))
				unit = 1;

			else if (sub.startsWith("minute"))
				unit = 60;

			else if (sub.startsWith("hour"))
				unit = 3600;

			else if (sub.startsWith("day"))
				unit = 86400;

			else if (sub.startsWith("week"))
				unit = 604800;

			else if (sub.startsWith("month"))
				unit = 2629743;

			else if (sub.startsWith("year"))
				unit = 31556926;
			else
				throw new RuntimeException("Must define date type! Example: '1 second' (Got '" + sub + "')");

			seconds += multiplier * (isTicks ? 1 : unit * 20);
		}

		return seconds;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Formatting
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Return the current date formatted as DAY.MONTH.YEAR HOUR:MINUTES:SECONDS
	 *
	 * @return
	 */
	public static String getFormattedDate() {
		return getFormattedDate(System.currentTimeMillis());
	}

	/**
	 * Return the given date in millis formatted as
	 * DAY.MONTH.YEAR HOUR:MINUTES:SECONDS
	 *
	 * @param time
	 * @return
	 */
	public static String getFormattedDate(long time) {
		return DATE_FORMAT.format(time);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Converting
	// ------------------------------------------------------------------------------------------------------------

}
