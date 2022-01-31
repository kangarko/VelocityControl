package com.james090500.CoreFoundation;

import com.james090500.CoreFoundation.collection.StrictCollection;
import com.james090500.CoreFoundation.collection.StrictMap;
import com.james090500.CoreFoundation.exception.FoException;
import com.james090500.CoreFoundation.model.ConfigSerializable;
import com.james090500.CoreFoundation.model.IsInList;
import com.james090500.CoreFoundation.model.SimpleTime;
import com.velocitypowered.api.proxy.Player;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Utility class for serializing objects to writeable YAML data and back.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SerializeUtil {

	/**
	 * When serializing unknown objects throw an error if strict mode is enabled
	 */
	public static boolean STRICT_MODE = true;

	// ------------------------------------------------------------------------------------------------------------
	// Converting objects into strings so you can save them in your files
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Converts the given object into something you can safely save in file as a string
	 *
	 * @param obj
	 * @return
	 */
	public static Object serialize(final Object obj) {
		if (obj == null)
			return null;

		if (obj instanceof ConfigSerializable)
			return serialize(((ConfigSerializable) obj).serialize().serialize());

		else if (obj instanceof StrictCollection)
			return serialize(((StrictCollection) obj).serialize());
//
//		else if (obj instanceof ChatColor)
//			return ((ChatColor) obj).name();

		else if (obj instanceof UUID)
			return obj.toString();

		else if (obj instanceof Enum<?>)
			return obj.toString();

		else if (obj instanceof Player)
			return ((Player) obj).getUsername();

		else if (obj instanceof SimpleTime)
			return ((SimpleTime) obj).getRaw();

		else if (obj instanceof Iterable || obj.getClass().isArray() || obj instanceof IsInList) {
			final List<Object> serialized = new ArrayList<>();

			if (obj instanceof Iterable || obj instanceof IsInList)
				for (final Object element : obj instanceof IsInList ? ((IsInList<?>) obj).getList() : (Iterable<?>) obj)
					serialized.add(serialize(element));
			else
				for (final Object element : (Object[]) obj)
					serialized.add(serialize(element));

			return serialized;
		} else if (obj instanceof StrictMap) {
			final StrictMap<Object, Object> oldMap = (StrictMap<Object, Object>) obj;
			final StrictMap<Object, Object> newMap = new StrictMap<>();

			for (final Map.Entry<Object, Object> entry : oldMap.entrySet())
				newMap.put(serialize(entry.getKey()), serialize(entry.getValue()));

			return newMap;
		} else if (obj instanceof Map) {
			final Map<Object, Object> oldMap = (Map<Object, Object>) obj;
			final Map<Object, Object> newMap = new HashMap<>();

			for (final Map.Entry<Object, Object> entry : oldMap.entrySet())
				newMap.put(serialize(entry.getKey()), serialize(entry.getValue()));

			return newMap;

		}

		else if (obj instanceof Integer || obj instanceof Double || obj instanceof Float || obj instanceof Long
				|| obj instanceof String || obj instanceof Boolean || obj instanceof Map)
			return obj;

		if (STRICT_MODE)
			throw new FoException("Does not know how to serialize " + obj.getClass().getSimpleName() + "! Does it extends ConfigSerializable? Data: " + obj);

		else
			return Objects.toString(obj);
	}

	/**
	 * Runs through each item in the list and serializes it
	 * <p>
	 * Returns a new list of serialized items
	 *
	 * @param array
	 * @return
	 */
	public static List<Object> serializeList(final Iterable<?> array) {
		final List<Object> list = new ArrayList<>();

		for (final Object t : array)
			list.add(serialize(t));

		return list;
	}

}
