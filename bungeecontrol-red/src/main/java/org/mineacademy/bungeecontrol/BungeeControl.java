package org.mineacademy.bungeecontrol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.bungee.SimpleBungee;
import org.mineacademy.bfo.bungee.message.OutgoingMessage;
import org.mineacademy.bfo.command.ReloadCommand;
import org.mineacademy.bfo.plugin.SimplePlugin;
import org.mineacademy.bfo.settings.YamlStaticConfig;
import org.mineacademy.bungeecontrol.hook.PartiesAndFriendsHook;
import org.mineacademy.bungeecontrol.listener.BungeeControlListener;
import org.mineacademy.bungeecontrol.listener.ChatListener;
import org.mineacademy.bungeecontrol.listener.PlayerListener;
import org.mineacademy.bungeecontrol.listener.SwitchListener;
import org.mineacademy.bungeecontrol.model.BungeePacket;
import org.mineacademy.bungeecontrol.model.Redis;
import org.mineacademy.bungeecontrol.operator.PlayerMessages;
import org.mineacademy.bungeecontrol.settings.Settings;

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.PluginManager;

/**
* The main BungeeControl Red plugin class.
*/
public final class BungeeControl extends SimplePlugin {

	/**
	* The channel we are broadcasting at
	*/
	public static final String CHANNEL = "plugin:chcred";

	/**
	 * The redis channel we are broadcasting at
	 */
	public static final String REDIS_CHANNEL = "redischcred";

	/**
	 * Is RedisBungee Bungee plugin installed?
	 */
	@Getter
	private static boolean redisFound = false;

	/**
	 * Is the Parties plugin installed?
	 */
	@Getter
	private static boolean partiesFound = false;

	/**
	 * The listener
	 */
	private final BungeeControlListener bungeeControl = new BungeeControlListener();

	/**
	 * The main BungeeCord listener
	 */
	@Getter
	private final SimpleBungee bungeeCord = new SimpleBungee(CHANNEL, bungeeControl, BungeePacket.values());

	@Override
	public void onPluginStart() {

		ServerCache.getInstance();

		registerEvents(new SwitchListener());
		registerEvents(new ChatListener());
		registerEvents(new PlayerListener());

		registerCommand(new ReloadCommand("bcreload", "chatcontrol.command.reload"));

		final PluginManager manager = getProxy().getPluginManager();

		if (manager.getPlugin("RedisBungee") != null) {
			redisFound = true;
			Redis.register();

			Common.log("&fHooked into: &3RedisBungee");
		}

		if (manager.getPlugin("Parties") != null) {
			partiesFound = true;

			Common.log("&fHooked into: &3Parties");
		}

		if (manager.getPlugin("PartyAndFriends") != null)
			PartiesAndFriendsHook.register();
	}

	/**
	 * @see org.mineacademy.bfo.plugin.SimplePlugin#onReloadablesStart()
	 */
	@Override
	protected void onReloadablesStart() {
		PlayerMessages.getInstance().load();

		bungeeControl.scheduleSyncTask();
	}

	@Override
	public void onPluginStop() {
		getProxy().unregisterChannel(CHANNEL);

		if (redisFound)
			Redis.unregister();
	}

	@Override
	public List<Class<? extends YamlStaticConfig>> getSettings() {
		return Arrays.asList(Settings.class);
	}

	@Override
	public int getFoundedYear() {
		return 2015;
	}

	/* ------------------------------------------------------------------------------- */
	/* Methods */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Return all servers on the network or Redis
	 *
	 * @return
	 */
	public static Collection<ServerInfo> getServers() {
		return redisFound ? Redis.getServers() : ProxyServer.getInstance().getServers().values();
	}

	/**
	 * Return all players on the network or Redis
	 *
	 * @return
	 */
	public static Collection<ProxiedPlayer> getPlayers() {
		final Collection<ProxiedPlayer> players = new ArrayList<>();

		for (final ServerInfo serverInfo : getServers())
			players.addAll(serverInfo.getPlayers());

		return players;
	}

	/**
	 * Forwards the given message as the given sender to non-empty server and Redis.
	 *
	 * @param message
	 * @param player
	 */
	public static void forwardMessage(OutgoingMessage message, ProxiedPlayer player) {
		final byte[] data = message.compileData();

		// Distribute data
		if (BungeeControl.isRedisFound())
			Redis.sendDataToOtherServers(player.getUniqueId(), BungeeControl.CHANNEL, data);

		else
			for (final Map.Entry<String, ServerInfo> entry : ProxyServer.getInstance().getServers().entrySet()) {
				final ServerInfo iteratedServer = entry.getValue();

				if (!iteratedServer.getPlayers().isEmpty())
					iteratedServer.sendData(BungeeControl.CHANNEL, data);
			}
	}

	/**
	 * Broadcast the given message to all non empty servery
	 *
	 * @param message
	 */
	public static void broadcastPacket(OutgoingMessage message) {
		for (final ServerInfo server : BungeeControl.getServers()) {

			// Avoiding sending to empty server since messages will then "stack up"
			if (!server.getPlayers().isEmpty())
				message.send(server);
		}
	}
}
