package org.mineacademy.bungeecontrol;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.FileUtil;
import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.bfo.settings.YamlConfig;

import lombok.Getter;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * The data.db file storing various data information
 */
public final class ServerCache extends YamlConfig {

	@Getter
	private static final ServerCache instance = new ServerCache();

	/**
	 * A list of players who got "caught" up by this plugin,
	 * used for first join messages.
	 */
	private List<UUID> registeredPlayers;

	/**
	 * Load the file
	 */
	private ServerCache() {
		loadConfiguration(NO_DEFAULT, "data.db");
	}

	/**
	 * Load the values in the file
	 */
	@Override
	protected void onLoadFinish() {
		this.registeredPlayers = getList("Players", UUID.class);

		migrateTxtFile();
	}

	/*
	 * Migrate from data.txt to data.db
	 */
	private void migrateTxtFile() {
		final File txtFile = FileUtil.getFile("data.txt");

		if (txtFile.exists()) {
			for (final String rawUUID : FileUtil.readLines(txtFile)) {
				final UUID uuid = UUID.fromString(rawUUID);

				registeredPlayers.add(uuid);
			}

			save();
			txtFile.delete();

			Common.log("Your data.txt has been upgraded to data.yml");
		}
	}

	/**
	 * @see org.mineacademy.bfo.settings.YamlConfig#onSave()
	 */
	@Override
	protected void onSave() {
		final SerializedMap map = SerializedMap.ofArray("Players", this.registeredPlayers);

		for (final Map.Entry<String, Object> entry : map.entrySet())
			setNoSave(entry.getKey(), entry.getValue());
	}

	/**
	 * Register the player as "played" on the server
	 *
	 * @param player
	 */
	public void registerPlayer(final ProxiedPlayer player) {
		this.registeredPlayers.add(player.getUniqueId());

		save();
	}

	/**
	 * Is the player registered in our data.db file yet?
	 *
	 * @param player
	 * @return
	 */
	public boolean isPlayerRegistered(ProxiedPlayer player) {
		return registeredPlayers.contains(player.getUniqueId());
	}
}
