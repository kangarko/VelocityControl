package org.mineacademy.bungeecontrol.model;

import static org.mineacademy.bungeecontrol.BungeeControl.REDIS_CHANNEL;

import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.debug.Debugger;
import org.mineacademy.bfo.plugin.SimplePlugin;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * The main class providing a partial Redis integration
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Redis implements Listener {

	/**
	 * The Redis API class
	 */
	private static final RedisBungeeAPI redisAPI = RedisBungee.getApi();

	/* ------------------------------------------------------------------------------- */
	/* Registration */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Register the redis integration and events, called when our plugin starts
	 */
	public static void register() {
		redisAPI.registerPubSubChannels(REDIS_CHANNEL);

		ProxyServer.getInstance().getPluginManager().registerListener(SimplePlugin.getInstance(), new Redis());
	}

	/**
	 * Unregisters our redis integration, called when our plugin stops/reloads
	 */
	public static void unregister() {
		redisAPI.unregisterPubSubChannels(REDIS_CHANNEL);
	}

	/* ------------------------------------------------------------------------------- */
	/* Event listener */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Listen to plugin messages across network
	 *
	 * @param event
	 */
	@EventHandler
	public void onPubSubMessage(PubSubMessageEvent event) {
		if (event.getChannel().equals(REDIS_CHANNEL)) {

			Debugger.debug("redis", "Received redis message: " + event.getMessage());

			try {
				final String[] data = event.getMessage().split(":", 4);

				if (data.length == 4 && data[0].equals("SEND_SB")) {
					final UUID playerId = UUID.fromString(data[1]);

					// is this player on this proxy?
					final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerId);

					if (player != null && player.isConnected()) {
						// re-encode the message
						final byte[] byteOutput = decapsulate(data[3]);
						player.getServer().sendData(data[2].replace(" \\;", ":").replace("\\\\", "\\"), byteOutput);
					}

				} else if (data.length == 4 && data[0].equals("SEND_OB")) {

					// send to servers that player is not on
					final UUID playerId = UUID.fromString(data[1]);

					for (final ServerInfo otherServer : ProxyServer.getInstance().getServers().values()) {
						boolean allow = false;

						for (final ProxiedPlayer otherPlayer : otherServer.getPlayers()) {
							if (otherPlayer.isConnected() && otherPlayer.getUniqueId().equals(playerId)) {
								allow = false;

								break;
							}

							allow = true;
						}

						if (allow) {
							Debugger.debug("redis", "Sending data to " + otherServer.getName());
							final byte[] byteOutput = decapsulate(data[3]);

							otherServer.sendData(data[2].replace(" \\;", ":").replace("\\\\", "\\"), byteOutput);

						} else
							Debugger.debug("redis", "Not sending to " + otherServer.getName());
					}

				} else if (data.length == 4 && data[0].equals("SEND_M")) {
					final UUID playerId = UUID.fromString(data[1]);

					// is this player on this proxy?
					final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerId);

					if (player != null && player.isConnected())
						player.sendMessage(data[3]);

				} else
					Common.log("BungeeControl received an invalid RedisBungee message: " + event.getMessage());

			} catch (final Throwable t) {
				Common.error(t, "Unhandled error processing redis message in BungeeControl Red");
			}

		}
	}

	/* ------------------------------------------------------------------------------- */
	/* Methods */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Returns all servers found from all players connected on the Redis network
	 *
	 * @return
	 */
	public static Collection<ServerInfo> getServers() {
		final Map<Integer, ServerInfo> servers = new HashMap<>();

		for (final UUID playerId : redisAPI.getPlayersOnline()) {
			final ServerInfo playerServer = redisAPI.getServerFor(playerId);

			if (playerServer != null)
				if (!servers.containsKey(playerServer.getAddress().hashCode()))
					servers.put(playerServer.getAddress().hashCode(), playerServer);

			Debugger.debug("redis", "BungeeControl failed to find server associated with Redis player '" + playerId + "'!");
		}

		return servers.values();
	}

	/**
	 * Sends a raw plugin message data to the whole Redis network
	 *
	 * @param uuid
	 * @param channel
	 * @param data
	 */
	public static void sendDataToOtherServers(UUID uuid, String channel, byte[] data) {
		redisAPI.sendChannelMessage(REDIS_CHANNEL, "SEND_OB:" + uuid.toString() + ":" + channel.replace("\\", "\\\\").replace(":", " \\;") + ":" + encapsulate(data));
	}

	/**
	 * Executes the given command across Redis network
	 *
	 * @param cmd
	 */
	public static void dispatchCommand(String cmd) {
		redisAPI.sendProxyCommand(cmd);
	}

	/*
	 * Convert the data array to Base64
	 */
	private static String encapsulate(byte[] data) {
		return Base64.getEncoder().encodeToString(data);
	}

	/*
	 * Convers the Base64 string to data array
	 */
	private static byte[] decapsulate(String data) {
		return Base64.getDecoder().decode(data);
	}
}
