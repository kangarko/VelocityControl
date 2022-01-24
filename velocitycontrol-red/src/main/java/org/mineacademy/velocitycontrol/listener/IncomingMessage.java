package org.mineacademy.velocitycontrol.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.mineacademy.bfo.CompressUtil;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.velocitycontrol.VelocityControl;

import java.util.Iterator;
import java.util.UUID;

public final class IncomingMessage extends Message {
    private final byte[] data;
    private final ByteArrayDataInput input;

    public IncomingMessage(byte[] data) {
        this.data = data;
        this.input = ByteStreams.newDataInput(data);
        this.setSenderUid(this.input.readUTF());
        this.setServerName(this.input.readUTF());
        this.setAction(this.input.readUTF());
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

    public boolean readBoolean() {
        this.moveHead(Boolean.class);
        return this.input.readBoolean();
    }

    public byte readByte() {
        this.moveHead(Byte.class);
        return this.input.readByte();
    }

    public double readDouble() {
        this.moveHead(Double.class);
        return this.input.readDouble();
    }

    public float readFloat() {
        this.moveHead(Float.class);
        return this.input.readFloat();
    }

    public int readInt() {
        this.moveHead(Integer.class);
        return this.input.readInt();
    }

    public long readLong() {
        this.moveHead(Long.class);
        return this.input.readLong();
    }

    public short readShort() {
        this.moveHead(Short.class);
        return this.input.readShort();
    }

    public void forward(ChannelMessageSink connection) {
        Valid.checkBoolean(connection instanceof ServerConnection, "Connection must be ServerConnection", new Object[0]);
        connection.sendPluginMessage(this.getChannel(), this.data);
    }

    public void forwardToOthers() {
        Iterator var1 = VelocityControl.getServers().iterator();

        while (var1.hasNext()) {
            RegisteredServer server = (RegisteredServer) var1.next();
            if (!server.getServerInfo().getName().equals(this.getServerName())) {
                this.forward(server);
            }
        }

    }

    public void forwardToAll() {
        Iterator var1 = VelocityControl.getServers().iterator();

        while (var1.hasNext()) {
            RegisteredServer server = (RegisteredServer) var1.next();
            this.forward(server);
        }

    }

    public void forward(RegisteredServer server) {
        VelocityControl.getServer().getScheduler().buildTask(VelocityControl.getInstance(), () -> dispatchMessage(server));
    }

    private void dispatchMessage(RegisteredServer server) {
        server.sendPluginMessage(VelocityControl.CHANNEL, this.data);
    }

    public byte[] getData() {
        return this.data;
    }
}
