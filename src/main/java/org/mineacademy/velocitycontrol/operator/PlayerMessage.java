package org.mineacademy.velocitycontrol.operator;

import com.james090500.CoreFoundation.Common;
import com.james090500.CoreFoundation.Valid;
import com.james090500.CoreFoundation.collection.SerializedMap;
import com.james090500.CoreFoundation.debug.Debugger;
import com.james090500.CoreFoundation.exception.EventHandledException;
import com.james090500.CoreFoundation.model.*;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.mineacademy.velocitycontrol.VelocityControl;
import org.mineacademy.velocitycontrol.settings.Settings;

import java.util.*;

/**
 * Represents an operator that has require/ignore for both sender and receiver
 * Used for join/leave/kick/death messages yo
 */
@Getter
public abstract class PlayerMessage extends Operator implements Rule {

	/**
	 * The type of this message
	 */
	@Getter
	private final Type type;

	/**
	 * The name of this message group
	 */
	private final String group;

	/**
	 * Permission required for the player that caused the rule to fire in
	 * order for the rule to apply
	 */

	private Tuple<String, String> requireSenderPermission;

	/**
	 * Permission required for receivers of the message of the rule
	 */

	private Tuple<String, String> requireReceiverPermission;

	/**
	 * JavaScript boolean output required to be true for the rule to apply
	 */

	private String requireSenderScript;

	/**
	 * JavaScript boolean output required to be true for the rule to apply
	 */

	private String requireReceiverScript;

	/**
	 * The server to require for sender
	 */
	private String requireSenderServer;

	/**
	 * The server to require for receiver
	 */
	private String requireReceiverServer;

	/**
	 * Should the message only be sent to the sending player?
	 */
	private boolean requireSelf;

	/**
	 * Should the message not be sent to the sending player?
	 */
	private boolean ignoreSelf;

	/**
	 * Permission to bypass the rule
	 */

	private String ignoreSenderPermission;

	/**
	 * Permission to bypass the rule
	 */

	private String ignoreReceiverPermission;

	/**
	 * JavaScript boolean output when true for the rule to bypass
	 */

	private String ignoreSenderScript;

	/**
	 * JavaScript boolean output when true for the rule to bypass
	 */

	private String ignoreReceiverScript;

	/**
	 * The server to ignore for sender
	 */
	private String ignoreSenderServer;

	/**
	 * The server to ignore for receiver
	 */
	private String ignoreReceiverServer;

	/**
	 * The suffix for the {@link #messages}
	 */
	private String prefix;

	/**
	 * The suffix for the {@link #messages}
	 */
	private String suffix;

	@Getter
	private final List<String> messages = new ArrayList<>();

	/*
	 * A special flag to indicate we are about to load messages
	 */
	private boolean loadingMessages = false;

	/*
	 * Used to compute messages
	 */
	private int lastMessageIndex = 0;

	protected PlayerMessage(Type type, String group) {
		this.type = type;
		this.group = group;
	}

	/**
	 * Return the next message in a cyclic repetition
	 *
	 * @return
	 */
	public final String getNextMessage() {
		Valid.checkBoolean(!this.messages.isEmpty(), "Messages must be set on " + this);

		if (this.lastMessageIndex >= this.messages.size())
			this.lastMessageIndex = 0;

		return this.messages.get(this.lastMessageIndex++);
	}

	/**
	 * Return the prefix or the default one if not set
	 *
	 * @return the prefix
	 */
	public String getPrefix() {
		return Common.getOrDefault(this.prefix, Settings.getSettings().Messages.Prefix.get(this.type));
	}

	@Override
	public final String getUid() {
		return this.group;
	}

	/**
	 * @see Operator#onParse(java.lang.String, java.lang.String, java.lang.String[])
	 */
	@Override
	protected boolean onParse(String firstThreeParams, String theRestThree, String[] args) {

		firstThreeParams = Common.joinRange(0, 3, args, " ");
		theRestThree = Common.joinRange(3, args);

		if (this.loadingMessages) {
			final String everything = String.join(" ", args).trim();

			if (everything.startsWith("- ")) {
				String line = everything.substring(1).trim();

				if (line.startsWith("\"") || line.startsWith("'"))
					line = line.substring(1);

				if (line.endsWith("\"") || line.endsWith("'"))
					line = line.substring(0, line.length() - 1);

				this.messages.add(line);

			} else {
				Valid.checkBoolean(!this.messages.isEmpty(), "Enter messages with '-' on each line. Got: " + everything);

				// Merge the line that does not start with "-", assume it is used
				// for multiline messages:
				// - first line
				//   second line
				//   third line etc.
				final int index = this.messages.size() - 1;
				final String lastMessage = this.messages.get(index) + "\n" + everything;

				this.messages.set(index, lastMessage);
			}

			return true;
		}

		final String line = Common.joinRange(1, args);

		if ("prefix".equals(args[0])) {
			if (this.prefix != null)
				this.prefix += "\n" + line;

			else
				this.prefix = line;
		} else if ("suffix".equals(args[0])) {
			if (this.suffix != null)
				this.suffix += "\n" + line;

			else
				this.suffix = line;
		} else if ("message:".equals(args[0]) || "messages:".equals(args[0])) {
			Valid.checkBoolean(!this.loadingMessages, "Operator messages: can only be used once in " + this);

			this.loadingMessages = true;
		} else if ("require sender perm".equals(firstThreeParams) || "require sender permission".equals(firstThreeParams)) {
			checkNotSet(this.requireSenderPermission, "require sender perm");
			final String[] split = theRestThree.split(" ");

			this.requireSenderPermission = new Tuple<>(split[0], split.length > 1 ? Common.joinRange(1, split) : null);
		} else if ("require receiver perm".equals(firstThreeParams) || "require receiver permission".equals(firstThreeParams)) {
			checkNotSet(this.requireReceiverPermission, "require receiver perm");
			final String[] split = theRestThree.split(" ");

			this.requireReceiverPermission = new Tuple<>(split[0], split.length > 1 ? Common.joinRange(1, split) : null);
		} else if ("require sender script".equals(firstThreeParams)) {
			checkNotSet(this.requireSenderScript, "require sender script");

			this.requireSenderScript = theRestThree;
		} else if ("require receiver script".equals(firstThreeParams)) {
			checkNotSet(this.requireReceiverScript, "require receiver script");

			this.requireReceiverScript = theRestThree;
		} else if ("require sender server".equals(firstThreeParams)) {
			checkNotSet(this.requireSenderServer, "require sender server");

			this.requireSenderServer = theRestThree;
		} else if ("require receiver server".equals(firstThreeParams)) {
			checkNotSet(this.requireReceiverServer, "require receiver server");

			this.requireReceiverServer = theRestThree;
		} else if ("require self".equals(Common.joinRange(0, 2, args, " "))) {
			Valid.checkBoolean(!this.requireSelf, "'require self' option already set for " + this);

			this.requireSelf = true;
		} else if ("ignore self".equals(Common.joinRange(0, 2, args, " "))) {
			Valid.checkBoolean(!this.ignoreSelf, "'ignore self' option already set for " + this);

			this.ignoreSelf = true;
		} else if ("ignore sender perm".equals(firstThreeParams) || "ignore sender permission".equals(firstThreeParams)) {
			checkNotSet(this.ignoreSenderPermission, "ignore sender perm");

			this.ignoreSenderPermission = theRestThree;
		} else if ("ignore receiver perm".equals(firstThreeParams) || "ignore receiver permission".equals(firstThreeParams)) {
			checkNotSet(this.ignoreReceiverPermission, "ignore receiver perm");

			this.ignoreReceiverPermission = theRestThree;
		} else if ("ignore sender script".equals(firstThreeParams)) {
			checkNotSet(this.ignoreSenderScript, "ignore sender script");
			this.ignoreSenderScript = theRestThree;
		} else if ("ignore receiver script".equals(firstThreeParams)) {
			checkNotSet(this.ignoreReceiverScript, "ignore receiver script");

			this.ignoreReceiverScript = theRestThree;
		} else if ("ignore sender server".equals(firstThreeParams)) {
			checkNotSet(this.ignoreSenderServer, "ignore sender server");

			this.ignoreSenderServer = theRestThree;
		} else if ("ignore receiver server".equals(firstThreeParams)) {
			checkNotSet(this.ignoreReceiverServer, "ignore receiver server");

			this.ignoreReceiverServer = theRestThree;
		} else
			return false;

		return true;
	}

	/**
	 * Collect all options we have to debug
	 *
	 * @return
	 */
	@Override
	protected SerializedMap collectOptions() {
		return SerializedMap.ofArray(
			"Group", this.group,
			"Prefix", this.prefix,
			"Suffix", this.suffix,
			//"Bungee", this.bungee,
			"Messages", this.messages,

			"Require Sender Permission", this.requireSenderPermission,
			"Require Sender Script", this.requireSenderScript,
				/*"Require Sender Gamemodes", this.requireSenderGamemodes,
				"Require Sender Worlds", this.requireSenderWorlds,
				"Require Sender Regions", this.requireSenderRegions,
				"Require Sender Channels", this.requireSenderChannels,
				
				"Require Receiver Permission", this.requireReceiverPermission,
				"Require Receiver Script", this.requireReceiverScript,
				"Require Receiver Gamemodes", this.requireReceiverGamemodes,
				"Require Receiver Worlds", this.requireReceiverWorlds,
				"Require Receiver Regions", this.requireReceiverRegions,
				"Require Receiver Channels", this.requireReceiverChannels,*/

			"Require Self", this.requireSelf,
			"Ignore Self", this.ignoreSelf,
			//"Ignore Match", this.ignoreMatch,

			"Ignore Sender Permission", this.ignoreSenderPermission,
			"Ignore Sender Script", this.ignoreSenderScript//,
		/*"Ignore Sender Regions", this.ignoreSenderRegions,
		"Ignore Sender Gamemodes", this.ignoreSenderGamemodes,
		"Ignore Sender Worlds", this.ignoreSenderWorlds,
		"Ignore Sender Channels", this.ignoreSenderChannels,
		
		"Ignore Receiver Permission", this.ignoreReceiverPermission,
		"Ignore Receiver Regions", this.ignoreReceiverRegions,
		"Ignore Receiver Script", this.ignoreReceiverScript,
		"Ignore Receiver Gamemodes", this.ignoreReceiverGamemodes,
		"Ignore Receiver Worlds", this.ignoreReceiverWorlds,
		"Ignore Receiver Channels", this.ignoreReceiverChannels*/

		);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Player Message " + super.collectOptions().put(SerializedMap.of("Type", this.type)).toStringFormatted();
	}

	/* ------------------------------------------------------------------------------- */
	/* Classes */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Represents a check that is implemented by this class
	 * @param <T>
	 */
	public static abstract class PlayerMessageCheck<T extends PlayerMessage> extends OperatorCheck<T> {

		/**
		 * The message type
		 */
		protected final Type type;

		/**
		 * Players who have seen at least one message (we prevent players
		 * from seeing more than one message at a time)
		 */
		private final Set<UUID> messageReceivers = new HashSet<>();

		/**
		 * The current iterated receiver
		 */
		protected Player receiver;

		/**
		 * Pick one message randomly from the list to show to all players equally
		 */
		protected String pickedMessage;

		/**
		 * Has this rule been run at least once? Used to prevent firing operators
		 * for the receiver the amount of times as the online player count.
		 */
		private boolean executed;

		protected PlayerMessageCheck(Type type, Player player, SerializedMap variables) {
			super(player, variables);

			this.type = type;
		}

		/**
		 * Set variables for the receiver when he is iterated and shown messages to
		 *
		 * @param receiver
		 */
		protected void setVariablesFor(@NonNull Player receiver) {
			this.receiver = receiver;
		}

		/**
		 * @see Operator.OperatorCheck#filter(Operator)
		 */
		@Override
		protected void filter(T message) throws EventHandledException {

			Debugger.debug("operator", "FILTERING " + message.getUid());

			// Delay
			if (message.getDelay() != null) {
				final SimpleTime time = message.getDelay().getKey();
				final long now = System.currentTimeMillis();

				// Round the number due to Bukkit scheduler lags
				final long delay = Math.round((now - message.getLastExecuted()) / 1000D);

				if (delay < time.getTimeSeconds()) {
					Debugger.debug("operator", "\tbefore delay: " + delay + " threshold: " + time.getTimeSeconds());

					return;
				}

				message.setLastExecuted(now);
			}

			boolean pickedMessage = false;

			for (final Player player : VelocityControl.getPlayers()) {
				if (this.sender != null) {
					if (message.isRequireSelf() && !this.sender.equals(player))
						continue;

					if (message.isIgnoreSelf() && this.sender.equals(player))
						continue;
				}

				if (this.messageReceivers.contains(player.getUniqueId()) && Settings.getSettings().Messages.Stop_On_First_Match) {
					Debugger.debug("operator", "\t" + player.getUsername() + " already received a message");

					continue;
				}

				this.setVariablesFor(player);

				// Filter for each player
				if (!canFilterMessage(message)) {
					Debugger.debug("operator", "\tcanFilterMessage returned false for " + player.getUsername());

					continue;
				}

				// Pick the message ONLY if it can be shown to at least ONE player
				if (!pickedMessage) {
					this.pickedMessage = message.getNextMessage();

					pickedMessage = true;
				}

				// Execute main operators
				executeOperators(message);
			}
		}

		/**
		 */
		protected boolean canFilterMessage(T operator) {
			Valid.checkNotNull(receiver, "receiver in canFilter == null");

			Debugger.debug("operator", "CAN FILTER message " + operator.getUid());

			// ----------------------------------------------------------------
			// Require
			// ----------------------------------------------------------------

			if (operator.getRequireSenderPermission() != null) {
				final String permission = operator.getRequireSenderPermission().getKey();
				final String noPermissionMessage = operator.getRequireSenderPermission().getValue();

				if (!this.sender.hasPermission(replaceVariables(permission, operator))) {
					if (noPermissionMessage != null) {
						sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(replaceVariables(noPermissionMessage, operator)));

						throw new EventHandledException(true);
					}

					Debugger.debug("operator", "\tno required sender permission");
					return false;
				}
			}

			if (operator.getRequireReceiverPermission() != null) {
				final String permission = operator.getRequireReceiverPermission().getKey();
				final String noPermissionMessage = operator.getRequireReceiverPermission().getValue();

				if (!this.receiver.hasPermission(replaceReceiverVariables(permission, operator))) {
					if (noPermissionMessage != null) {
						receiver.sendMessage(
							LegacyComponentSerializer.legacyAmpersand().deserialize(replaceReceiverVariables(noPermissionMessage, operator))
						);

						throw new EventHandledException(true);
					}

					Debugger.debug("operator", "\tno required receiver permission");
					return false;
				}
			}

			if (operator.getRequireSenderScript() != null) {
				final Object result = JavaScriptExecutor.run(replaceVariables(operator.getRequireSenderScript(), operator));

				if (result != null) {
					Valid.checkBoolean(result instanceof Boolean, "require sender script condition must return boolean not " + (result == null ? "null" : result.getClass()) + " for rule " + operator);

					if ((boolean) result == false) {
						Debugger.debug("operator", "\tno required sender script");

						return false;
					}
				}
			}

			if (operator.getRequireReceiverScript() != null) {
				final Object result = JavaScriptExecutor.run(replaceReceiverVariables(operator.getRequireReceiverScript(), operator));

				if (result != null) {
					Valid.checkBoolean(result instanceof Boolean, "require receiver script condition must return boolean not " + (result == null ? "null" : result.getClass()) + " for rule " + operator);

					if ((boolean) result == false) {
						Debugger.debug("operator", "\tno required receiver script");

						return false;
					}
				}
			}

			if (operator.getRequireSenderServer() != null && this.isPlayer && !this.player.getCurrentServer().get().getServerInfo().getName().equalsIgnoreCase(operator.getRequireSenderServer())) {
				Debugger.debug("operator", "\tno require sender server");

				return false;
			}

			if (operator.getRequireReceiverServer() != null && !this.receiver.getCurrentServer().get().getServerInfo().getName().equalsIgnoreCase(operator.getRequireReceiverServer())) {
				Debugger.debug("operator", "\tno require receiver server");

				return false;
			}


			// ----------------------------------------------------------------
			// Ignore
			// ----------------------------------------------------------------

			if (operator.getIgnoreSenderPermission() != null && this.sender.hasPermission(replaceVariables(operator.getIgnoreSenderPermission(), operator))) {
				Debugger.debug("operator", "\tignore sender permission found");

				return false;
			}

			if (operator.getIgnoreReceiverPermission() != null && this.receiver.hasPermission(replaceReceiverVariables(operator.getIgnoreReceiverPermission(), operator))) {
				Debugger.debug("operator", "\tignore receiver permission found");

				return false;
			}

			if (operator.getIgnoreSenderScript() != null) {
				final Object result = JavaScriptExecutor.run(replaceVariables(operator.getIgnoreSenderScript(), operator));

				if (result != null) {
					Valid.checkBoolean(result instanceof Boolean, "ignore sendre script condition must return boolean not " + (result == null ? "null" : result.getClass()) + " for rule " + operator);

					if ((boolean) result == true) {
						Debugger.debug("operator", "\tignore sender script found");

						return false;
					}
				}
			}

			if (operator.getIgnoreReceiverScript() != null) {
				final Object result = JavaScriptExecutor.run(replaceReceiverVariables(operator.getIgnoreReceiverScript(), operator));

				if (result != null) {
					Valid.checkBoolean(result instanceof Boolean, "ignore receiver script condition must return boolean not " + (result == null ? "null" : result.getClass()) + " for rule " + operator);

					if ((boolean) result == true) {
						Debugger.debug("operator", "\tignore receiver script found");

						return false;
					}
				}
			}

			if (operator.getIgnoreSenderServer() != null && this.isPlayer && this.player.getCurrentServer().get().getServerInfo().getName().equalsIgnoreCase(operator.getIgnoreSenderServer())) {
				Debugger.debug("operator", "\tignore sender server found");

				return false;
			}

			if (operator.getIgnoreReceiverServer() != null && this.receiver.getCurrentServer().get().getServerInfo().getName().equalsIgnoreCase(operator.getIgnoreReceiverServer())) {
				Debugger.debug("operator", "\tignore receiver server found");

				return false;
			}

			return true;
		}

		/**
		 * @see Operator.OperatorCheck#executeOperators(Operator)
		 */
		@Override
		protected void executeOperators(T operator) throws EventHandledException {

			// Use the same message for all players
			String replaceVariables = replaceVariables(this.pickedMessage, operator).replace("{player}", player.getUsername());

			TextComponent prefix = operator.getPrefix() != null ? LegacyComponentSerializer.legacyAmpersand().deserialize(operator.getPrefix()) : Component.text("");
			TextComponent suffix = operator.getSuffix() != null ? LegacyComponentSerializer.legacyAmpersand().deserialize(Common.getOrEmpty(operator.getSuffix())) : Component.text("");
			TextComponent message = LegacyComponentSerializer.legacyAmpersand().deserialize(replaceVariables);
			TextComponent replaced = prefix.append(message).append(suffix);

			receiver.sendMessage(replaced);

			// Register as received message
			this.messageReceivers.add(receiver.getUniqueId());

			if (!this.executed)
				super.executeOperators(operator);

			// Mark as executed, starting the first receiver
			this.executed = true;
		}

		/*
		 * Replace all kinds of check variables
		 */
		private final String replaceReceiverVariables(String message, T operator) {
			if (message == null)
				return null;

			return Replacer.replaceVariables(message, prepareVariables(operator));
		}
		@Override
		protected SerializedMap prepareVariables(T operator) {
			return super.prepareVariables(operator).putArray("broadcast_group", operator.getGroup());
		}
	}

	/**
	 * Represents a message type
	 */
	@RequiredArgsConstructor
	public enum Type {

		/**
		 * Join messages
		 */
		JOIN("join"),

		/**
		 * Leave messages
		 */
		QUIT("quit"),

		/**
		 * Kick messages
		 */
		SWITCH("switch");

		/**
		 * The saveable non-obfuscated key
		 */
		@Getter
		private final String key;

		/**
		 * Attempt to load a log type from the given config key
		 *
		 * @param key
		 * @return
		 */
		public static Type fromKey(String key) {
			for (final Type mode : values())
				if (mode.key.equalsIgnoreCase(key))
					return mode;

			throw new IllegalArgumentException("No such message type: " + key + ". Available: " + Common.join(values()));
		}

		/**
		 * Returns {@link #getKey()}
		 */
		@Override
		public String toString() {
			return this.key;
		}
	}
}
