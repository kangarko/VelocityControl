package org.mineacademy.velocitycontrol;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.mineacademy.velocitycontrol.command.ReloadCommand;
import org.mineacademy.velocitycontrol.listener.*;
import org.mineacademy.velocitycontrol.operator.PlayerMessages;
import org.mineacademy.velocitycontrol.settings.Settings;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Collection;

/**
* The main VelocityControl Red plugin class.
*/
@Plugin(id = "velocitycontrol", name = "VelocityControl", version = "3.11.7-SNAPSHOT", authors = {"kangarko", "relavis", "james090500"})
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
	public VelocityControl(final ProxyServer proxyServer, Logger logger, final @DataDirectory Path dataDirectory) {
		this.server = proxyServer;
		this.folder = dataDirectory;
		this.logger = logger;
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
		//Make plugin folder first
		if(!VelocityControl.getFolder().toFile().exists()) {
			VelocityControl.getFolder().toFile().mkdirs();
		}

		long time = System.currentTimeMillis();
		ServerCache.getInstance();
		Settings.load();

		velocityControl = new VelocityControlListener();
		server.getChannelRegistrar().register(CHANNEL);
		server.getEventManager().register(this, new SwitchListener());
		server.getEventManager().register(this, new ChatListener());
		server.getEventManager().register(this, new CommandListener());
		server.getEventManager().register(this, new PlayerListener());
		server.getEventManager().register(this, velocityControl);
		this.onReloadablesStart();

		CommandMeta commandMeta = server.getCommandManager().metaBuilder("vcreload").build();
		server.getCommandManager().register(commandMeta, new ReloadCommand());

		time = System.currentTimeMillis() - time;
		getServer().getConsoleCommandSource().sendMessage(
				LegacyComponentSerializer.legacyAmpersand().deserialize("&a[VelocityControl] Loaded in " + time + "ms!")
		);
	}

	protected void onReloadablesStart() {
		PlayerMessages.getInstance().load();
		velocityControl.scheduleSyncTask();
	}

	public void onPluginStop() {
		server.getChannelRegistrar().unregister(CHANNEL);
		ServerCache.getInstance().saveFile();
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
	 */
	public static void forwardMessage(OutgoingMessage message) {
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
