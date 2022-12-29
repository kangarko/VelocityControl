package org.mineacademy.velocitycontrol.listener;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.mineacademy.velocitycontrol.VelocityControl;
import org.mineacademy.velocitycontrol.model.ProxyPacket;

import java.util.UUID;

/**
 * Represents a in/out message with a given action and server name
 * and a safety check for writing/reading the data
 * based on the action's content.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
abstract class Message {

	@Getter
	@Setter
	private String channelName;

	/**
	 * The UUID of the sender who initiated the packet, can be null
	 */
	@Getter
	private UUID senderUid;

	/**
	 * The server name
	 */
	@Getter
	private String serverName;

	/**
	 * The action
	 */
	@Getter
	private ProxyPacket action;

	/**
	 * The current position of writing the data based on the
	 * {@link ProxyPacket#getContent()}
	 */
	private int actionHead = 0;

	/**
	 * Set the sender UUID
	 *
	 * @param raw
	 */
	protected final void setSenderUid(String raw) {
		if (raw != null)
			try {
				this.senderUid = UUID.fromString(raw);
			} catch (final IllegalArgumentException ex) {
				throw new IllegalArgumentException("Expected UUID, got " + raw + " for packet " + this.action + " from server " + this.serverName);
			}
	}

	/**
	 * Set the server name for this message, reason it is here:
	 * cannot read in the constructor in {@link org.mineacademy.velocitycontrol.listener.OutgoingMessage}
	 *
	 * @param serverName
	 */
	protected final void setServerName(String serverName) {
		Preconditions.checkArgument(this.serverName == null, "Server name already set");
		Preconditions.checkNotNull(serverName, "Server name cannot be null!");

		this.serverName = serverName;
	}

	/**
	 * Set the action head for this message, reason it is here:
	 * static access in {@link org.mineacademy.velocitycontrol.listener.OutgoingMessage}
	 */
	protected final void setAction(String actionName) {
		final ProxyPacket action = ProxyPacket.getByName(actionName);

		Preconditions.checkNotNull(action, "Unknown action named: " + actionName + ".");
		setAction(action);
	}

	/**
	 * Set the action head for this message, reason it is here:
	 * static access in {@link OutgoingMessage}
	 *
	 * @param action
	 */
	protected final void setAction(ProxyPacket action) {
		Preconditions.checkArgument(this.action == null, "Action already set");

		this.action = action;
	}

	/**
	 * Ensures we are reading in the correct order as the given {@link ProxyPacket}
	 * specifies in its {@link ProxyPacket#getContent()} getter.
	 *
	 * This also ensures we are reading the correct data type (both primitives and wrappers
	 * are supported).
	 *
     */
	protected final void moveHead() {
		Preconditions.checkNotNull(serverName, "Server name not set!");
		Preconditions.checkNotNull(serverName, "Server name not set!");
		Preconditions.checkNotNull(action, "Action not set!");

		final Class<?>[] content = action.getContent();
		Preconditions.checkArgument(actionHead < content.length, "Head out of bounds! Max data size for " + action.name() + " is " + content.length);

		actionHead++;
	}

	/**
	 * Return the bungee channel, always returns
	 *
	 * @return
	 */
	public final ChannelIdentifier getChannel() {
		return VelocityControl.CHANNEL;
	}

}
