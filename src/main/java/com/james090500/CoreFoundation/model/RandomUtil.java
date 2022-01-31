package com.james090500.CoreFoundation.model;

import com.james090500.CoreFoundation.Common;
import com.james090500.CoreFoundation.Valid;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Utility class for generating random numbers.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RandomUtil {

	/**
	 * The random instance for this class
	 */
	private static final Random random = new Random();

	/**
	 * Returns a random integer, see {@link Random#nextInt(int)}
	 *
	 * @param boundExclusive
	 * @return
	 */
	public static int nextInt(final int boundExclusive) {
		Valid.checkBoolean(boundExclusive > 0, "Getting a random number must have the bound above 0, got: " + boundExclusive);

		return random.nextInt(boundExclusive);
	}

    /**
	 * Return a random item in list
	 *
	 * @param <T>
	 * @param items
	 * @return
	 */
	public static <T> T nextItem(final Iterable<T> items) {
		return nextItem(items, null);
	}

	/**
	 * Return a random item in list only among those that match the given condition
	 *
	 * @param <T>
	 * @param items
	 * @param condition the condition applying when selecting
	 * @return
	 */
	public static <T> T nextItem(final Iterable<T> items, final Predicate<T> condition) {
		final List<T> list = items instanceof List ? new ArrayList<>((List<T>) items) : Common.toList(items);

		// Remove values failing the condition
		if (condition != null)
			for (final Iterator<T> it = list.iterator(); it.hasNext();) {
				final T item = it.next();

				if (!condition.test(item))
					it.remove();
			}

		return list.get(nextInt(list.size()));
	}
}
