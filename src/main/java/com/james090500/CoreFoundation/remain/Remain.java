package com.james090500.CoreFoundation.remain;

import com.james090500.CoreFoundation.exception.FoException;

public final class Remain {

    /**
     * Converts an unchecked exception into checked
     *
     * @param throwable
     */
    public static void sneaky(final Throwable throwable) {
        try {
            SneakyThrow.sneaky(throwable);

        } catch (final NoClassDefFoundError | NoSuchFieldError | NoSuchMethodError err) {
            throw new FoException(throwable);
        }
    }

    /**
     * Return the corresponding major Java version such as 8 for Java 1.8, or 11 for Java 11.
     *
     * @return
     */
    public static int getJavaVersion() {
        String version = System.getProperty("java.version");

        if (version.startsWith("1."))
            version = version.substring(2, 3);

        else {
            final int dot = version.indexOf(".");

            if (dot != -1)
                version = version.substring(0, dot);
        }

        if (version.contains("-"))
            version = version.split("\\-")[0];

        return Integer.parseInt(version);
    }
}

/**
 * A wrapper for Spigot
 */
class SneakyThrow {

    public static void sneaky(final Throwable t) {
        throw SneakyThrow.<RuntimeException>superSneaky(t);
    }

    private static <T extends Throwable> T superSneaky(final Throwable t) throws T {
        throw (T) t;
    }
}
