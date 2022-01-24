package org.mineacademy.velocitycontrol.model;

import lombok.Getter;
import org.mineacademy.bfo.bungee.BungeeAction;
import org.mineacademy.bfo.collection.SerializedMap;

import java.util.UUID;

/**
 * Proprietary implementation of BungeeAction for some of our
 * premium plugins handled by BungeeControl
 *
 * The BungeeCord protocol always begins with
 *
 * 1) The UUID of the sender from which we send the packet, or null
 * 2) The sender server name
 * 3) The {@link BungeeAction}
 *
 * and the rest is the actual data within this enum
 */
public enum ProxyPacket implements BungeeAction {

	/**
	 * Remove the given message from the players screen if he has received it.
	 */
	REMOVE_MESSAGE_BY_UUID(String.class /* remove mode */, UUID.class /*message id*/, Boolean.class /* async */
	),

	/**
	 * Clears the game chat
	 */
	CLEAR_CHAT(String.class /*broacast message*/
	),

	/**
	 * Forward commands to BungeeCord or other server
	 */
	FORWARD_COMMAND(String.class /*server*/, String.class /*command*/
	),

	/**
	 * Update mute status
	 */
	MUTE(String.class /*type*/, String.class /*object such as channel name*/, Boolean.class /*mute or unmute*/, String.class /*announce message*/
	),

	/**
	 * Send a sound to a player
	 */
	SOUND(String.class /*receiver UUID*/, String.class /*simple sound raw*/
	),

	/**
	 * Broadcast server alias set on BungeeCord downstream
	 */
	SERVER_ALIAS(String.class /* server name */, String.class /* server alias */
	),

	/**
	 * Used to display join/switch messages
	 */
	CONFIRM_PLAYER_READY(UUID.class /* player uuid */, String.class /* synced data line */
	),

	// ----------------------------------------------------------------------------------------------------
	// Messages
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Broadcast a message in a channel.
	 */
	CHANNEL(String.class /*channel*/, String.class /*sender name*/, UUID.class /*sender uid*/, String.class /*message*/, String.class /*simplecomponent json*/, String.class /*console format*/, Long.class /*discord channel id*/, String.class /*discord message*/, Boolean.class /*mute bypass*/, Boolean.class /*ignore bypass*/, Boolean.class /*log bypass*/
	),

	/**
	 * Broadcast message to spying players
	 */
	SPY(String.class /*spy type*/, String.class /*channel name*/, String.class /*message*/, String.class /*simplecomponent json*/, String.class /*json string list of UUIDs of players we should ignore*/
	),

	/**
	 * Send a toast message
	 */
	TOAST(UUID.class /*receiver UUID*/, String.class /*toggle type*/, String.class /*message*/, String.class /*compmaterial*/
	),

	/**
	 * Send announcement message
	 */
	ANNOUNCEMENT(String.class /*type*/, String.class /*message*/, String.class /*json data*/
	),

	/**
	 * Broadcast the /me command
	 */
	ME(UUID.class /*sender uuid*/, Boolean.class /*has reach bypass perm*/, String.class /*simplecomponent json*/
	),

	/**
	 * Send motd to the given receiver
	 */
	MOTD(String.class /*receiver uuid*/
	),

	/**
	 * Rules notify handling
	 */
	NOTIFY(String.class /*permission*/, String.class /*simplecomponent json*/
	),

	/**
	 * Send a plain message to all fools
	 */
	PLAIN_BROADCAST(String.class /*message to broadcast*/
	),

	/**
	 * Send a plain message to the given receiver
	 */
	PLAIN_MESSAGE(UUID.class /*receiver*/, String.class /*message*/
	),

	/**
	 * Very simple component message to receiver
	 */
	SIMPLECOMPONENT_MESSAGE(UUID.class /*receiver*/, String.class /*message json*/
	),

	/**
	 * Broadcast this BaseComponent to all online players
	 */
	JSON_BROADCAST(String.class /*message json*/
	),

	// ----------------------------------------------------------------------------------------------------
	// Data gathering
	// ----------------------------------------------------------------------------------------------------

	/**
	 * Indicates MySQL has changed for player and we need pulling it again
	 */
	DB_UPDATE(String.class /*player name*/, String.class /*player UUID*/, String.class /*data JSON*/, String.class /*message to player*/
	),

	/**
	 * Indicates that the player should have his reply player
	 * updated
	 */
	REPLY_UPDATE(UUID.class /* player to update uuid */, String.class /* reply player name */, UUID.class /* reply player uuid */
	),

	/**
	 * This will sync one mail to BungeeCord.
	 * Sent after MAIL_SYNC_START.
	 */
	MAIL_SYNC(String.class /*mail as json*/
	),

	/**
	 * Sync of data between BungeeCord servers (name-uuid map)
	 */
	PLAYERS_CLUSTER_HEADER(SerializedMap.class /*name-uuid map*/),

	/**
	 * Sync of data between servers using BungeeCord
	 */
	PLAYERS_CLUSTER_DATA(String.class, /*sync type*/
			String.class /*map*/
	),

	;

	/**
	 * Stores all valid values, the names of them are only used
	 * in the error message when the length of data does not match
	 */
	@Getter
	private final Class<?>[] content;

	/**
	 * Constructs a new bungee action
	 *
	 * @param validValues
	 */
	ProxyPacket(final Class<?>... validValues) {
		this.content = validValues;
	}

	public static ProxyPacket getByName(String name) {
		ProxyPacket[] actions = values();

		for (ProxyPacket action : actions) {
			if (action.name().equals(name)) {
				return action;
			}
		}

		return null;
	}
}