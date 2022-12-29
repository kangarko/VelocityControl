package org.mineacademy.velocitycontrol.listener;

import com.google.gson.Gson;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.mineacademy.velocitycontrol.SyncedCache;
import org.mineacademy.velocitycontrol.VelocityControl;
import org.mineacademy.velocitycontrol.model.ProxyPacket;
import org.mineacademy.velocitycontrol.settings.Settings;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * Represents our core packet handling that reads and forwards
 * packets from Spigot servers
 */
public final class VelocityControlListener {
    public VelocityControlListener() {
        scheduleSyncTask();
    }

    /**
     * The present connection, always updated when new packet is received
     */
    private ServerConnection connection;

    /**
     * The present senders UUID
     */
    private UUID senderUid;

    /**
     * The present server name
     */
    private String serverName;

    /**
     * The data that are being synced between servers
     */
    private HashMap<SyncType, HashMap> clusteredData = new HashMap<>();

    public enum SyncType {
    }

    /**
     * Reschedule data sync task
     */
    public void scheduleSyncTask() {

        VelocityControl.getServer().getScheduler().buildTask(VelocityControl.getInstance(), () -> {
            // Upload the always reliable player list from Bungee (do not compile lists given by downstream
            // due to poor performance in the past)
            SyncedCache.updateForOnlinePlayers();

            // Upload downstream data here and redistribute over network
            synchronized (this.clusteredData) {

                Gson gson = new Gson();
                this.clusteredData.forEach((syncType, hashMap) -> {
                    final OutgoingMessage message = new OutgoingMessage(ProxyPacket.PLAYERS_CLUSTER_DATA);

                    message.writeString(syncType.toString());
                    message.writeString(gson.toJson(hashMap));

                    VelocityControl.broadcastPacket(message);
                    SyncedCache.upload(syncType, hashMap);
                });

                this.clusteredData.clear();
            }
        }).delay(1000, TimeUnit.MILLISECONDS)
        .repeat(1000, TimeUnit.MILLISECONDS)
        .schedule();
    }

    @Subscribe
    public void onMessageReceived(PluginMessageEvent event) {
        if (event.getIdentifier() != VelocityControl.CHANNEL) return;

        // Set the connection early to use later
        this.connection = (ServerConnection) event.getSource();
        IncomingMessage message = new IncomingMessage(event.getData());

        try {
            handle(message);

        } catch (final Throwable t) {
            t.printStackTrace();
            VelocityControl.getLogger().error(
                    "!-----------------------------------------------------!",
                    "ERROR COMMUNICATING WITH CHATCONTROL",
                    "!-----------------------------------------------------!",
                    "Ensure you are running latest version of",
                    "both VelocityControl and ChatControl!",
                    "",
                    "Server: " + connection.getServerInfo().getName(),
                    "Error: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }

        //Set message has handles to avoid client spam/forwarding
        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    /*
     * Handle and process incoming packet
     */
    private void handle(IncomingMessage message) {
        // Get the raw data
        final byte[] data = message.getData();

        // Read the first three values of the packet, these are always the same
        this.senderUid = message.getSenderUid();
        this.serverName = Settings.getServerNameAlias(message.getServerName());

        final ProxyPacket packet = message.getAction();

//        if (packet != ProxyPacket.PLAYERS_CLUSTER_DATA && packet != ProxyPacket.PLAYERS_CLUSTER_HEADER) {
//            VelocityControl.getLogger().debug("Incoming packet " + packet + " from " + serverName);
//        }

        if (packet == ProxyPacket.PLAYERS_CLUSTER_DATA) {
            synchronized (this.clusteredData) {
                final SyncType syncType = SyncType.valueOf(message.readString());
                final HashMap dataMap = message.readMap();

                final HashMap oldData = this.clusteredData.getOrDefault(syncType, new HashMap());
                dataMap.forEach((key, value) -> {
                    if(key != null && value != null && !oldData.containsKey(key)) {
                        oldData.put(key, value);
                    }
                });
                this.clusteredData.put(syncType, oldData);
            }
        } else if (packet == ProxyPacket.FORWARD_COMMAND) {
            final String server = message.readString();
            final String command = message.readString().replace("{server_name}", serverName);

            if ("velocity".equals(server)) {
                VelocityControl.getServer().getCommandManager().executeAsync(VelocityControl.getServer().getConsoleCommandSource(), command);
            } else {
                forwardData(data, false);
            }
        } else if (packet == ProxyPacket.CONFIRM_PLAYER_READY) {
            final UUID uniqueId = message.readUUID();
            final String syncedCacheLine = message.readString();
            final Optional<Player> player = VelocityControl.getServer().getPlayer(uniqueId);

            if (player.isPresent()) {
                SyncedCache.uploadSingle(player.get().getUsername(), uniqueId, syncedCacheLine);
                SwitchListener.broadcastPendingMessage(player.get());
            }
        } else {
            forwardData(data, packet == ProxyPacket.DB_UPDATE);
        }
    }

    /*
     * Forward the given data with optional sender unique ID to all other servers
     * or Redis
     */
    private void forwardData(byte[] data, boolean forceSelf) {
        for (final RegisteredServer server : VelocityControl.getServers()) {
            final String iteratedName = server.getServerInfo().getName();
            final RegisteredServer iteratedServer = server;

            if (iteratedServer.getPlayersConnected().isEmpty()) {
                VelocityControl.getLogger().debug("packet", "\tDid not send to '" + iteratedName + "', the server is empty");

                continue;
            }

            if (!forceSelf && iteratedServer.getServerInfo().getAddress().equals(this.connection.getServerInfo().getAddress())) {
                VelocityControl.getLogger().debug("packet", "\tDid not send to '" + iteratedName + "', the server equals sender");

                continue;
            }

            VelocityControl.getLogger().debug("packet", "\tForwarded to '" + iteratedName + "'");
            iteratedServer.sendPluginMessage(VelocityControl.CHANNEL, data);
        }
    }
}