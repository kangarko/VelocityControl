package org.mineacademy.velocitycontrol.settings;

import com.james090500.CoreFoundation.collection.StrictMap;
import com.james090500.CoreFoundation.model.IsInList;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.Getter;
import org.mineacademy.velocitycontrol.VelocityControl;
import org.mineacademy.velocitycontrol.operator.PlayerMessage;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Represents the main plugin configuration
 */
@SuppressWarnings("unused")
public final class Settings {
	private static byte[] toByteArray(InputStream input) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];

		while ((nRead = input.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		return buffer.toByteArray();
	}
	public static void load() {
		try {
			File file = new File(VelocityControl.getFolder().toFile(), "settings.yml");
			if(!VelocityControl.getFolder().toFile().exists()) {
				VelocityControl.getFolder().toFile().mkdirs();
			}

			if (!file.exists()) {
				try (InputStream in = Settings.class.getResourceAsStream("/settings.yml")) {
					Files.copy(in, file.toPath());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			try (Reader reader = new FileReader(file)) {
				//LoaderOptions loaderOptions = new LoaderOptions();
				//loaderOptions.setEnumCaseSensitive(false); When SnakeYAML updates!
				CustomClassLoaderConstructor customClassLoaderConstructor = new CustomClassLoaderConstructor(SettingsFile.class.getClassLoader());
				settings = new Yaml(customClassLoaderConstructor).loadAs(reader, SettingsFile.class);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Getter
	private static SettingsFile settings;

	public static class SettingsFile {
		public Map<String, String> Server_Aliases;
		public Messages Messages;
		public TabComplete Tab_Complete;
		public ChatForwarding Chat_Forwarding;
		public Clusters Clusters;
		public String Spy_Format;
	}

	/**
	 * Settings for timed message broadcaster
	 */
	public static class Messages {
		public ArrayList<PlayerMessage.Type> Apply_On = new ArrayList<>();
		public ArrayList<String> Ignored_Servers = new ArrayList<>();
		public Boolean Stop_On_First_Match;
		public Map<PlayerMessage.Type, String> Prefix;
		public long Defer_Join_Message_By;
	}

	/**
	 * Settings for tab filter
	 */
	public static class TabComplete {
		public StrictMap<String, IsInList<String>> Filter_Arguments;
	}

	/**
	 * Clusters
	 */
	public static class Clusters {
		public Boolean Enabled;
		public Map<String, Set<String>> List;
	}


	/**
	 * Relay chat
	 */
	public static class ChatForwarding {
		public boolean Enabled;
		public ArrayList<String> To_Servers;
		public ArrayList<String> From_Servers;
	}

	private static Map<String, String> Server_Aliases;

	/**
	 * A helper method to use {@link #Server_Aliases} or return the default server name if alias not set
	 *
	 * @param info
	 * @return
	 */
	public static String getServerNameAlias(RegisteredServer info) {
		final String name = info.getServerInfo().getName();

		return Settings.getSettings().Server_Aliases.getOrDefault(name, name);
	}

	/**
	 * A helper method to use {@link #Server_Aliases} or return the default server name if alias not set
	 *
	 * @param serverName
	 * @return
	 */
	public static String getServerNameAlias(String serverName) {
		return Settings.getSettings().Server_Aliases.getOrDefault(serverName, serverName);
	}

	public static String getFromServerName(String serverName) {
		if (getSettings().Clusters.Enabled)
			for (final Map.Entry<String, Set<String>> entry : getSettings().Clusters.List.entrySet()) {
				final String clusterName = entry.getKey();

				for (final String clusterServerName : entry.getValue())
					if (clusterServerName.equals(serverName))
						return clusterName;
			}

		return "global";
	}
}
