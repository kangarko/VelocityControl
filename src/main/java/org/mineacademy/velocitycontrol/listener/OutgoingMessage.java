package org.mineacademy.velocitycontrol.listener;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.mineacademy.velocitycontrol.VelocityControl;
import org.mineacademy.velocitycontrol.foundation.Debugger;
import org.mineacademy.velocitycontrol.foundation.exception.VCException;
import org.mineacademy.velocitycontrol.model.ProxyPacket;

import java.util.*;

public final class OutgoingMessage extends Message {
	private final List<Object> queue;

	public OutgoingMessage(ProxyPacket action) {
		this(UUID.fromString("00000000-0000-0000-0000-000000000000"), "", action);
	}

	public OutgoingMessage(UUID fromSenderUid, String fromServerName, ProxyPacket action) {
		this.queue = new ArrayList<>();
		setSenderUid(fromSenderUid.toString());
		setServerName(fromServerName);
		setAction(action);
		if (this.getChannelName() == null) {
			this.setChannelName("Null");
		}
		this.queue.add(this.getChannelName()); //Unused channel name
		this.queue.add(fromSenderUid);
		this.queue.add(getServerName());
		this.queue.add(getAction().name());
	}

	public void writeString(String... messages) {
		String[] var2 = messages;
		int var3 = messages.length;

		for(int var4 = 0; var4 < var3; ++var4) {
			String message = var2[var4];
			this.write(message);
		}

	}

	public void writeMap(HashMap<String, UUID> map) {
		Gson gson = new Gson();
		write(gson.toJson(map));
	}

	private void write(Object object) {
		Preconditions.checkNotNull(object, "Added object must not be null!");
		this.moveHead();
		this.queue.add(object);
	}

	public void send(RegisteredServer server) {
		server.sendPluginMessage(this.getChannel(), this.compileData());
		Debugger.debug("bungee", "Sending data on " + this.getChannel() + " channel from " + this.getAction() + " to " + server.getServerInfo() + " server.");
	}

	public byte[] compileData() {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();

		for (Object object : this.queue) {
			if (object instanceof String) {
				out.writeUTF((String) object);
			} else if (object instanceof Boolean) {
				out.writeBoolean((Boolean) object);
			} else if (object instanceof Byte) {
				out.writeByte((Byte) object);
			} else if (object instanceof Double) {
				out.writeDouble((Double) object);
			} else if (object instanceof Float) {
				out.writeFloat((Float) object);
			} else if (object instanceof Integer) {
				out.writeInt((Integer) object);
			} else if (object instanceof Long) {
				out.writeLong((Long) object);
			} else if (object instanceof Short) {
				out.writeShort((Short) object);
			} else if (object instanceof UUID) {
				out.writeUTF(object.toString());
			} else {
				if (!(object instanceof byte[])) {

					throw new VCException("Unsupported write of " + object.getClass().getSimpleName() + " to channel " + this.getChannel() + " with action " + this.getAction().toString());
				}

				out.write((byte[]) object);
			}
		}

		return out.toByteArray();
	}
}
