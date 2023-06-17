package org.mineacademy.velocitycontrol.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.UUID;

public final class IncomingMessage extends Message {
    private final byte[] data;
    private final ByteArrayDataInput input;

    private final ByteArrayInputStream stream;

    public IncomingMessage(byte[] data) {
        this.data = data;
        this.stream = new ByteArrayInputStream(data);
        this.input = ByteStreams.newDataInput(this.stream);

        setChannelName(this.input.readUTF()); //Unused channel name
        setSenderUid(this.input.readUTF());
        setServerName(this.input.readUTF());
        setAction(this.input.readUTF());
    }

    public UUID readUUID() {
        this.moveHead();
        return UUID.fromString(this.input.readUTF());
    }

    public HashMap readMap() {
        Gson gson = new Gson();
        return gson.fromJson(this.readString(), HashMap.class);
    }

    public String readString() {
        this.moveHead();
        return this.input.readUTF();
    }

    public byte[] getData() {
        return this.data;
    }
}
