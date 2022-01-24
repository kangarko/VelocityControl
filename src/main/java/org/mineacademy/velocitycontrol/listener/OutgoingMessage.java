package org.mineacademy.velocitycontrol.listener;


import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.mineacademy.bfo.Valid;
import org.mineacademy.bfo.bungee.BungeeAction;
import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.bfo.exception.FoException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class OutgoingMessage extends Message {
	private final List<Object> queue;

	public OutgoingMessage(BungeeAction action) {
		this(UUID.fromString("00000000-0000-0000-0000-000000000000"), "", action);
	}

	public OutgoingMessage(UUID fromSenderUid, String fromServerName, BungeeAction action) {
		this.queue = new ArrayList();
		this.setSenderUid(fromSenderUid.toString());
		this.setServerName(fromServerName);
		this.setAction(action);
		this.queue.add(fromSenderUid);
		this.queue.add(fromServerName);
		this.queue.add(action.name());
	}

	public void writeString(String... messages) {
		String[] var2 = messages;
		int var3 = messages.length;

		for(int var4 = 0; var4 < var3; ++var4) {
			String message = var2[var4];
			this.write(message, String.class);
		}

	}

	public void writeMap(SerializedMap map) {
		write(map.toJson(), String.class);
	}

	public void writeBoolean(boolean bool) {
		this.write(bool, Boolean.class);
	}

	public void writeByte(byte number) {
		this.write(number, Byte.class);
	}

	public void writeDouble(double number) {
		this.write(number, Double.class);
	}

	public void writeFloat(float number) {
		this.write(number, Float.class);
	}

	public void writeInt(int number) {
		this.write(number, Integer.class);
	}

	public void writeLong(long number) {
		this.write(number, Long.class);
	}

	public void writeShort(short number) {
		this.write(number, Short.class);
	}

	public void writeUUID(UUID uuid) {
		this.write(uuid, UUID.class);
	}

	private void write(Object object, Class<?> typeOf) {
		Valid.checkNotNull(object, "Added object must not be null!");
		this.moveHead(typeOf);
		this.queue.add(object);
	}

	public void send(ChannelMessageSink connection) {
		if (connection instanceof Player) {
			connection = ((Player) connection).getCurrentServer().orElse(null);
		}

		Valid.checkBoolean(connection instanceof ServerConnection, "Connection must be ServerConnection", new Object[0]);
		((ServerConnection)connection).sendPluginMessage(this.getChannel(), this.compileData());
		Debugger.debug("bungee", new String[]{"Sending data on " + this.getChannel() + " channel from " + this.getAction() + " to " + ((ServerConnection)connection).getServerInfo().getName() + " server."});
	}

	public void send(RegisteredServer server) {
		server.sendPluginMessage(this.getChannel(), this.compileData());
		Debugger.debug("bungee", new String[]{"Sending data on " + this.getChannel() + " channel from " + this.getAction() + " to " + server.getServerInfo() + " server."});
	}

	public byte[] compileData() {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		Iterator var2 = this.queue.iterator();

		while(var2.hasNext()) {
			Object object = var2.next();
			if (object instanceof String) {
				out.writeUTF((String)object);
			} else if (object instanceof Boolean) {
				out.writeBoolean((Boolean)object);
			} else if (object instanceof Byte) {
				out.writeByte((Byte)object);
			} else if (object instanceof Double) {
				out.writeDouble((Double)object);
			} else if (object instanceof Float) {
				out.writeFloat((Float)object);
			} else if (object instanceof Integer) {
				out.writeInt((Integer)object);
			} else if (object instanceof Long) {
				out.writeLong((Long)object);
			} else if (object instanceof Short) {
				out.writeShort((Short)object);
			} else if (object instanceof UUID) {
				out.writeUTF(object.toString());
			} else {
				if (!(object instanceof byte[])) {
					throw new FoException("Unsupported write of " + object.getClass().getSimpleName() + " to channel " + this.getChannel() + " with action " + this.getAction().toString());
				}

				out.write((byte[])object);
			}
		}

		return out.toByteArray();
	}
}
