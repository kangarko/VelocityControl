package org.mineacademy.bungeecontrol.settings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mineacademy.bfo.collection.StrictList;
import org.mineacademy.bfo.collection.StrictMap;
import org.mineacademy.bfo.collection.StrictSet;
import org.mineacademy.bfo.model.IsInList;
import org.mineacademy.bfo.model.SimpleTime;
import org.mineacademy.bfo.settings.SimpleSettings;
import org.mineacademy.bungeecontrol.operator.PlayerMessage;

import net.md_5.bungee.api.config.ServerInfo;

/**
 * Represents the main plugin configuration
 */
@SuppressWarnings("unused")
public final class Settings extends SimpleSettings {

	/**
	 * @see org.mineacademy.bfo.settings.SimpleSettings#getConfigVersion()
	 */
	@Override
	protected int getConfigVersion() {
		return 10;
	}

	/**
	 * @see org.mineacademy.bfo.settings.YamlStaticConfig#getUncommentedSections()
	 */
	@Override
	protected List<String> getUncommentedSections() {
		return Arrays.asList("Server_Aliases", "Clusters.List", "Tab_Complete.Filter_Arguments");
	}

	/**
	 * Settings for timed message broadcaster
	 */
	public static class Messages {

		public static StrictSet<PlayerMessage.Type> APPLY_ON;
		public static StrictList<String> IGNORED_SERVERS = new StrictList<>();
		public static Boolean STOP_ON_FIRST_MATCH;
		public static Map<PlayerMessage.Type, String> PREFIX;
		public static SimpleTime DEFER_JOIN_MESSAGE_BY;

		private static void init() {
			pathPrefix("Messages_New");

			APPLY_ON = new StrictSet<>(getList("Apply_On", PlayerMessage.Type.class));
			IGNORED_SERVERS = new StrictList<>(getStringList("Ignored_Servers"));
			STOP_ON_FIRST_MATCH = getBoolean("Stop_On_First_Match");
			PREFIX = getMap("Prefix", PlayerMessage.Type.class, String.class);
			DEFER_JOIN_MESSAGE_BY = getTime("Defer_Join_Message_By");
		}
	}

	/**
	 * Settings for tab filter
	 */
	public static class TabComplete {

		public static StrictMap<String, IsInList<String>> FILTER_ARGUMENTS;

		private static void init() {
			pathPrefix("Tab_Complete");

			final StrictMap<String, IsInList<String>> filterArgs = new StrictMap<>();

			for (final Map.Entry<String, Object> entry : getMap("Filter_Arguments").entrySet()) {
				final String label = entry.getKey();
				final IsInList<String> args = new IsInList<>((List<String>) entry.getValue());

				filterArgs.put(label, args);
			}

			FILTER_ARGUMENTS = filterArgs;
		}
	}

	/**
	 * Clusters
	 */
	public static class Clusters {

		public static Boolean ENABLED;
		public static Map<String, Set<String>> LIST;

		private static void init() {
			pathPrefix("Clusters");

			ENABLED = getBoolean("Enabled");
			LIST = new HashMap<>();

			for (final Map.Entry<String, Object> entry : getMap("List").entrySet()) {
				final String clusterName = entry.getKey();
				final List<String> servers = (List<String>) entry.getValue();

				LIST.put(clusterName, new HashSet<>(servers));
			}
		}

		public static String getFromServerName(String serverName) {
			if (ENABLED)
				for (final Map.Entry<String, Set<String>> entry : LIST.entrySet()) {
					final String clusterName = entry.getKey();

					for (final String clusterServerName : entry.getValue())
						if (clusterServerName.equals(serverName))
							return clusterName;
				}

			return "global";
		}
	}

	/**
	 * Relay chat
	 */
	public static class ChatForwarding {

		public static Boolean ENABLED;
		public static StrictList<String> TO_SERVERS;
		public static StrictList<String> FROM_SERVERS;

		private static void init() {
			pathPrefix("Chat_Forwarding");

			ENABLED = getBoolean("Enabled");
			TO_SERVERS = new StrictList<>(getStringList("To_Servers"));
			FROM_SERVERS = new StrictList<>(getStringList("From_Servers"));
		}
	}

	/**
	 * Third party plugin support
	 *
	 */
	public static class Integration {

		public static String PARTIES_PLAYER_NAME;

		private static void init() {
			pathPrefix("Integration.Parties");

			PARTIES_PLAYER_NAME = getString("Player_Name");
		}
	}

	private static Map<String, String> SERVER_ALIASES;

	/**
	 * A helper method to use SERVER_ALIASES or return the default server name if alias not set
	 *
	 * @param info
	 * @return
	 */
	public static String getServerNameAlias(ServerInfo info) {
		final String name = info.getName();

		return Settings.SERVER_ALIASES.getOrDefault(name, name);
	}

	/**
	 * A helper method to use SERVER_ALIASES or return the default server name if alias not set
	 *
	 * @param serverName
	 * @return
	 */
	public static String getServerNameAlias(String serverName) {
		return Settings.SERVER_ALIASES.getOrDefault(serverName, serverName);
	}

	private static void init() {
		SERVER_ALIASES = getMap("Server_Aliases", String.class, String.class);
	}
}
