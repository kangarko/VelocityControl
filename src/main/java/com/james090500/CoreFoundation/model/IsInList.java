package com.james090500.CoreFoundation.model;

import com.james090500.CoreFoundation.collection.StrictSet;
import lombok.Getter;

import java.util.Collection;

/**
 * A simple class allowing you to match if something is in that list.
 * <p>
 * Example: The list contains "apple", "red", "car",
 * you call isInList("car") and you get true. Same for any other data type
 * <p>
 * If you create new IsInList("*") or with an empty list, everything will be matched
 *
 * @param <T>
 */
public final class IsInList<T> {

	/**
	 * The internal set for matching
	 */
	@Getter
	private final StrictSet<T> list;

	/**
	 * Is everything matched?
	 */
	private final boolean matchAll;

	/**
	 * Create a new matching list
	 *
	 * @param list
	 */
	public IsInList(final StrictSet<T> list) {
		this(list.getSource());
	}

	/**
	 * Create a new matching list
	 *
	 * @param list
	 */
	public IsInList(final Collection<T> list) {
		this.list = new StrictSet<>(list);

		if (list.isEmpty())
			matchAll = true;

		else if (list.iterator().next().equals("*"))
			matchAll = true;

		else
			matchAll = false;
	}

	/**
	 * Return true if the given value is in this list
	 *
	 * @param toEvaluateAgainst
	 * @return
	 */
	public boolean contains(final T toEvaluateAgainst) {
		return matchAll || list.contains(toEvaluateAgainst);
	}
}
