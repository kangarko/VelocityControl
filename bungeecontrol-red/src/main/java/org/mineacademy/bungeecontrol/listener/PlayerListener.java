package org.mineacademy.bungeecontrol.listener;

import org.mineacademy.bfo.bungee.message.OutgoingMessage;
import org.mineacademy.bungeecontrol.BungeeControl;
import org.mineacademy.bungeecontrol.model.BungeePacket;
import org.mineacademy.bungeecontrol.settings.Settings;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public final class PlayerListener implements Listener {

	/**
	 * Notify downstream what server name alias we configured here
	 *
	 * @param event
	 */
	@EventHandler
	public void onJoin(final ServerConnectedEvent event) {
		final ProxiedPlayer player = event.getPlayer();
		final ServerInfo server = event.getServer().getInfo();

		// Prepare packet
		final OutgoingMessage message = new OutgoingMessage(BungeePacket.SERVER_ALIAS);

		message.writeString(server.getName());
		message.writeString(Settings.getServerNameAlias(server));

		BungeeControl.forwardMessage(message, player);
	}
}
