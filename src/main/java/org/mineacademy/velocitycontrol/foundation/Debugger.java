package org.mineacademy.velocitycontrol.foundation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.mineacademy.velocitycontrol.VelocityControl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for solving problems and errors
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Debugger {

    /**
     * Logs the error in the console and writes all details into the errors.log file
     *
     * @param t
     * @param messages
     */
    public static void saveError(Throwable t, String... messages) {
        final List<String> lines = new ArrayList<>();
        final String header = "VelocityControl encountered: " + t.getClass().getSimpleName();

        //Get Plugins
        StringBuilder plugins = new StringBuilder();
        VelocityControl.getServer().getPluginManager().getPlugins().forEach(pluginContainer -> plugins.append(pluginContainer.getDescription().getName().get() + " " + pluginContainer.getDescription().getVersion()));

        //Write out header and server info
        fill(lines,
                "------------------------------------[ " + SimpleDateFormat.getInstance().format(System.currentTimeMillis()) + " ]-----------------------------------",
                header,
                "Running Velocity " + VelocityControl.getServer().getVersion() + " and Java " + System.getProperty("java.version"),
				"Plugins: " + plugins,
                "----------------------------------------------------------------------------------------------");

        // Write additional data
        if (messages != null && !String.join("", messages).isEmpty()) {
            fill(lines, "\nMore Information: ");
            fill(lines, messages);
        }

        { // Write the stack trace

            do {
                // Write the error header
                String error = t.getMessage() != null ? t.getMessage() : t.getLocalizedMessage() != null ? t.getLocalizedMessage() : "(Unknown Cause)";
                fill(lines, t == null ? "Unknown error" : t.getClass().getSimpleName() + " " + error);

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
        VelocityControl.getLogger().error(header + "! Please check your error.log and report this issue with the information in that file.");

        // Finally, save the error file
        try {
            File errorFile = new File(VelocityControl.getFolder().toFile(), "error.log");
            Files.write(errorFile.toPath(), lines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void fill(List<String> list, String... messages) {
        list.addAll(Arrays.asList(messages));
    }

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

    private static boolean canPrint(String message) {
        return !message.contains("net.minecraft") && !message.contains("org.bukkit.craftbukkit") && !message.contains("nashorn") && !message.contains("javax.script");
    }

    // Print a simple console message
    public static void print(String message) {
        VelocityControl.getLogger().debug(message);
    }
}