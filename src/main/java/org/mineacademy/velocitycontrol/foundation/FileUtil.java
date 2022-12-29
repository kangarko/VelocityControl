package org.mineacademy.velocitycontrol.foundation;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.mineacademy.velocitycontrol.VelocityControl;
import org.mineacademy.velocitycontrol.foundation.exception.VCException;

import java.io.*;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Utility class for managing files.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUtil {

    /**
     * Return the name of the file from the given path, stripping
     * any extension and folders.
     *
     * Example: classes/Archer.yml will only return Archer
     *
     * @param file
     * @return
     */
    public static String getFileName(File file) {
        return getFileName(file.getName());
    }

    /**
     * Return the name of the file from the given path, stripping
     * any extension and folders.
     *
     * Example: classes/Archer.yml will only return Archer
     *
     * @param path
     * @return
     */
    public static String getFileName(String path) {
        Preconditions.checkArgument(path != null && !path.isEmpty(), "The given path must not be empty!");

        int pos = path.lastIndexOf("/");

        if (pos > 0)
            path = path.substring(pos + 1, path.length());

        pos = path.lastIndexOf(".");

        if (pos > 0)
            path = path.substring(0, pos);

        return path;
    }

    // ----------------------------------------------------------------------------------------------------
    // Getting files
    // ----------------------------------------------------------------------------------------------------

    /**
     * Returns a file from the given path inside our plugin folder, creating
     * the file if it does not exist
     *
     * @param path
     * @return
     */
    public static File getOrMakeFile(String path) {
        final File file = getFile(path);

        return file.exists() ? file : createFile(path);
    }

    /**
     * Create a new file in our plugin folder, supporting multiple directory paths
     *
     * Example: logs/admin/console.log or worlds/nether.yml are all valid paths
     *
     * @param path
     * @return
     */
    private static File createFile(String path) {
        final File datafolder = VelocityControl.getFolder().toFile();
        final int lastIndex = path.lastIndexOf('/');
        final File directory = new File(datafolder, path.substring(0, lastIndex >= 0 ? lastIndex : 0));

        directory.mkdirs();

        final File destination = new File(datafolder, path);

        try {
            destination.createNewFile();

        } catch (final IOException ex) {
            VelocityControl.getLogger().error("Failed to create a new file " + path);

            ex.printStackTrace();
        }

        return destination;
    }

    /**
     * Return a file in a path in our plugin folder, file may not exist
     *
     * @param path
     * @return
     */
    public static File getFile(String path) {
        return new File(VelocityControl.getFolder().toFile(), path);
    }

    // ----------------------------------------------------------------------------------------------------
    // Reading
    // ----------------------------------------------------------------------------------------------------

    /**
     * Return all lines in the file, failing if the file does not exists
     *
     * @param file
     * @return
     */
    public static List<String> readLines(File file) {
        Preconditions.checkNotNull(file, "File cannot be null");
        Preconditions.checkArgument(file.exists(), "File: " + file + " does not exists!");

        try {
            return Files.readAllLines(Paths.get(file.toURI()), StandardCharsets.UTF_8);

        } catch (final IOException ex) {

            // Older method, missing libraries
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                final List<String> lines = new ArrayList<>();
                String line;

                while ((line = br.readLine()) != null)
                    lines.add(line);

                return lines;

            } catch (final IOException ee) {
                throw new VCException(ee, "Could not read lines from " + file.getName());
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // Writing
    // ----------------------------------------------------------------------------------------------------

    /**
     * Write a line to file with optional prefix which can be null.
     *
     * The line will be as follows: [date] prefix msg
     *
     * @param to     	path to the file inside the plugin folder
     * @param prefix 	optional prefix, can be null
     * @param message   line, is split by \n
     */
    public static void writeFormatted(String to, String prefix, String message) {
        message = Common.stripColors(message).trim();

        if (!message.equalsIgnoreCase("none") && !message.isEmpty())
            for (final String line : message.split("\n"))
                if (!line.isEmpty())
                    write(to, "[" + SimpleDateFormat.getInstance().format(System.currentTimeMillis()) + "] " + (prefix != null ? prefix + ": " : "") + line);
    }

    /**
     * Write lines to a file path in our plugin directory,
     * creating the file if it does not exist, appending lines at the end
     *
     * @param to
     * @param lines
     */
    public static void write(String to, String... lines) {
        write(to, Arrays.asList(lines));
    }

    /**
     * Write lines to a file path in our plugin directory,
     * creating the file if it does not exist, appending lines at the end
     *
     * @param to
     * @param lines
     */
    public static void write(String to, Collection<String> lines) {
        write(getOrMakeFile(to), lines, StandardOpenOption.APPEND);
    }

    /**
     * Write the given lines to file
     *
     * @param to
     * @param lines
     * @param options
     */
    public static void write(File to, Collection<String> lines, StandardOpenOption... options) {
        try {
            final Path path = Paths.get(to.toURI());

            try {
                Files.write(path, lines, StandardCharsets.UTF_8, options);

            } catch (final ClosedByInterruptException ex) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(to, true))) {
                    for (final String line : lines)
                        bw.append(System.lineSeparator() + line);

                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (final Exception ex) {
            VelocityControl.getLogger().error("Failed to write to " + to);

            ex.printStackTrace(); // do not throw our exception since it would cause an infinite loop if there is a problem due to error writing
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // Extracting from our plugin .jar file
    // ----------------------------------------------------------------------------------------------------

    /**
     * Copy file from our plugin jar to destination.
     * No action is done if the file already exists.
     *
     * @param path the path to the file inside the plugin
     *
     * @return the extracted file
     */
    public static File extract(String path) {
        return extract(false, path, path, null);
    }

    public static File extract(boolean override, String from, String to, Function<String, String> replacer) {
        final InputStream is = getInternalResource((from.contains("/") ? "" : "/") + from);
        Preconditions.checkNotNull(is, "Inbuilt file not found: " + from);

        return extract(override, is, to, replacer);
    }

    /**
     * Copy file from our plugin jar to destination - customizable destination file
     * name.
     *
     * @param override always extract file even if already exists?
     * @param is
     * @param to       the path where the file will be copyed inside the plugin
     *                 folder
     * @param replacer the variables replacer
     *
     * @return the extracted file
     */
    public static File extract(boolean override, @NonNull InputStream is, String to, Function<String, String> replacer) {
        File file = new File(VelocityControl.getFolder().toFile(), to);

        if (!override && file.exists())
            return file;

        file = createFile(to);

        try {
            final List<String> lines = new ArrayList<>();

            // Load lines from internal file and replace them
            try (final BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;

                while ((line = br.readLine()) != null) {
                    line = replacer != null ? replacer.apply(line) : line;

                    lines.add(line);
                }
            }

            Files.write(file.toPath(), lines, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (final IOException ex) {
            Common.error(ex,
                    "Failed to extract file to " + to,
                    "Error: %error");
        }

        return file;
    }

    /**
     * Return an internal resource within our plugin's jar file
     *
     * @param path
     * @return the resource input stream, or null if not found
     */
    public static InputStream getInternalResource(String path) {
        // First attempt
        final InputStream is = VelocityControl.class.getResourceAsStream("/" + path);
        return is;
    }

    // ----------------------------------------------------------------------------------------------------
    // Checksums
    // ----------------------------------------------------------------------------------------------------

}