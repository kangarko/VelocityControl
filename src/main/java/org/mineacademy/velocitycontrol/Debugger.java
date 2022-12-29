package org.mineacademy.velocitycontrol;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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

        //Get Plugins
        StringBuilder plugins = new StringBuilder();
        VelocityControl.getServer().getPluginManager().getPlugins().forEach(pluginContainer -> plugins.append(pluginContainer.getDescription().getName().get() + " " + pluginContainer.getDescription().getVersion()));

        //Write out header and server info
        fill(lines,
                "------------------------------------[ " + TimeUtil.getFormattedDate() + " ]-----------------------------------",
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


}