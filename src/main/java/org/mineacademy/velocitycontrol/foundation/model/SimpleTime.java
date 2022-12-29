package org.mineacademy.velocitycontrol.foundation.model;

import com.google.common.base.Preconditions;
import lombok.Getter;

/**
 * A simple class holding time values in human readable form such as 1 second or 5 minutes
 */
@Getter
public final class SimpleTime {

	private final String raw;
	private final int timeTicks;

	private SimpleTime(final String time) {
		raw = time;
		timeTicks = (int) toTicks(time);
	}

    /**
	 * Generate new time. Valid examples: 15 ticks 1 second 25 minutes 3 hours etc.
	 *
	 * @param time
	 * @return
	 */
	public static SimpleTime from(final String time) {
		return new SimpleTime(time);
	}

	/**
	 * Get the time specified in seconds (ticks / 20)
	 *
	 * @return
	 */
	public int getTimeSeconds() {
		return timeTicks / 20;
	}

    @Override
	public String toString() {
		return raw;
	}

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
		Preconditions.checkNotNull(humanReadableTime, "Time is null");

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
}