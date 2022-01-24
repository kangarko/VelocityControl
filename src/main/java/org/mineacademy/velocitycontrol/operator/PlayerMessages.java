package org.mineacademy.velocitycontrol.operator;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.velocitypowered.api.proxy.Player;
import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.FileUtil;
import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.bfo.model.RuleSetReader;
import org.mineacademy.velocitycontrol.VelocityControl;
import org.mineacademy.velocitycontrol.api.PlayerMessageEvent;
import org.mineacademy.velocitycontrol.operator.Operator.OperatorCheck;
import org.mineacademy.velocitycontrol.operator.PlayerMessage.PlayerMessageCheck;
import org.mineacademy.velocitycontrol.operator.PlayerMessage.Type;
import org.mineacademy.velocitycontrol.settings.Settings;

import lombok.Getter;

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
		final JoinQuitKickMessage.Type type = PlayerMessage.Type.fromKey(FileUtil.getFileName(file));

		return new JoinQuitKickMessage(type, value);

	}

	/**
	 * Attempt to find a rule by name
	 *
	 * @param type
	 * @param group
	 *
	 * @return
	 */
	public PlayerMessage findMessage(JoinQuitKickMessage.Type type, String group) {
		for (final PlayerMessage item : getMessages(type))
			if (item.getGroup().equalsIgnoreCase(group))
				return item;

		return null;
	}

	/**
	 * Return all player message names
	 * @param type
	 *
	 * @return
	 */
	public Set<String> getMessageNames(JoinQuitKickMessage.Type type) {
		return Common.convertSet(this.getMessages(type), PlayerMessage::getGroup);
	}

	/**
	 * Return all player message that are also enabled in Apply_On in settings
	 *
	 * @param type
	 * @return
	 */
	public Set<String> getEnabledMessageNames(JoinQuitKickMessage.Type type) {
		return Common.convertSet(this.getMessages(type).stream().filter(message -> Settings.getSettings().Messages.Apply_On.contains(message.getType())).collect(Collectors.toList()), PlayerMessage::getGroup);
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
	public static void broadcast(PlayerMessage.Type type, Player player, SerializedMap variables) {

		if (Settings.getSettings().Messages.Apply_On.contains(type)) {

			final OperatorCheck<?> check = new JoinQuitKickCheck(type, player, variables);
			final PlayerMessageEvent event = new PlayerMessageEvent(player, type, check, variables);

			VelocityControl.getServer().getEventManager().fire(event).thenApply(r -> {
				if (r.getResult().isAllowed()) {
					if (type == Type.JOIN)
						VelocityControl.getServer().getScheduler().buildTask(VelocityControl.getInstance(), check::start)
						.delay((int) Settings.getSettings().Messages.Defer_Join_Message_By, TimeUnit.MILLISECONDS);

					else
						check.start();
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
		private JoinQuitKickCheck(PlayerMessage.Type type, Player player, SerializedMap variables) {
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

		@Override
		protected Player getMessagePlayerForVariables() {
			return this.sender;
		}
	}
}
