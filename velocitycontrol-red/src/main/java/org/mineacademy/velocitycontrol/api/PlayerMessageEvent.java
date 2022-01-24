package org.mineacademy.velocitycontrol.api;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.Player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.velocitycontrol.operator.Operator.OperatorCheck;
import org.mineacademy.velocitycontrol.operator.PlayerMessage;

/**
 * An event that is executed when a player joins a channel.
 */
@Getter
@RequiredArgsConstructor
public final class PlayerMessageEvent implements ResultedEvent<ResultedEvent.GenericResult> {

	/**
	 * The Dude himself
	 */
	private final Player player;

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
	private GenericResult result = GenericResult.allowed();

	@Override
	public GenericResult getResult() {
		return result;
	}

	@Override
	public void setResult(GenericResult genericResult) {
		result = genericResult;
	}
}