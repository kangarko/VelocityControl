package org.mineacademy.bungeecontrol.listener;

import static org.mineacademy.bungeecontrol.settings.Settings.getServerNameAlias;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.bfo.debug.Debugger;
import org.mineacademy.bfo.exception.FoException;
import org.mineacademy.bfo.model.Tuple;
import org.mineacademy.bungeecontrol.ServerCache;
import org.mineacademy.bungeecontrol.SyncedCache;
import org.mineacademy.bungeecontrol.operator.PlayerMessage;
import org.mineacademy.bungeecontrol.operator.PlayerMessages;
import org.mineacademy.bungeecontrol.settings.Settings;

import lombok.NonNull;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Handles join, quit and server switch messages.
 *
 * @author kangarko
 *
 */
public final class SwitchListener implements Listener {

	/**
	 * We cannot show join/switch messages right now because we need to wait for Spigot to tell
	 * us if player is vanished, afk etc etc for rules conditions. This map is used to store players
	 * waiting for their messages to show.
	 *
	 * It overrides previous values, only last message will get shown
	 */
	private static final Map<UUID, Tuple<PlayerMessage.Type, SerializedMap>> pendingMessages = new HashMap<>();

	/**
	 * Because join and switch events are called in the same event (ServerSwitchEvent), we
	 * need to store joining players here to handle network switch.
	 */
	private final Map<UUID, ServerInfo> players = new HashMap<>();

	/**
	 * Handle join messages.
	 *
	 * @param event
	 */
	@EventHandler(priority = 32)
	public void onConnect(ServerSwitchEvent event) {
		final ProxiedPlayer player = event.getPlayer();

		if (!this.players.containsKey(player.getUniqueId()) && !isSilent(player.getServer().getInfo())) {
			final String toServer = getServerNameAlias(player.getServer().getInfo());

			if (!isSilent(toServer)) {
				Debugger.debug("player-message", "Detected " + player.getName() + " join to " + toServer + ", waiting for server data..");

				pendingMessages.put(player.getUniqueId(), new Tuple<>(PlayerMessage.Type.JOIN, SerializedMap.of("server", toServer)));
			}
		}
	}

	/**
	 * Handle quit messages.
	 *
	 * @param event
	 */
	@EventHandler(priority = 64)
	public void onDisconnect(PlayerDisconnectEvent event) {
		final ProxiedPlayer player = event.getPlayer();
		final ServerCache cache = ServerCache.getInstance();
		final String playerName = event.getPlayer().getName();
		final ServerInfo server = this.players.remove(player.getUniqueId());

		if (server != null && !isSilent(server)) {
			final String fromServer = getServerNameAlias(server);
			final SyncedCache synced = SyncedCache.fromName(playerName);

			if (synced != null && (!synced.isVanished() || player.hasPermission("chatcontrol.bypass.reach")) && !isSilent(fromServer))
				PlayerMessages.broadcast(PlayerMessage.Type.QUIT, player, SerializedMap.of("server", fromServer));
		}

		// Register player for rules operator "has played before"
		if (!cache.isPlayerRegistered(player))
			cache.registerPlayer(player);
	}

	/**
	 * Handle server switch messages.
	 *
	 * @param event
	 */
	@EventHandler(priority = 64)
	public void onSwitch(ServerSwitchEvent event) {
		final ProxiedPlayer player = event.getPlayer();
		final ServerInfo currentServer = player.getServer().getInfo();
		final ServerInfo lastServer = this.players.put(player.getUniqueId(), currentServer);

		// Announce switches to/from silent servers on servers not silenced
		if (lastServer != null) {
			final String fromServer = getServerNameAlias(lastServer);
			final String toServer = getServerNameAlias(currentServer);

			if (!isSilent(fromServer)) {
				Debugger.debug("player-message", "Detected " + player.getName() + " switch from " + fromServer + " to " + toServer + ", waiting for server data..");

				pendingMessages.put(player.getUniqueId(), new Tuple<>(PlayerMessage.Type.SWITCH, SerializedMap.ofArray("from_server", fromServer, "to_server", toServer)));
			}
		}
	}

	/**
	 * Return true if the server alias is ignored
	 *
	 * @param serverAlias
	 * @return
	 */
	private boolean isSilent(@NonNull String serverAlias) {
		return Settings.Messages.IGNORED_SERVERS.contains(serverAlias);
	}

	/**
	 * Return true if the server is ignored
	 *
	 * @param info
	 * @return
	 */
	private boolean isSilent(@NonNull ServerInfo info) {
		return Settings.Messages.IGNORED_SERVERS.contains(info.getName());
	}

	/**
	 * Broadcast pending join or switch message for the given player.
	 *
	 * @param player
	 */
	public static void broadcastPendingMessage(@NonNull ProxiedPlayer player) {
		final Tuple<PlayerMessage.Type, SerializedMap> data = pendingMessages.remove(player.getUniqueId());

		if (data != null) {

			final PlayerMessage.Type type = data.getKey();
			final SerializedMap variables = data.getValue();
			final SyncedCache cache = SyncedCache.fromName(player.getName());

			if (cache == null)
				throw new FoException("Unable to find synced data for " + player.getName());

			if (!cache.isVanished() || player.hasPermission("chatcontrol.bypass.reach")) {
				Debugger.debug("player-message", "Broadcast " + type + " message for " + player.getName() + " with variables " + variables);

				PlayerMessages.broadcast(type, player, variables);

			} else
				Debugger.debug("player-message", "Failed sending " + type + " message for " + player.getName() + ", vanished ? " + cache.isVanished() + ", has bypass reach perm ? " + player.hasPermission("chatcontrol.bypass.reach"));
		}

		else
			Debugger.debug("player-message", "Failed finding pending join/switch message for " + player.getName() + ", quitting..");
	}
}
