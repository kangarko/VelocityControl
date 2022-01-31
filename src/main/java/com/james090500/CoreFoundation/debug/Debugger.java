package com.james090500.CoreFoundation.debug;

import com.james090500.CoreFoundation.Common;
import com.james090500.CoreFoundation.FileUtil;
import com.james090500.CoreFoundation.TimeUtil;
import lombok.*;
import org.mineacademy.velocitycontrol.VelocityControl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for solving problems and errors
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Debugger {

	@Getter
	@Setter
	private static boolean debugAll = false;

	// ----------------------------------------------------------------------------------------------------
	// Saving errors to file
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Logs the error in the console and writes all details into the errors.log file
	 *
	 * @param t
	 * @param messages
	 */
	public static void saveError(Throwable t, String... messages) {
		final List<String> lines = new ArrayList<>();
		final String header = "VelocityControl encountered " + Common.article(t.getClass().getSimpleName());

		// Write out header and server info
		fill(lines,
				"------------------------------------[ " + TimeUtil.getFormattedDate() + " ]-----------------------------------",
				header,
				"Running Velocity " + VelocityControl.getServer().getVersion() + " and Java " + System.getProperty("java.version"),
//				"Plugins: " + Common.join(VelocityControl.getServer().getPluginManager().getPlugins(), ", ", plugin -> plugin.getDescription().getName() + " " + plugin.getDescription().getVersion()),
				"----------------------------------------------------------------------------------------------");

		// Write additional data
		if (messages != null && !String.join("", messages).isEmpty()) {
			fill(lines, "\nMore Information: ");
			fill(lines, messages);
		}

		{ // Write the stack trace

			do {
				// Write the error header
				fill(lines, t == null ? "Unknown error" : t.getClass().getSimpleName() + " " + Common.getOrDefault(t.getMessage(), Common.getOrDefault(t.getLocalizedMessage(), "(Unknown cause)")));

				int count = 0;

				for (final StackTraceElement el : t.getStackTrace()) {
					count++;

					final String trace = el.toString();

					if (count > 6 && trace.startsWith("net.minecraft.server"))
						break;

					fill(lines, "\t at " + el.toString());
				}
			} while ((t = t.getCause()) != null);
		}

		fill(lines, "----------------------------------------------------------------------------------------------", System.lineSeparator());

		// Log to the console
		Common.log(header + "! Please check your error.log and report this issue with the information in that file.");

		// Finally, save the error file
		FileUtil.write("error.log", lines);
	}

	private static void fill(List<String> list, String... messages) {
		list.addAll(Arrays.asList(messages));
	}

	// ----------------------------------------------------------------------------------------------------
	// Utility methods
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Prints a Throwable's first line and stack traces.
	 *
	 * Ignores the native Bukkit/Minecraft server.
	 *
	 * @param throwable the throwable to print
	 */
	public static void printStackTrace(@NonNull Throwable throwable) {
		print(throwable.toString());

		for (final StackTraceElement element : throwable.getStackTrace()) {
			final String line = element.toString();

			if (canPrint(line))
				print("\tat " + line);
		}

		Throwable cause = throwable.getCause();

		if (cause != null)
			do
				if (cause != null)
					printStackTrace(cause);
			while ((cause = cause.getCause()) != null);
	}

	/**
	 * Returns whether a line is suitable for printing as an error line - we ignore stuff from NMS and OBF as this is not needed
	 *
	 * @param message
	 * @return
	 */
	private static boolean canPrint(String message) {
		return !message.contains("net.minecraft") && !message.contains("org.bukkit.craftbukkit") && !message.contains("nashorn") && !message.contains("javax.script");
	}

	// Print a simple console message
	public static void print(String message) {
		VelocityControl.getLogger().debug(message);
	}

	/**
	 *
	 *  The old debugger methods
	 *
	 */

	public static void debug(String a, String b) {
		VelocityControl.getLogger().debug("[" + a + "] " + b);
	}

	public static void debug(String a, String[] b) {
		for (String v : b) debug(a, v);
	}
}
