package com.james090500.CoreFoundation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for various reflection methods
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflectionUtil {

	/**
	 * Reflection utilizes a simple cache for fastest performance
	 */
	private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
	private static final Map<Class<?>, ReflectionData<?>> reflectionDataCache = new ConcurrentHashMap<>();
	private static final Collection<String> classNameGuard = ConcurrentHashMap.newKeySet();

	/**
	 * Makes a new instance of a class
	 *
	 * @param clazz
	 * @return
	 */
	public static <T> T instantiate(final Class<T> clazz) {
		try {
			final Constructor<T> constructor;

			if (reflectionDataCache.containsKey(clazz))
				constructor = ((ReflectionData<T>) reflectionDataCache.get(clazz)).getDeclaredConstructor();

			else
				constructor = clazz.getDeclaredConstructor();

			constructor.setAccessible(true);

			return constructor.newInstance();

		} catch (final ReflectiveOperationException e) {
			throw new ReflectionException("Could not make instance of: " + clazz, e);
		}
	}

	/**
	 * Return true if the given absolute class path is available,
	 * useful for checking for older MC versions for classes such as org.bukkit.entity.Phantom
	 *
	 * @param path
	 * @return
	 */
	public static boolean isClassAvailable(final String path) {
		try {
			if (classCache.containsKey(path))
				return true;

			Class.forName(path);

			return true;

		} catch (final Throwable t) {
			return false;
		}
	}

	/**
	 * Wrapper for Class.forName
	 * @param <T>
	 * @param path
	 *
	 * @return
	 */
	public static <T> Class<T> lookupClass(final String path) {
		if (classCache.containsKey(path))
			return (Class<T>) classCache.get(path);

		if (classNameGuard.contains(path)) {
			while (classNameGuard.contains(path)) {
				// Wait for other thread
			}

			return lookupClass(path); // Re run method to see if the cached value now exists.
		}

		try {
			classNameGuard.add(path);

			final Class<?> clazz = Class.forName(path);

			classCache.put(path, clazz);
			reflectionDataCache.computeIfAbsent(clazz, ReflectionData::new);

			return (Class<T>) clazz;

		} catch (final ClassNotFoundException ex) {
			throw new ReflectionException("Could not find class: " + path);

		} finally {
			classNameGuard.remove(path);
		}
	}

	/* ------------------------------------------------------------------------------- */
	/* Classes */
	/* ------------------------------------------------------------------------------- */

	private static final class ReflectionData<T> {
		private final Class<T> clazz;

		ReflectionData(final Class<T> clazz) {
			this.clazz = clazz;
		}

		private final Map<Integer, Constructor<?>> constructorCache = new ConcurrentHashMap<>();
		private final Collection<Integer> constructorGuard = ConcurrentHashMap.newKeySet();

		public void cacheConstructor(final Constructor<T> constructor) {
			final List<Class<?>> classes = new ArrayList<>();

			for (final Class<?> param : constructor.getParameterTypes()) {
				Valid.checkNotNull(param, "Argument cannot be null when instatiating " + clazz);

				classes.add(param);
			}

			constructorCache.put(Arrays.hashCode(classes.toArray(new Class<?>[0])), constructor);
		}

		public Constructor<T> getDeclaredConstructor(final Class<?>... paramTypes) throws NoSuchMethodException {
			final Integer hashCode = Arrays.hashCode(paramTypes);

			if (constructorCache.containsKey(hashCode))
				return (Constructor<T>) constructorCache.get(hashCode);

			if (constructorGuard.contains(hashCode)) {
				while (constructorGuard.contains(hashCode)) {

				} // Wait for other thread;
				return getDeclaredConstructor(paramTypes);
			}

			constructorGuard.add(hashCode);

			try {
				final Constructor<T> constructor = clazz.getDeclaredConstructor(paramTypes);

				cacheConstructor(constructor);

				return constructor;

			} finally {
				constructorGuard.remove(hashCode);
			}
		}

	}

	/**
	 * Represents an exception during reflection operation
	 */
	public static final class ReflectionException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public ReflectionException(final String msg) {
			super(msg);
		}

		public ReflectionException(final String msg, final Exception ex) {
			super(msg, ex);
		}
	}

}
