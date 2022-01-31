package com.james090500.CoreFoundation.collection;

import com.google.gson.Gson;
import com.james090500.CoreFoundation.Common;
import com.james090500.CoreFoundation.Valid;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Serialized map enables you to save and retain values from your
 * configuration easily, such as locations, other maps or lists and
 * much more.
 */
public final class SerializedMap extends StrictCollection {

	/**
	 * The Google Json instance
	 */
	private final static Gson gson = new Gson();

	/**
	 * The internal map with values
	 */
	private final StrictMap<String, Object> map = new StrictMap<>();

	/**
	 * Creates a new serialized map with the given first key-value pair
	 *
	 * @param key
	 * @param value
	 */
	private SerializedMap(final String key, final Object value) {
		this();

		put(key, value);
	}

	public SerializedMap() {
		super("Cannot remove '%s' as it is not in the map!", "Value '%s' is already in the map!");
	}

	/**
	 * Put key-value pairs from another map into this map
	 * <p>
	 * If the key already exist, it is ignored
	 *
	 * @param anotherMap
	 * @return
	 */
	public SerializedMap mergeFrom(final SerializedMap anotherMap) {
		for (final Entry<String, Object> entry : anotherMap.entrySet()) {
			final String key = entry.getKey();
			final Object value = entry.getValue();

			if (key != null && value != null && !this.map.containsKey(key))
				this.map.put(key, value);
		}

		return this;
	}

	/**
	 * @param key
	 * @return
	 * @see Map#containsKey(Object)
	 */
	public boolean containsKey(final String key) {
		return map.containsKey(key);
	}

	/**
	 * Puts a key:value pair into the map only if the values are not null
	 *
	 * @param associativeArray
	 * @return
	 */
	public SerializedMap putArray(final Object... associativeArray) {
		boolean string = true;
		String lastKey = null;

		for (final Object obj : associativeArray) {
			if (string) {
				Valid.checkBoolean(obj instanceof String, "Expected String at " + obj + ", got " + obj.getClass().getSimpleName());

				lastKey = (String) obj;

			} else
				map.override(lastKey, obj);

			string = !string;
		}

		return this;
	}

	/**
	 * Add another map to this map
	 *
	 * @param anotherMap
	 * @return
	 */
	public SerializedMap put(@NonNull SerializedMap anotherMap) {
		map.putAll(anotherMap.asMap());

		return this;
	}

	/**
	 * Puts a new key-value pair in the map, failing if the value is null
	 * or if the old key exists
	 *
	 * @param key
	 * @param value
	 */
	public void put(final String key, final Object value) {
		Valid.checkNotNull(value, "Value with key '" + key + "' is null!");

		map.put(key, value);
	}

	/**
	 * @return
	 * @see Map#entrySet()
	 */
	public Set<Entry<String, Object>> entrySet() {
		return map.entrySet();
	}

	/**
	 * Get the Java map representation
	 *
	 * @return
	 */
	public Map<String, Object> asMap() {
		return map.getSource();
	}

	/**
	 * Convert this map into a serialized one (again, but iterating through each pair as well)
	 */
	@Override
	public Object serialize() {
		return map.serialize();
	}

	/**
	 * Converts this map into a JSON string
	 *
	 * @return
	 */
	public String toJson() {
		final Object map = serialize();

		try {
			return gson.toJson(map);

		} catch (final Throwable t) {
			Common.error(t, "Failed to serialize to json, data: " + map);

			return "{}";
		}
	}

	/**
	 * Convert the key pairs into formatted string such as {
	 * 	"key" = "value"
	 *  "another" = "value2"
	 *  ...
	 * }
	 *
	 * @return
	 */
	public String toStringFormatted() {
		final Map<?, ?> map = (Map<?, ?>) serialize();
		final List<String> lines = new ArrayList<>();

		lines.add("{");

		for (final Entry<?, ?> entry : map.entrySet()) {
			final Object value = entry.getValue();

			if (value != null && !value.toString().equals("[]") && !value.toString().equals("{}") && !value.toString().isEmpty() && !value.toString().equals("0.0") && !value.toString().equals("false"))
				lines.add("\t'" + entry.getKey() + "' = '" + entry.getValue() + "'");
		}

		lines.add("}");

		return String.join("\n", lines);
	}

	@Override
	public String toString() {
		return serialize().toString();
	}

	// ----------------------------------------------------------------------------------------------------
	// Static
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Create a new map with the first key-value pair
	 *
	 * @param key
	 * @param value
	 * @return
	 */
	public static SerializedMap of(final String key, final Object value) {
		return new SerializedMap(key, value);
	}

	/**
	 * Create new serialized map from key-value pairs like you would in PHP:
	 * <p>
	 * array(
	 * "name" => value,
	 * "name2" => value2,
	 * )
	 * <p>
	 * Except now you just use commas instead of =>'s
	 *
	 * @param array
	 * @return
	 */
	public static SerializedMap ofArray(final Object... array) {

		// If the first argument is a map already, treat as such
		if (array != null && array.length == 1) {
			final Object firstArgument = array[0];

			if (firstArgument instanceof SerializedMap)
				return (SerializedMap) firstArgument;

			if (firstArgument instanceof Map)
				return SerializedMap.of((Map<String, Object>) firstArgument);

			if (firstArgument instanceof StrictMap)
				return SerializedMap.of(((StrictMap<String, Object>) firstArgument).getSource());
		}

		final SerializedMap map = new SerializedMap();
		map.putArray(array);

		return map;
	}

	/**
	 * Converts the given Map into a serializable map
	 *
	 * @param map
	 * @return
	 */
	public static SerializedMap of(final Map<String, Object> map) {
		final SerializedMap serialized = new SerializedMap();

		serialized.map.clear();
		serialized.map.putAll(map);

		return serialized;
	}

	/**
	 * Attempts to parse the given JSON into a serialized map
	 * <p>
	 * Values are not deserialized right away, they are converted
	 * when you call get() functions
	 *
	 * @param json
	 * @return
	 */
	public static SerializedMap fromJson(final String json) {
		final SerializedMap serializedMap = new SerializedMap();

		try {
			final Map<String, Object> map = gson.fromJson(json, Map.class);

			serializedMap.map.putAll(map);

		} catch (final Throwable t) {
			Common.throwError(t, "Failed to parse JSON from " + json);
		}

		return serializedMap;
	}
}
