package org.mineacademy.velocitycontrol.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.james090500.CoreFoundation.collection.SerializedMap;

import java.io.ByteArrayInputStream;
import java.util.UUID;

public final class IncomingMessage extends Message {
    private final byte[] data;
    private final ByteArrayDataInput input;

    private final ByteArrayInputStream stream;

    public IncomingMessage(byte[] data) {
        this.data = data;
        this.stream = new ByteArrayInputStream(data);
        this.input = ByteStreams.newDataInput(this.stream);
        setSenderUid(this.input.readUTF());
        setServerName(this.input.readUTF());
        setAction(this.input.readUTF());
    }

    public UUID readUUID() {
        this.moveHead(String.class);
        return UUID.fromString(this.input.readUTF());
    }

    public SerializedMap readMap() {
        return SerializedMap.fromJson(this.readString());
    }

    public String readString() {
        this.moveHead(String.class);
        return this.input.readUTF();
    }

    public byte[] getData() {
        return this.data;
    }
}
