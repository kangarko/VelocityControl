package org.mineacademy.velocitycontrol;

import lombok.Getter;
import org.mineacademy.velocitycontrol.foundation.Common;
import org.mineacademy.velocitycontrol.foundation.Debugger;
import org.mineacademy.velocitycontrol.listener.OutgoingMessage;
import org.mineacademy.velocitycontrol.listener.VelocityControlListener;
import org.mineacademy.velocitycontrol.model.ChannelMode;
import org.mineacademy.velocitycontrol.model.ProxyPacket;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a cache with data from BungeeCord
 */
@Getter
public final class SyncedCache {

	/**
	 * The internal map
	 * Name : Data
	 */
	private static final Map<String, SyncedCache> cacheMap = new HashMap<>();

	/**
	 * The player name
	 */
	@Getter
	private final String playerName;

	/**
	 * The unique ID
	 */
	@Getter
	private final UUID uniqueId;

	/**
	 * The server where this player is on
	 */
	@Getter
	private String serverName = "";

	/**
	 * His nick if any
	 */
	@Getter
	private String nick;

	/**
	 * Is the player vanished?
	 */
	private boolean vanished;

	/**
	 * Is the player a fucking drunk?
	 */
	private boolean afk;

	/**
	 * Does this plugin not give a damn about private messages?
	 */
	private boolean ignoringPMs;

	/**
	 * Is the player ignoring sound notifications
	 */
	private boolean ignoringSoundNotify;

	/**
	 * List of ignored dudes
	 */
	private Set<UUID> ignoredPlayers = new HashSet<>();;

	/**
	 * Map of channel names and modes this synced man is in
	 */
	@Getter
	private Map<String, ChannelMode> channels = new HashMap<>();

	/**
	 * The player prefix from Vault
	 */
	@Getter
	private String prefix;

	/**
	 * The player group from Vault
	 */
	@Getter
	private String group;

	/*
	 * Create a synced cache from the given data map
	 */
	private SyncedCache(String playerName, UUID uniqueId) {
		this.playerName = playerName;
		this.uniqueId = uniqueId;
	}

	private void loadData(String line) {
		final String[] sections = line.split(".<<");

		for (final String section : sections) {
			final String[] parts = section.split("\\:");

			final String sectionName = parts[0];
			final String sectionValue = Common.joinRange(1, parts.length, parts, ":");

			if ("S".equals(sectionName))
				this.serverName = sectionValue;

			else if ("N".equals(sectionName))
				this.nick = sectionValue.isEmpty() ? null : sectionValue;

			else if ("V".equals(sectionName))
				this.vanished = Builder.parseBoolean(sectionValue);

			else if ("A".equals(sectionName))
				this.afk = Builder.parseBoolean(sectionValue);

			else if ("IM".equals(sectionName))
				this.ignoringPMs = Builder.parseBoolean(sectionValue);

			else if ("IN".equals(sectionName))
				this.ignoringSoundNotify = Builder.parseBoolean(sectionValue);

			else if ("IP".equals(sectionName))
				this.ignoredPlayers = Builder.parseUUIDList(sectionValue);

			else if ("C".equals(sectionName))
				this.channels = Builder.parseChannels(sectionValue);

			else if ("G".equals(sectionName))
				this.group = sectionValue;

			else if ("P".equals(sectionName))
				this.prefix = sectionValue;
		}

	}

	/**
	 * Return a dude's name or nick if set
	 *
	 * @return
	 */
	public String getNameOrNickColored() {
		return Common.getOrDefaultStrict(this.nick, this.playerName);
	}

	/**
	 * Is vanished?
	 */
	public boolean isVanished() {
		return this.vanished;
	}

	/**
	 * Is afk?
	 */
	public boolean isAfk() {
		return this.afk;
	}

	/**
	 * Is ignoring pms?
	 */
	public boolean isIgnoringPMs() {
		return this.ignoringPMs;
	}

	/**
	 * Convert into known variables usable in chat
	 *
	 * @return
	 */
	public HashMap<String, String> toVariables() {
		return new HashMap<>() {{
			put("player_name", getPlayerName());
			put("name", getPlayerName());
			put("player_nick", getNameOrNickColored());
			put("nick", getNameOrNickColored());
			put("player_group", getGroup());
			put("player_prefix", getPrefix());
			put("player_server", getServerName());
			put("player_afk", isAfk() ? "true" : "false");
			put("player_ignoring_pms", isIgnoringPMs() ? "true" : "false");
			put("player_ignoring_sound_notifications", isIgnoringSoundNotify() ? "true" : "false");
			put("player_vanished", isVanished() ? "true" : "false");
		}};
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SyncedCache{" + this.playerName + ",nick=" + this.nick + "}";
	}

	/* ------------------------------------------------------------------------------- */
	/* Static */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Resolve a synced cache from the given player
	 */

	public static SyncedCache fromName(String playerName) {
		synchronized (cacheMap) {
			return cacheMap.get(playerName);
		}
	}

	/**
	 * Add/remove syncedcaches based on online network players
	 */
	public static void updateForOnlinePlayers() {
		synchronized (cacheMap) {
			final HashMap<String, UUID> onlinePlayers = new HashMap<>();

			// Add non-cached players
			VelocityControl.getServer().getAllPlayers().forEach(player -> {
				final String playerName = player.getUsername();
				final UUID uniqueId = player.getUniqueId();

				if (!cacheMap.containsKey(playerName))
					cacheMap.put(playerName, new SyncedCache(playerName, uniqueId));

				onlinePlayers.put(playerName, uniqueId);
			});

			Iterator<Map.Entry<String, SyncedCache>> cacheMapIterator = cacheMap.entrySet().iterator();
			while(cacheMapIterator.hasNext()) {
				Map.Entry<String, SyncedCache> entry = cacheMapIterator.next();
				if(!onlinePlayers.containsKey(entry.getKey())) {
					cacheMapIterator.remove();
				}
			}

			final OutgoingMessage message = new OutgoingMessage(ProxyPacket.PLAYERS_CLUSTER_HEADER);
			message.writeMap(onlinePlayers);

			VelocityControl.broadcastPacket(message);
		}
	}

	/**
	 * Retrieve (or create) a sender cache
	 * @param syncType
	 * @param data
	 */
	public static void upload(VelocityControlListener.SyncType syncType, HashMap<String, String> data) {
		synchronized (cacheMap) {
			data.forEach((playerName, dataLine) -> {
				final SyncedCache cache = cacheMap.get(playerName);

				if (cache != null) {
					Debugger.debug("Loading data for " + playerName + " of type " + syncType + " from line " + dataLine);

					cache.loadData(dataLine);
				}
			});
		}
	}

	/**
	 * Force data update for the given dude.
	 *
	 * @param playerName
	 * @param uniqueId
	 * @param line
	 */
	public static void uploadSingle(String playerName, UUID uniqueId, String line) {
		SyncedCache cache = cacheMap.get(playerName);

		if (cache == null)
			cache = new SyncedCache(playerName, uniqueId);

		cache.loadData(line);
		cacheMap.put(playerName, cache);
	}

	/* ------------------------------------------------------------------------------- */
	/* Classes */
	/* ------------------------------------------------------------------------------- */

	/**
	 * A helper for cost-effective cross-network data sync
	 */
	public static final class Builder {

		/**
		 * Convert a line value into a boolean
		 * 
		 * @param value
		 * @return
		 */
		public static boolean parseBoolean(String value) {
			return value.equals("0") ? false : true;
		}

		/**
		 * Convert a line value into a set of uuids
		 * 
		 * @param value
		 * @return
		 */
		public static Set<UUID> parseUUIDList(String value) {
			return value.isEmpty() ? new HashSet<>() : new HashSet<>(Arrays.stream(value.split("\\|")).map(UUID::fromString).collect(Collectors.toSet()));
		}

		/**
		 * Convert a line value into a map of channel-mode pairs
		 * 
		 * @param value
		 * @return
		 */
		public static Map<String, ChannelMode> parseChannels(String value) {
			final Map<String, ChannelMode> channels = new HashMap<>();
			final String[] channelWithModes = value.split("\\|");

			for (final String channelWithMode : channelWithModes) {
				final String[] parts = channelWithMode.split("\\:");

				final String channelName = parts[0];
				final ChannelMode channelMode = ChannelMode.values()[1];

				channels.put(channelName, channelMode);
			}

			return channels;
		}
	}
}
