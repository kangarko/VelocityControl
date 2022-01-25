package org.mineacademy.velocitycontrol.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.TabCompleteEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.model.IsInList;
import org.mineacademy.velocitycontrol.VelocityControl;
import org.mineacademy.velocitycontrol.settings.Settings;

import java.util.Iterator;
import java.util.List;


/**
 * Forward chat messages
 */
public final class ChatListener {
	/**
	 * Filter tab
	 *
	 * @param event
	 */
	@Subscribe(order = PostOrder.LATE)
	public void onTabComplete(TabCompleteEvent event) {
		final String label = event.getPartialMessage().trim().substring(1);
		final List<String> args = event.getSuggestions();

		final IsInList<String> filterArgs = Settings.TabComplete.Filter_Arguments.get(label);

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
	@Subscribe(order = PostOrder.LATE)
	public void onChatEvent(PlayerChatEvent event) {
		if (!Settings.getSettings().Chat_Forwarding.Enabled || !event.getResult().isAllowed())
			return;

		String message = event.getMessage();

		if (message.length() == 0 || message.charAt(0) == '/')
			return;

		final Player player = event.getPlayer();
		final ServerInfo server = player.getCurrentServer().get().getServerInfo();

		if (server == null) {
			Common.log("Unexpected error: unknown server for " + player.getUsername());

			return;
		}

		if (!Settings.getSettings().Chat_Forwarding.From_Servers.contains(server.getName()))
			return;

		message = String.format("<%s> %s", player.getUsername(), message);

		for (final Player online : VelocityControl.getPlayers()) {
			final ServerInfo serverInfo = online.getCurrentServer().get().getServerInfo();

			if (!serverInfo.equals(server) && Settings.getSettings().Chat_Forwarding.To_Servers.contains(serverInfo.getName()))
				online.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
		}
	}
}
