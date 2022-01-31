package com.james090500.CoreFoundation.model;

import com.james090500.CoreFoundation.collection.SerializedMap;
import lombok.Data;

/**
 * Simple tuple for key-value pairs
 * @param <K>
 * @param <V>
 */
@Data
public final class Tuple<K, V> implements ConfigSerializable {

	/**
	 * The key
	 */
	private final K key;

	/**
	 * The value
	 */
	private final V value;

	/**
	 * @see ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		return SerializedMap.ofArray("Key", key, "Value", value);
	}
}
