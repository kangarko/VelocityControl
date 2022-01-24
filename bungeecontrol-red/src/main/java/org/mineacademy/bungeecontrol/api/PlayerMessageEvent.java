package org.mineacademy.bungeecontrol.api;

import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.bungeecontrol.operator.PlayerMessage;
import org.mineacademy.bungeecontrol.operator.Operator.OperatorCheck;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;

/**
 * An event that is executed when a player joins a channel.
 */
@Getter
@RequiredArgsConstructor
public final class PlayerMessageEvent extends Event implements Cancellable {

	/**
	 * The Dude himself
	 */
	private final ProxiedPlayer player;

	/**
	 * The channel player is joining into
	 */
	private final PlayerMessage.Type type;

	/**
	 * The associated check with this
	 */
	private final OperatorCheck<?> check;

	/**
	 * The variables such as {to_server} etc. used for the event
	 */
	
	private final SerializedMap variables;

	/**
	 * Enable joining?
	 */
	@Setter
	private boolean cancelled;
}