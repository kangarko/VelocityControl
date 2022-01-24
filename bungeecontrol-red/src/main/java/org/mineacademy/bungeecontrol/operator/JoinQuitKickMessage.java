package org.mineacademy.bungeecontrol.operator;

/**
 * Represents join, leave, kick or timed message broadcast
 */
public final class JoinQuitKickMessage extends PlayerMessage {

	/**
	 * Create a new broadcast by name
	 *
	 * @param type
	 * @param group
	 */
	public JoinQuitKickMessage(Type type, String group) {
		super(type, group);
	}
}
