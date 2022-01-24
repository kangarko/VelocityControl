package org.mineacademy.velocitycontrol.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import lombok.NonNull;
import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.bfo.model.Tuple;
import org.mineacademy.velocitycontrol.VelocityControl;
import org.mineacademy.velocitycontrol.SyncedCache;
import org.mineacademy.velocitycontrol.operator.PlayerMessage;
import org.mineacademy.velocitycontrol.operator.PlayerMessages;
import org.mineacademy.velocitycontrol.settings.Settings;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Handles join, quit and server switch messages.
 *
 * @author kangarko
 *
 */
public final class SwitchListener {

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
	private final Map<UUID, RegisteredServer> players = new HashMap<>();

	/**
	 * Handle join messages.
	 *
	 * @param event
	 */
	@Subscribe(order = PostOrder.LATE)
	public void onConnect(ServerConnectedEvent event) {

		final Player player = event.getPlayer();
		final String playerName = player.getUsername();

		if (!this.players.containsKey(player.getUniqueId()) && !isSilent(event.getServer().getServerInfo())) {
			final String toServer = Settings.getServerNameAlias(event.getServer());

			if (!isSilent(toServer)) {
				Debugger.debug("player-message", "Detected " + player.getUsername() + " join to " + toServer + ", waiting for server data..");

				pendingMessages.put(player.getUniqueId(), new Tuple<>(PlayerMessage.Type.JOIN, SerializedMap.of("server", toServer)));
			}
		}
	}

	/**
	 * Handle server switch messages.
	 *
	 * @param event
	 */
	@Subscribe(order = PostOrder.LATE)
	public void onSwitch(ServerConnectedEvent event) {
		if (!event.getPreviousServer().isPresent()) return;
		final Player player = event.getPlayer();
		final RegisteredServer currentServer = event.getServer();
		final RegisteredServer lastServer = this.players.put(player.getUniqueId(), currentServer);

		// Announce switches to/from silent servers on servers not silenced
		if (lastServer != null) {
			final String fromServer = Settings.getServerNameAlias(lastServer);
			final String toServer = Settings.getServerNameAlias(currentServer);

			if (!isSilent(fromServer)) {
				pendingMessages.put(player.getUniqueId(), new Tuple<>(PlayerMessage.Type.SWITCH, SerializedMap.ofArray("from_server", fromServer, "to_server", toServer)));
			}
		}
	}

	@Subscribe(order = PostOrder.LATE)
	public void onDisconnect(DisconnectEvent event) {
		final Player player = event.getPlayer();
		final String playerName = event.getPlayer().getUsername();
		final RegisteredServer server = this.players.remove(player.getUniqueId());

		if (server != null && !isSilent(server.getServerInfo())) {
			final Collection<Player> allPlayers = new HashSet<>(VelocityControl.getPlayers());
			final Collection<Player> fromPlayers = server.getPlayersConnected();

			allPlayers.removeAll(fromPlayers);

			final String fromServer = Settings.getServerNameAlias(server);

			final SyncedCache synced = SyncedCache.fromName(playerName);

			if (synced != null && !synced.isVanished() && !isSilent(fromServer))
				PlayerMessages.broadcast(PlayerMessage.Type.QUIT, player, SerializedMap.of("server", fromServer));

		}
	}

	/**
	 * Return true if the server alias is ignored
	 *
	 * @param serverAlias
	 * @return
	 */
	private boolean isSilent(@NonNull String serverAlias) {
		return Settings.getSettings().Messages.Ignored_Servers.contains(serverAlias);
	}

	/**
	 * Return true if the server is ignored
	 *
	 * @param info
	 * @return
	 */
	private boolean isSilent(@NonNull ServerInfo info) {
		return Settings.getSettings().Messages.Ignored_Servers.contains(info.getName());
	}

	/**
	 * Broadcast pending join or switch message for the given player.
	 *
	 * @param player
	 */
	public static void broadcastPendingMessage(@NonNull Player player) {
		final Tuple<PlayerMessage.Type, SerializedMap> data = pendingMessages.remove(player.getUniqueId());

		if (data != null) {

			final PlayerMessage.Type type = data.getKey();
			final SerializedMap variables = data.getValue();
			final SyncedCache cache = SyncedCache.fromName(player.getUsername());

			if (cache == null)
				//throw new FoException("Unable to find synced data for " + player.getName());

			if (!cache.isVanished() || player.hasPermission("chatcontrol.bypass.reach")) {
				//Debugger.debug("player-message", "Broadcast " + type + " message for " + player.getName() + " with variables " + variables);

				PlayerMessages.broadcast(type, player, variables);

			} //else
				//Debugger.debug("player-message", "Failed sending " + type + " message for " + player.getName() + ", vanished ? " + cache.isVanished() + ", has bypass reach perm ? " + player.hasPermission("chatcontrol.bypass.reach"));
		}

		//else
			//Debugger.debug("player-message", "Failed finding pending join/switch message for " + player.getName() + ", quitting..");
	}
}
