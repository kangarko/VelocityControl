package org.mineacademy.velocitycontrol;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;

import java.io.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The data.db file storing various data information
 */
public final class ServerCache {

    private final Gson GSON = new Gson();
    private final Path JSON_PATH = Path.of(VelocityControl.getFolder() + "/users.json");

    @Getter private static final ServerCache instance = new ServerCache();

    /**
     * A list of players who got "caught" up by this plugin,
     * used for first join messages.
     */
    private Set<UUID> registeredPlayers = new HashSet<>();

    /**
     * Load the file
     */
    private ServerCache() {
        try {
            // Make file if it doesn't exist
            if(!JSON_PATH.toFile().exists()) {
                JSON_PATH.toFile().createNewFile();
            }

            // Write data to the file
            Writer writer = new FileWriter(JSON_PATH.toFile());
            GSON.toJson(registeredPlayers, writer);
            writer.close();

            // Load file to list
            Reader reader = new FileReader(JSON_PATH.toFile());
            this.registeredPlayers = GSON.fromJson(reader, new TypeToken<HashSet<UUID>>(){}.getType());
            reader.close();
        } catch (IOException e) {
            VelocityControl.getLogger().error("Error loading users.json");
            this.registeredPlayers = new HashSet<>(); //Avoid a null pointer
            e.printStackTrace();
        }
    }

    /**
     * Register the player as "played" on the server
     *
     * @param player
     */
    public void registerPlayer(final Player player) {
        this.registeredPlayers.add(player.getUniqueId());
        try {
            Writer writer = new FileWriter(JSON_PATH.toFile());
            GSON.toJson(this.registeredPlayers, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Is the player registered in our data.db file yet?
     *
     * @param player
     * @return
     */
    public boolean isPlayerRegistered(Player player) {
        return registeredPlayers.contains(player.getUniqueId());
    }
}