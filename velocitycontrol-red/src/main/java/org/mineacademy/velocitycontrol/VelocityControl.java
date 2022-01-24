package org.mineacademy.velocitycontrol;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.Getter;
import org.mineacademy.velocitycontrol.listener.*;
import org.mineacademy.velocitycontrol.operator.PlayerMessages;
import org.mineacademy.velocitycontrol.settings.Settings;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Collection;

/**
* The main VelocityControl Red plugin class.
*/
@Plugin(id = "velocitycontrol", name = "VelocityControl", version = "0.0.1-SNAPSHOT")
public final class VelocityControl {
	@Getter
	private static VelocityControl instance;

	@Getter
	private static ProxyServer server;

	@Getter
	private static Path folder;

	@Getter
	private static Logger logger;
	/**
	* The channel we are broadcasting at
	*/
	public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create(
		"plugin", "chcred"
	);

	/**
	 * The listener
	 */
	private VelocityControlListener velocityControl;

	@Inject
	public VelocityControl(final ProxyServer proxyServer, final Logger _logger, final @DataDirectory Path dataDirectory) {
		server = proxyServer;
		folder = dataDirectory;
		logger = _logger;
		instance = this;
	}

	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
		onPluginStart();
	}

	@Subscribe
	public void onProxyInitialization(ProxyShutdownEvent event) {
		onPluginStop();
	}

	public void onPluginStart() {

		Settings.load();
		velocityControl = new VelocityControlListener();
		server.getChannelRegistrar().register(CHANNEL);
		server.getEventManager().register(this, new SwitchListener());
		server.getEventManager().register(this, new ChatListener());
		server.getEventManager().register(this, new PlayerListener());
		server.getEventManager().register(this, velocityControl);

	}

	protected void onReloadablesStart() {
		PlayerMessages.getInstance().load();

		velocityControl.scheduleSyncTask();
	}

	public void onPluginStop() {
		server.getChannelRegistrar().unregister(CHANNEL);
	}

	/* ------------------------------------------------------------------------------- */
	/* Methods */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Return all servers on the network or Redis
	 *
	 * @return
	 */
	public static Collection<RegisteredServer> getServers() {
		return server.getAllServers();
	}

	/**
	 * Return all players on the network or Redis
	 * 
	 * @return
	 */
	public static Collection<Player> getPlayers() {
		return server.getAllPlayers();
	}


	/**
	 * Forwards the given message as the given sender to non-empty server.
	 *
	 * @param message
	 * @param player
	 */
	public static void forwardMessage(OutgoingMessage message, Player player) {
		final byte[] data = message.compileData();

		for (final RegisteredServer registeredServer: getServers()) {

			if (!registeredServer.getPlayersConnected().isEmpty())
				registeredServer.sendPluginMessage(VelocityControl.CHANNEL, data);
		}
	}

	/**
	 * Broadcast the given message to all non empty servery
	 *
	 * @param message
	 */
	public static void broadcastPacket(OutgoingMessage message) {
		for (final RegisteredServer server : getServers()) {

			// Avoiding sending to empty server since messages will then "stack up"
			if (!server.getPlayersConnected().isEmpty())
				message.send(server);
		}
	}
}
