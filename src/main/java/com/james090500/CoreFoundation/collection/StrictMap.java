package com.james090500.CoreFoundation.collection;

import com.james090500.CoreFoundation.SerializeUtil;
import com.james090500.CoreFoundation.Valid;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Strict map that only allows to remove elements that are contained within, or add elements that are not.
 * <p>
 * Failing to do so results in an error, with optional error message.
 * @param <E>
 * @param <T>
 */
public final class StrictMap<E, T> extends StrictCollection {

	private final Map<E, T> map = new LinkedHashMap<>();

	public StrictMap() {
		this("Cannot remove '%s' as it is not in the map!", "Key '%s' is already in the map --> '%s'");
	}

	public StrictMap(String removeMessage, String addMessage) {
		super(removeMessage, addMessage);
	}

	public void put(E key, T value) {
		Valid.checkBoolean(!map.containsKey(key), String.format(getCannotAddMessage(), key, map.get(key)));

		override(key, value);
	}

	public void putAll(Map<? extends E, ? extends T> m) {
		for (final Entry<? extends E, ? extends T> e : m.entrySet())
			Valid.checkBoolean(!map.containsKey(e.getKey()), String.format(getCannotAddMessage(), e.getKey(), map.get(e.getKey())));

		override(m);
	}

	public void override(E key, T value) {
		map.put(key, value);
	}

	public void override(Map<? extends E, ? extends T> m) {
		map.putAll(m);
	}

	/**
	 * Will return the key as normal or put it there and return it.
	 * @param key
	 * @param defaultToPut
	 * @return
	 */
	public T getOrPut(E key, T defaultToPut) {
		if (containsKey(key))
			return get(key);

		put(key, defaultToPut);
		return defaultToPut;
	}

    /**
	 * CAN BE NULL, NO EXCEPTION THROWING
	 * @param key
	 * @return
	 */
	public T get(E key) {
		return map.get(key);
	}

	public T getOrDefault(E key, T def) {
		return map.getOrDefault(key, def);
	}

	public boolean containsKey(E key) {
		return key == null ? false : map.containsKey(key);
	}

    public Set<Entry<E, T>> entrySet() {
		return map.entrySet();
	}

	public void clear() {
		map.clear();
	}

	public Map<E, T> getSource() {
		return map;
	}

	@Override
	public Object serialize() {
		if (!map.isEmpty()) {
			final Map<Object, Object> copy = new HashMap<>();

			for (final Entry<E, T> e : entrySet()) {
				final T val = e.getValue();

				if (val != null)
					copy.put(SerializeUtil.serialize(e.getKey()), SerializeUtil.serialize(val));
			}

			return copy;
		}

		return getSource();
	}

	@Override
	public String toString() {
		return map.toString();
	}
}