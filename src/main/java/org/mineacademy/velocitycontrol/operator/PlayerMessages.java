package org.mineacademy.velocitycontrol.operator;

import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import org.mineacademy.velocitycontrol.VelocityControl;
import org.mineacademy.velocitycontrol.api.PlayerMessageEvent;
import org.mineacademy.velocitycontrol.foundation.RuleSetReader;
import org.mineacademy.velocitycontrol.operator.Operator.OperatorCheck;
import org.mineacademy.velocitycontrol.operator.PlayerMessage.PlayerMessageCheck;
import org.mineacademy.velocitycontrol.operator.PlayerMessage.Type;
import org.mineacademy.velocitycontrol.settings.Settings;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Represents the core engine for player message broadcasting
 */
public final class PlayerMessages extends RuleSetReader<PlayerMessage> {

	@Getter
	private static final PlayerMessages instance = new PlayerMessages();

	/**
	 * The loaded items sorted by group
	 */
	private final Map<JoinQuitKickMessage.Type, List<PlayerMessage>> messages = new HashMap<>();

	/*
	 * Create this class
	 */
	private PlayerMessages() {
		super("group");
	}

	/**
	 * Reloads the content of this class.
	 */
	@Override
	public void load() {
		this.messages.clear();

		for (final JoinQuitKickMessage.Type type : PlayerMessage.Type.values())
			this.messages.put(type, loadFromFile("messages/" + type.getKey() + ".rs"));
	}

	@Override
	protected PlayerMessage createRule(File file, String value) {
		String fileName = file.getName().replace(".rs", "");
		final JoinQuitKickMessage.Type type = PlayerMessage.Type.fromKey(fileName);

		return new JoinQuitKickMessage(type, value);

	}

	/**
	 * Return immutable collection of all loaded broadcasts
	 *
	 * @param type
	 * @param <T>
	 *
	 * @return
	 */
	public <T extends PlayerMessage> List<T> getMessages(JoinQuitKickMessage.Type type) {
		return (List<T>) Collections.unmodifiableList(this.messages.get(type));
	}

	/* ------------------------------------------------------------------------------- */
	/* Static */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Broadcast the given message type from the given sender and the original message
	 * ONLY if it's enabled through settings.
	 *
	 * @param type
	 * @param player
	 * @param variables
	 */
	public static void broadcast(PlayerMessage.Type type, Player player, HashMap<String, String> variables) {
		if (Settings.getSettings().Messages.Apply_On.contains(type)) {
			final OperatorCheck<?> check = new JoinQuitKickCheck(type, player, variables);
			final PlayerMessageEvent event = new PlayerMessageEvent(player, type, check, variables);

			VelocityControl.getServer().getEventManager().fire(event).thenApply(r -> {
				if (r.getResult().isAllowed()) {
					if (type == Type.JOIN) {
						VelocityControl.getServer().getScheduler().buildTask(VelocityControl.getInstance(), check::start)
						.delay((int) Settings.getSettings().Messages.Defer_Join_Message_By, TimeUnit.MILLISECONDS).schedule();
					} else {
						check.start();
					}
				}
				return r;
			});

		}
	}

	/* ------------------------------------------------------------------------------- */
	/* Classes */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Represents a singular broadcast
	 */
	public static final class JoinQuitKickCheck extends PlayerMessageCheck<PlayerMessage> {

		private final List<PlayerMessage> messages;

		/*
		 * Create new constructor with handy objects
		 */
		private JoinQuitKickCheck(Type type, Player player, HashMap<String, String> variables) {
			super(type, player, variables);

			this.messages = PlayerMessages.getInstance().getMessages(type);
		}

		/**
		 * @see Operator.OperatorCheck#getOperators()
		 */
		@Override
		public List<PlayerMessage> getOperators() {
			return this.messages;
		}
	}
}
