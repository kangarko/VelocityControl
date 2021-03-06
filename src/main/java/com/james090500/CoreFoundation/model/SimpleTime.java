package com.james090500.CoreFoundation.model;

import com.james090500.CoreFoundation.TimeUtil;
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
		timeTicks = (int) TimeUtil.toTicks(time);
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
}