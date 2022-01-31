package org.mineacademy.velocitycontrol.operator;

import com.james090500.CoreFoundation.Common;
import com.james090500.CoreFoundation.FileUtil;
import com.james090500.CoreFoundation.Valid;
import com.james090500.CoreFoundation.collection.SerializedMap;
import com.james090500.CoreFoundation.exception.EventHandledException;
import com.james090500.CoreFoundation.exception.RegexTimeoutException;
import com.james090500.CoreFoundation.model.*;
import com.velocitypowered.api.proxy.Player;
import lombok.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.mineacademy.velocitycontrol.SyncedCache;
import org.mineacademy.velocitycontrol.VelocityControl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Operator implements Rule {

	/**
	 * Represents the date formatting using to evaluate "expires" operator
	 *
	 * d MMM yyyy, HH:mm
	 */
	private final static DateFormat DATE_FORMATTING = new SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.ENGLISH);

	/**
	 * The time in the future when this broadcast no longer runs
	 */
	@Getter
	private long expires = -1;

	/**
	 * The delay between the next time this rule can be fired up, with optional warning message
	 */
	private Tuple<SimpleTime, String> delay;

	/**
	 * List of commands to run as player when rule matches
	 */
	private final List<String> playerCommands = new ArrayList<>();

	/**
	 * List of commands to send to BungeeCord to run when rule matches
	 */
	private final List<String> bungeeCommands = new ArrayList<>();

	/**
	 * List of messages to log
	 */
	private final List<String> consoleMessages = new ArrayList<>();

	/**
	 * Kick message that when set, rule will kick player
	 */

	private String kickMessage;

	/**
	 * Channel:Message map to send to Discord
	 */
	private final Map<String, String> discordMessages = new HashMap<>();

	/**
	 * File:Message messages to log
	 */
	private final Map<String, String> writeMessages = new HashMap<>();

	/**
	 * Map of messages to send back to player when rule matches
	 * They have unique ID assigned to prevent duplication
	 */
	private final Map<UUID, String> warnMessages = new LinkedHashMap<>();

	/**
	 * Should we abort checking more rules below this one?
	 */
	private boolean abort = false;

	/**
	 * Shall we cancel the event and not send the message at all?
	 */
	private boolean cancelMessage = false;

	/**
	 * Should we send the message only to the sender making him think it went through
	 * while hiding it from everyone else?
	 */
	private boolean cancelMessageSilently = false;

	/**
	 * Only fire this operator for the sender if he played before.
	 */
	private boolean requirePlayedBefore = false;

	/**
	 * Ignore this operator for the sender if he played before.
	 */
	private boolean ignorePlayedBefore = false;

	/**
	 * Should we exempt the rule from being logged?
	 */
	private boolean ignoreLogging = false;

	/**
	 * Prevent console catch information coming up?
	 */
	private boolean ignoreVerbose = false;

	/**
	 * Is this class (all operators here) temporary disabled?
	 */
	private boolean disabled;

	/**
	 * The time the operator was last executed
	 */
	@Setter(value = AccessLevel.PROTECTED)
	@Getter
	private long lastExecuted = -1;

	/**
	 * @see Rule#onOperatorParse(java.lang.String[])
	 */
	@Override
	public final boolean onOperatorParse(String[] args) {
		final String param = Common.joinRange(0, 2, args);
		final String theRest = Common.joinRange(args.length >= 2 ? 2 : 1, args);

		final List<String> theRestSplit = splitVertically(theRest);
		if ("expires".equals(args[0])) {
			Valid.checkBoolean(this.expires == -1, "Operator 'expires' already defined on " + this);

			String date = Common.joinRange(1, args);

			try {
				// Workaround to enable users put in both short and fully abbreviated month names
				final String[] months = new String[] { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
				final String[] fullNameMonths = new String[] { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };

				for (int i = 0; i < months.length; i++)
					date = date.replaceAll(months[i] + "\\b", fullNameMonths[i]);

				this.expires = DATE_FORMATTING.parse(date).getTime();

			} catch (final ParseException ex) {
				Common.throwError(ex, "Syntax error in 'expires' operator. Valid: dd MMM yyyy, HH:mm Got: " + date);
			}
		}

		else if ("delay".equals(args[0])) {
			checkNotSet(this.delay, "delay");

			try {
				final SimpleTime time = SimpleTime.from(Common.joinRange(1, 3, args));
				final String message = args.length > 2 ? Common.joinRange(3, args) : null;

				this.delay = new Tuple<>(time, message);

			} catch (final Throwable ex) {
				Common.throwError(ex, "Syntax error in 'delay' operator. Valid: <amount> <unit> (1 second, 2 minutes). Got: " + String.join(" ", args));
			}
		}

		else if ("then command".equals(param) || "then commands".equals(param))
			this.playerCommands.addAll(theRestSplit);

		else if ("then bungeeconsole".equals(param) || "then bungee".equals(param))
			this.bungeeCommands.addAll(theRestSplit);

		else if ("then log".equals(param))
			this.consoleMessages.addAll(theRestSplit);

		else if ("then kick".equals(param)) {
			checkNotSet(this.kickMessage, "then kick");

			this.kickMessage = theRest;
		}

		else if ("then discord".equals(param)) {
			final String[] split = theRest.split(" ");
			Valid.checkBoolean(split.length > 1, "wrong then discord syntax! Usage: <channel> <message>");

			final String channel = split[0];
			final String message = Common.joinRange(1, split);

			this.discordMessages.put(channel, message);
		}

		else if ("then write".equals(param)) {
			final String[] split = theRest.split(" ");
			Valid.checkBoolean(split.length > 1, "wrong 'then log' syntax! Usage: <file (without spaces)> <message>");

			final String file = split[0];
			final String message = Common.joinRange(1, split);

			this.writeMessages.put(file, message);
		}

		else if ("then warn".equals(param)) {
			this.warnMessages.put(UUID.randomUUID(), theRest);

		} else if ("then abort".equals(param)) {
			Valid.checkBoolean(this.abort == false, "then abort already used on " + this);

			this.abort = true;
		}

		else if ("then deny".equals(param)) {
			if ("silently".equals(theRest)) {
				Valid.checkBoolean(this.cancelMessageSilently == false, "then deny silently already used on " + this);

				this.cancelMessageSilently = true;

			} else {
				Valid.checkBoolean(this.cancelMessage == false, "then deny already used on " + this);

				this.cancelMessage = true;
			}
		}

		else if ("require playedbefore".equals(param)) {
			Valid.checkBoolean(this.requirePlayedBefore == false, "require playedbefore already used on " + this);

			this.requirePlayedBefore = true;
		}

		else if ("ignore playedbefore".equals(param)) {
			Valid.checkBoolean(this.ignorePlayedBefore == false, "ignore playedbefore already used on " + this);

			this.ignorePlayedBefore = true;
		}

		else if ("dont log".equals(param)) {
			Valid.checkBoolean(this.ignoreLogging == false, "dont log already used on " + this);

			this.ignoreLogging = true;
		}

		else if ("dont verbose".equals(param)) {
			Valid.checkBoolean(this.ignoreVerbose == false, "dont verbose already used on " + this);

			this.ignoreVerbose = true;
		}

		else if ("disabled".equals(args[0])) {
			Valid.checkBoolean(!this.disabled, "'disabled' already used on " + this);

			this.disabled = true;
		}

		else {
			final boolean success = onParse(param, theRest, args);

			Valid.checkBoolean(success, "Unrecognized operator '" + String.join(" ", args) + "' found in " + this);
		}

		return true;
	}

	/**
	 * Parses additional operators
	 *
	 * @param param
	 * @param theRest
	 * @param args
	 * @return
	 */
	protected abstract boolean onParse(String param, String theRest, String[] args);

	/**
	 * Check if the value is null or complains that the operator of the given type is already defined
	 *
	 * @param value
	 * @param type
	 */
	protected final void checkNotSet(Object value, String type) {
		Valid.checkBoolean(value == null, "Operator '" + type + "' already defined on " + this);
	}

	/**
	 * A helper method to split a message by |
	 * but ignore \| and replace it with | only.
	 *
	 * @param message
	 * @return
	 */
	protected static final List<String> splitVertically(String message) {
		final List<String> split = Arrays.asList(message.split("(?<!\\\\)\\|"));

		for (int i = 0; i < split.size(); i++)
			split.set(i, split.get(i).replace("\\|", "|"));

		return split;
	}

	/**
	 * Collect all options we have to debug
	 *
	 * @return
	 */
	protected SerializedMap collectOptions() {
		return SerializedMap.ofArray(
				//"Require Keys", this.requireData,
				//"Ignore Keys", this.ignoreData,
				//"Save Keys", this.saveData,
				"Expires", this.expires != -1 ? this.expires : null,
				"Delay", this.delay,
				"Player Commands", this.playerCommands,
				//"Console Commands", this.consoleCommands,
				"BungeeCord Commands", this.bungeeCommands,
				"Console Messages", this.consoleMessages,
				"Kick Message", this.kickMessage,
				//"Toast Message", this.toast,
				//"Notify Messages", this.notifyMessages,
				"Discord Message", this.discordMessages,
				"Log To File", this.writeMessages,
				//"Fine", this.fine,
				//"Warning Points", this.warningPoints,
				//"Sounds", this.sounds,
				//"Book", this.book,
				//"Title", this.title,
				//"Action Bar", this.actionBar,
				//"Boss Bar", this.bossBar == null ? null : this.bossBar.toString(),
				"Warn Messages", this.warnMessages,
				"Abort", this.abort,
				"Cancel Message", this.cancelMessage,
				"Cancel Message Silently", this.cancelMessageSilently,
				"Require Played Before", this.requirePlayedBefore,
				"Ignore Played Before", this.ignorePlayedBefore,
				//"Require Discord", this.requireDiscord,
				"Ignore Logging", this.ignoreLogging,
				"Ignore Verbose", this.ignoreVerbose,
				//"Ignore Discord", this.ignoreDiscord,
				"Disabled", this.disabled

		);
	}

	/**
	 * Return a tostring representation suitable to show in game
	 *
	 * @return
	 */
	public final String toDisplayableString() {
		return Common.revertColorizing(toString().replace("\t", "    "));
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public final boolean equals(Object obj) {
		return obj instanceof Operator && ((Operator) obj).getUid().equals(this.getUid());
	}

	/* ------------------------------------------------------------------------------- */
	/* Classes */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Represents a check that is implemented by this class
	 * @param <T>
	 */
	public abstract static class OperatorCheck<T extends Operator> {

		/**
		 * Variables available at all times
		 */
		private final SerializedMap variables;

		/**
		 * The sender involved in this check
		 */
		protected Player sender;

		/**
		 * Is the {@link #sender} a {@link Player}?
		 */
		protected boolean isPlayer;

		/**
		 * The player if {@link #isPlayer} is true
		 */
		protected Player player;

		/**
		 * Should we cancel the event silently and only send the message
		 * to the sender himself?
		 */
		@Getter
		protected boolean cancelledSilently;

		/**
		 * Construct check and useful parameters
		 */
		protected OperatorCheck(@NonNull Player sender, SerializedMap variables) {
			this.variables = variables;
			this.sender = sender;

			this.isPlayer = sender instanceof Player;
			this.player = isPlayer ? sender : null;
		}

		public final void start() {

			// Collect all to filter
			final List<T> operators = getOperators();

			// Iterate through all rules and parse
			for (final T operator : operators)
				try {
					if (!canFilter(operator))
						continue;

					filter(operator);

				} catch (final OperatorAbortException ex) {
					// Verbose
					if (!operator.isIgnoreVerbose())
						verbose("&cStopping further operator check.");

					break;

				} catch (final EventHandledException ex) {
					throw ex; // send upstream

				} catch (final RegexTimeoutException ex) {
					ex.printStackTrace();

				} catch (final Throwable t) {
					Common.throwError(t, "Error parsing rule: " + operator);
				}
		}

		/**
		 * Returns the list of effective operators this check will evaluate against the message
		 *
		 * @return
		 */
		public abstract List<T> getOperators();

		/**
		 * Starts the filtering
		 */
		protected abstract void filter(T operator) throws EventHandledException;

		/**
		 * Return true if the given operator can be applied for the given message
		 */
		private final boolean canFilter(T operator) {

			// Ignore disabled rules
			if (operator.isDisabled())
				return false;

			// Expired
			if (operator.getExpires() != -1 && System.currentTimeMillis() > operator.getExpires())
				return false;

			if (operator.isRequirePlayedBefore() && !VelocityControl.getPlayers().contains(this.player))
				return false;

			if (operator.isIgnorePlayedBefore() && VelocityControl.getPlayers().contains(this.player))
				return false;

			return true;
		}

		/**
		 * Run given operators for the given message and return the updated message
		 */
		protected void executeOperators(T operator) throws EventHandledException {

			if (isPlayer)
				for (final String command : operator.getPlayerCommands())
					VelocityControl.getServer().getCommandManager().executeAsync(player, replaceVariables(command, operator));

			for (final String commandLine : operator.getBungeeCommands()) {
				final String command = Common.joinRange(0, commandLine.split(" "));

				VelocityControl.getServer().getCommandManager().executeAsync(sender, replaceVariables(command, operator));
			}

			for (final String message : operator.getConsoleMessages())
				Common.log(replaceVariables(message, operator));

			for (final Map.Entry<String, String> entry : operator.getWriteMessages().entrySet()) {
				final String file = entry.getKey();
				final String message = replaceVariables(entry.getValue(), operator);

				FileUtil.writeFormatted(file, "", message);
			}

			if (operator.getKickMessage() != null) {
				final String kickReason = replaceVariables(operator.getKickMessage(), operator);

				if (isPlayer)
					player.disconnect(Component.text(kickReason));
			}

			// Dirty: Run later including when EventHandledException is thrown
			if (!operator.getWarnMessages().isEmpty())
				Common.runLater(1, () -> {
					for (final Entry<UUID, String> entry : operator.getWarnMessages().entrySet()) {
						final String warnMessage = RandomUtil.nextItem(splitVertically(entry.getValue())); // pick one in a list of |

						sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(replaceVariables(warnMessage, operator)));
					}
				});

			if (operator.isCancelMessage()) {
				if (!operator.isIgnoreVerbose())
					verbose("&cOriginal message cancelled.");

				throw new EventHandledException(true);
			}

			if (operator.isCancelMessageSilently())
				this.cancelledSilently = true;
		}

		/*
		 * Replace all kinds of check variables
		 */
		protected String replaceVariables(String message, T operator) {
			if (message == null)
				return null;

			return Replacer.replaceVariables(message, prepareVariables(operator));
		}

		/**
		 * Prepare variables available in this check
		 *
		 * @param operator
		 * @return
		 */
		protected SerializedMap prepareVariables(T operator) {
			final SyncedCache cache = SyncedCache.fromName(this.sender.getUsername());
			final SerializedMap map = cache != null ? cache.toVariables()
					: SerializedMap.ofArray(
					"player_name", this.sender.getUsername(),
					"name", this.sender.getUsername(),
					"player_nick", this.sender.getUsername(),
					"nick", this.sender.getUsername(),
					"player_group", "",
					"player_prefix", "",
					"player_server", this.sender instanceof Player ? this.sender.getCurrentServer().get().getServerInfo().getName() : "",
					"player_afk", "false",
					"player_ignoring_pms", "false",
					"player_vanished", "false");

			return map.mergeFrom(this.variables);
		}

		/**
		 * Return if the sender has the given permission
		 *
		 * @param permission
		 * @return
		 */
		protected final boolean hasPerm(String permission) {
			return this.sender.hasPermission(permission);
		}

		/**
		 * Cancels the pipeline by throgin a {@link EventHandledException}
		 */
		protected final void cancel() {
			this.cancel(null);
		}

		/**
		 * Cancels the pipeline by throgin a {@link EventHandledException}
		 * and send an error message to the player
		 *
		 * @param errorMessage
		 */
		protected final void cancel(String errorMessage) {
			if (errorMessage != null)
				sender.sendMessage(Component.text(errorMessage).color(NamedTextColor.RED));

			throw new EventHandledException(true);
		}

		/**
		 * Show the message if rules are set to verbose
		 */
		protected final void verbose(String... messages) {
			Common.log(messages);
		}
	}

	/**
	 * Represents an indication that further rule processing should be aborted
	 */
	@Getter
	@RequiredArgsConstructor
	public final static class OperatorAbortException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
}
