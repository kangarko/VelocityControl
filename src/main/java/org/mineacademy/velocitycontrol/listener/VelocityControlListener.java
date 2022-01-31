package org.mineacademy.velocitycontrol.listener;

import com.james090500.CoreFoundation.collection.SerializedMap;
import com.james090500.CoreFoundation.collection.StrictMap;
import com.james090500.CoreFoundation.debug.Debugger;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.mineacademy.velocitycontrol.SyncedCache;
import org.mineacademy.velocitycontrol.VelocityControl;
import org.mineacademy.velocitycontrol.model.ProxyPacket;
import org.mineacademy.velocitycontrol.settings.Settings;

import java.util.Map;
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
    private volatile StrictMap<SyncType, SerializedMap> clusteredData = new StrictMap<>();

    public enum SyncType {
        SERVER,
        NICK,
        VANISH,
        AFK,
        IGNORE,
        IGNORE_PMS,
        CHANNELS,
        VAULT
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

                for (final Map.Entry<SyncType, SerializedMap> entry : this.clusteredData.entrySet()) {

                    final SyncType syncType = entry.getKey();
                    final SerializedMap data = entry.getValue();
                    final OutgoingMessage message = new OutgoingMessage(ProxyPacket.PLAYERS_CLUSTER_DATA);

                    message.writeString(syncType.toString());
                    message.writeString(data.toJson());

                    VelocityControl.broadcastPacket(message);
                    SyncedCache.upload(syncType, data);
                }

                this.clusteredData.clear();
            }
        }).delay(1000, TimeUnit.MILLISECONDS)
        .repeat(1000, TimeUnit.MILLISECONDS)
        .schedule();
    }

    /**
     * @see org.mineacademy.bfo.bungee.BungeeListener#onMessageReceived(Connection, org.mineacademy.bfo.bungee.message.IncomingMessage)
     */
    @Subscribe
    public void onMessageReceived(PluginMessageEvent event) {
        if (event.getIdentifier() != VelocityControl.CHANNEL) return;

        // Set the connection early to use later
        this.connection = (ServerConnection) event.getSource();
        IncomingMessage message = new IncomingMessage(event.getData());

        try {
            handle(connection, message);

        } catch (final Throwable t) {
            t.printStackTrace();
            /*VelocityControl.getLogger().error(
                    Common.consoleLine(),
                    "ERROR COMMUNICATING WITH CHATCONTROL",
                    Common.consoleLine(),
                    "Ensure you are running latest version of",
                    "both BungeeControl and ChatControl!",
                    "",
                    "Server: " + connection.getServerInfo().getName(),
                    "Error: " + t.getClass().getSimpleName() + ": " + t.getMessage());*/
        }
    }

    /*
     * Handle and process incoming packet
     */
    private void handle(ServerConnection connection, IncomingMessage message) {
        // Get the raw data
        final byte[] data = message.getData();

        // Read the first three values of the packet, these are always the same
        this.senderUid = message.getSenderUid();
        this.serverName = Settings.getServerNameAlias(message.getServerName());

        final ProxyPacket packet = message.getAction();


        if (packet != ProxyPacket.PLAYERS_CLUSTER_DATA && packet != ProxyPacket.PLAYERS_CLUSTER_HEADER) {
            VelocityControl.getLogger().debug("Incoming packet " + packet + " from " + serverName);
        }

        if (packet == ProxyPacket.PLAYERS_CLUSTER_DATA) {
            synchronized (this.clusteredData) {
                final SyncType syncType = SyncType.valueOf(message.readString());
                final SerializedMap dataMap = message.readMap();

                final SerializedMap oldData = this.clusteredData.getOrDefault(syncType, new SerializedMap());
                oldData.mergeFrom(dataMap);

                this.clusteredData.override(syncType, oldData);
            }
        } else if (packet == ProxyPacket.FORWARD_COMMAND) {
            final String server = message.readString();
            final String command = message.readString().replace("{server_name}", serverName);

            if ("bungee".equals(server)) {
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
                Debugger.debug("packet", "\tDid not send to '" + iteratedName + "', the server is empty");

                continue;
            }

            if (!forceSelf && iteratedServer.getServerInfo().getAddress().equals(this.connection.getServerInfo().getAddress())) {
                Debugger.debug("packet", "\tDid not send to '" + iteratedName + "', the server equals sender");

                continue;
            }

            Debugger.debug("packet", "\tForwarded to '" + iteratedName + "'");
            iteratedServer.sendPluginMessage(VelocityControl.CHANNEL, data);
        }
    }
}