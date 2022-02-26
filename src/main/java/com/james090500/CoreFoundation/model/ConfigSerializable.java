package com.james090500.CoreFoundation.model;

import com.james090500.CoreFoundation.collection.SerializedMap;

/**
 * <p>Classes implementing this can be stored/loaded from a settings file</p>
 *
 * <p>** All classes must also implement the following: **</p>
 * <p>public static T deserialize(SerializedMap map)</p>
 */
public interface ConfigSerializable {
    /**
     * Creates a Map representation of this class that you can
     * save in your settings yaml or json file.
     *
     * @return Map containing the current state of this class
     */
    SerializedMap serialize();
}
