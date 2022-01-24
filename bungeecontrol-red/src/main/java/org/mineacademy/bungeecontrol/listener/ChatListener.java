package org.mineacademy.bungeecontrol.listener;

import java.util.Iterator;
import java.util.List;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.model.IsInList;
import org.mineacademy.bungeecontrol.settings.Settings;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

/**
 * Forward chat messages and filter tab
 */
public final class ChatListener implements Listener {

	/**
	 * Filter tab
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onTabComplete(TabCompleteEvent event) {
		final String label = event.getCursor().trim().substring(1);
		final List<String> args = event.getSuggestions();

		final IsInList<String> filterArgs = Settings.TabComplete.FILTER_ARGUMENTS.get(label);

		if (filterArgs != null)
			for (final Iterator<String> it = args.iterator(); it.hasNext();) {
				final String arg = it.next();

				if (filterArgs.contains(arg))
					it.remove();
			}
	}

	/**
	 * Forward chat messages
	 *
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onChatEvent(ChatEvent event) {
		if (!Settings.ChatForwarding.ENABLED || event.isCancelled() || !(event.getSender() instanceof ProxiedPlayer))
			return;

		String message = event.getMessage();

		if (message.length() == 0 || message.charAt(0) == '/')
			return;

		final ProxiedPlayer player = (ProxiedPlayer) event.getSender();
		final ServerInfo server = player.getServer().getInfo();

		if (server == null) {
			Common.log("Unexpected error: unknown server for " + player.getName());

			return;
		}

		if (!Settings.ChatForwarding.FROM_SERVERS.contains(server.getName()))
			return;

		message = String.format("<%s> %s", player.getName(), message);

		for (final ProxiedPlayer online : ProxyServer.getInstance().getPlayers()) {
			final ServerInfo serverInfo = online.getServer().getInfo();

			if (!serverInfo.equals(server) && Settings.ChatForwarding.TO_SERVERS.contains(serverInfo.getName()))
				online.sendMessage(ChatMessageType.CHAT, TextComponent.fromLegacyText(message));
		}
	}
}
