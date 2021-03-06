package com.james090500.CoreFoundation.model;

import com.james090500.CoreFoundation.Common;
import com.james090500.CoreFoundation.ReflectionUtil;
import com.james090500.CoreFoundation.collection.expiringmap.ExpiringMap;
import com.james090500.CoreFoundation.exception.EventHandledException;
import com.james090500.CoreFoundation.remain.Remain;
import com.velocitypowered.api.proxy.Player;
import lombok.NonNull;
import org.mineacademy.velocitycontrol.VelocityControl;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * An engine that compiles and executes code on the fly.
 * <p>
 * The code is based off JavaScript with new Java methods, see:
 * https://winterbe.com/posts/2014/04/05/java8-nashorn-tutorial/
 */
public final class JavaScriptExecutor {

	/**
	 * The engine singleton
	 */
	private static final ScriptEngine engine;

	/**
	 * Cache scripts for 1 second per player for highest performance
	 * <p>
	 * Player -> Map of scripts and their results
	 */
	private static final Map<UUID, Map<String, Object>> resultCache = ExpiringMap.builder().expiration(1, TimeUnit.SECONDS).build();

	// Load the engine
	static {
		Thread.currentThread().setContextClassLoader(VelocityControl.class.getClassLoader());

		ScriptEngineManager engineManager = new ScriptEngineManager();
		ScriptEngine scriptEngine = engineManager.getEngineByName("Nashorn");

		// Workaround for newer Minecraft releases, still unsure what the cause is
		if (scriptEngine == null) {
			engineManager = new ScriptEngineManager(null);

			scriptEngine = engineManager.getEngineByName("Nashorn");
		}

		// If still fails, try to load our own library for Java 15 and up
		if (scriptEngine == null) {
			final String nashorn = "org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory";

			if (ReflectionUtil.isClassAvailable(nashorn)) {
				final ScriptEngineFactory engineFactory = ReflectionUtil.instantiate(ReflectionUtil.lookupClass(nashorn));

				engineManager.registerEngineName("Nashorn", engineFactory);
				scriptEngine = engineManager.getEngineByName("Nashorn");
			}
		}

		engine = scriptEngine;

		if (engine == null) {
			final List<String> warningMessage = Common.newList(
					"ERROR: JavaScript placeholders will not function!",
					"",
					"Your Java version/distribution lacks the",
					"Nashorn library for JavaScript placeholders.");

			if (Remain.getJavaVersion() >= 15)
				warningMessage.addAll(Arrays.asList(
						"",
						"To fix this, install the NashornPlus",
						"plugin from mineacademy.org/nashorn"));
			else
				warningMessage.addAll(Arrays.asList(
						"",
						"To fix this, install Java 11 from Oracle",
						"or other vendor that supports Nashorn."));

			Common.logFramed(false, Common.toArray(warningMessage));
		}
	}

	/**
	 * Compiles and executes the given JavaScript code
	 *
	 * @param javascript
	 * @return
	 */
	public static Object run(final String javascript) {
		return run(javascript, (Player) null);
	}
	/**
	 * Compiles and executes the Javascript code for the player ("player" variable is put into the JS code)
	 * as well as the bukkit event (use "event" variable there)
	 *
	 * @param javascript
	 * @param sender
	 * @return
	 */
	public static Object run(@NonNull String javascript, final Player sender) {

		// Cache for highest performance
		Map<String, Object> cached = resultCache.get((sender).getUniqueId());

		if (cached != null) {
			final Object result = cached.get(javascript);

			if (result != null)
				return result;
		}

		if (engine == null) {
			Common.warning("Not running script" + (sender != null ? " for " + sender.getUsername() : "") + " because JavaScript library is missing (install Oracle Java 8, 11 or use mineacademy.org/nashorn): " + javascript);

			return null;
		}

		try {
			engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();

			if (sender != null)
				engine.put("player", sender);

			final Object result = engine.eval(javascript);

			if (sender instanceof Player) {
				if (cached == null)
					cached = new HashMap<>();

				cached.put(javascript, result);
				resultCache.put(sender.getUniqueId(), cached);
			}

			return result;

		} catch (final Throwable ex) {
			final String message = ex.toString();
			String error = "Script execution failed for";

			if (message.contains("ReferenceError:") && message.contains("is not defined"))
				error = "Found invalid or unparsed variable in";

			// Special support for throwing exceptions in the JS code so that users
			// can send messages to player directly if upstream supports that
			if (ex.getCause() != null && ex.getCause().toString().contains("event handled: ")) {
				final String[] errorMessage = ex.getCause().toString().split("event handled\\: ");

				throw new EventHandledException(true, errorMessage.length == 2 ? errorMessage[1] : null);
			}

			throw new RuntimeException(error + " '" + javascript + "'", ex);
		}
	}

}