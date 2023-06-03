package org.mineacademy.velocitycontrol.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import lombok.NonNull;
import org.mineacademy.velocitycontrol.ServerCache;
import org.mineacademy.velocitycontrol.SyncedCache;
import org.mineacademy.velocitycontrol.VelocityControl;
import org.mineacademy.velocitycontrol.foundation.Debugger;
import org.mineacademy.velocitycontrol.operator.PlayerMessage;
import org.mineacademy.velocitycontrol.operator.PlayerMessages;
import org.mineacademy.velocitycontrol.settings.Settings;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
	private static final Map<UUID, HashMap<PlayerMessage.Type, HashMap<String, String>>> pendingMessages = new HashMap<>();

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

		if (!this.players.containsKey(player.getUniqueId()) && !isSilent(event.getServer().getServerInfo())) {
			final String toServer = Settings.getServerNameAlias(event.getServer());

			if (!isSilent(toServer)) {
				Debugger.debug("player-message", "Detected " + player.getUsername() + " join to " + toServer + ", waiting for server data..");
				pendingMessages.put(player.getUniqueId(), new HashMap<>() {{ put(PlayerMessage.Type.JOIN, new HashMap<>() {{ put("server", toServer); }}); }});
			}
		}
	}

	/**
	 * Handle server switch messages.
	 *
	 * @param event
	 */
	@Subscribe(order = PostOrder.LAST)
	public void onSwitch(ServerConnectedEvent event) {
		final Player player = event.getPlayer();
		final RegisteredServer currentServer = event.getServer();
		final RegisteredServer lastServer = this.players.put(player.getUniqueId(), currentServer);

		// Announce switches to/from silent servers on servers not silenced
		if (lastServer != null) {
			final String fromServer = Settings.getServerNameAlias(lastServer);
			final String toServer = Settings.getServerNameAlias(currentServer);

			if (!isSilent(fromServer)) {
				pendingMessages.put(player.getUniqueId(), new HashMap<>() {{ put(PlayerMessage.Type.SWITCH, new HashMap<>() {{ put("from_server", fromServer); put("to_server", toServer); }}); }});
			}
		}
	}

	@Subscribe(order = PostOrder.FIRST)
	public void onDisconnect(DisconnectEvent event) {
		final Player player = event.getPlayer();
		final String playerName = event.getPlayer().getUsername();
		final ServerCache cache = ServerCache.getInstance();
		final RegisteredServer server = this.players.remove(player.getUniqueId());

		if (server != null && !isSilent(server.getServerInfo())) {
			final String fromServer = Settings.getServerNameAlias(server);
			final SyncedCache synced = SyncedCache.fromName(playerName);

			if (synced != null && !synced.isVanished() && !isSilent(fromServer)) {
				PlayerMessages.broadcast(PlayerMessage.Type.QUIT, player, new HashMap<>() {{ put("server", fromServer); }});

				if (!cache.isPlayerRegistered(player)) {
					cache.registerPlayer(player);
				}
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
		final HashMap<PlayerMessage.Type, HashMap<String, String>> data = pendingMessages.remove(player.getUniqueId());
		//Adding null check to fix this error: https://pastebin.com/iuestAMR
		if (data == null) {
			return;
		}
		data.forEach((type, variables) -> {
			final SyncedCache cache = SyncedCache.fromName(player.getUsername());
			if (!cache.isVanished() || player.hasPermission("chatcontrol.bypass.reach")) {
				PlayerMessages.broadcast(type, player, variables);
			}
		});
	}
}
