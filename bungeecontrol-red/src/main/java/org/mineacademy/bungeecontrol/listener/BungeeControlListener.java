package org.mineacademy.bungeecontrol.listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.mineacademy.bfo.Common;
import org.mineacademy.bfo.bungee.BungeeListener;
import org.mineacademy.bfo.bungee.message.IncomingMessage;
import org.mineacademy.bfo.bungee.message.OutgoingMessage;
import org.mineacademy.bfo.collection.SerializedMap;
import org.mineacademy.bfo.collection.StrictMap;
import org.mineacademy.bfo.debug.Debugger;
import org.mineacademy.bfo.plugin.SimplePlugin;
import org.mineacademy.bungeecontrol.BungeeControl;
import org.mineacademy.bungeecontrol.SyncedCache;
import org.mineacademy.bungeecontrol.model.BungeePacket;
import org.mineacademy.bungeecontrol.model.Redis;
import org.mineacademy.bungeecontrol.settings.Settings;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

/**
 * Represents our core packet handling that reads and forwards
 * packets from Spigot servers
 */
public final class BungeeControlListener extends BungeeListener {

	/**
	 * The present connection, always updated when new packet is received
	 */
	private Connection connection;

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
		VAULT,
	}

	/**
	 * Reschedule data sync task
	 */
	public void scheduleSyncTask() {

		ProxyServer.getInstance().getScheduler().schedule(SimplePlugin.getInstance(), () -> {

			// Upload the always reliable player list from Bungee (do not compile lists given by downstream
			// due to poor performance in the past)
			SyncedCache.updateForOnlinePlayers();

			// Upload downstream data here and redistribute over network
			synchronized (this.clusteredData) {
				for (final Map.Entry<SyncType, SerializedMap> entry : this.clusteredData.entrySet()) {

					final SyncType syncType = entry.getKey();
					final SerializedMap data = entry.getValue();
					final OutgoingMessage message = new OutgoingMessage(BungeePacket.PLAYERS_CLUSTER_DATA);

					message.writeString(syncType.toString());
					message.writeString(data.toJson());

					BungeeControl.broadcastPacket(message);
					SyncedCache.upload(syncType, data);
				}

				this.clusteredData.clear();

				Debugger.debug("player-sync", "Distributing network players: " + SyncedCache.getJustNames());
			}

		}, 1000, 1000, TimeUnit.MILLISECONDS);
	}

	/**
	 * @see org.mineacademy.bfo.bungee.BungeeListener#onMessageReceived(Connection, org.mineacademy.bfo.bungee.message.IncomingMessage)
	 */
	@Override
	public void onMessageReceived(Connection connection, IncomingMessage message) {

		// Set the connection early to use later
		this.connection = connection;

		try {
			handle(connection, message);

		} catch (final Throwable t) {
			Common.error(t,
					Common.consoleLine(),
					"ERROR COMMUNICATING WITH CHATCONTROL",
					Common.consoleLine(),
					"Ensure you are running latest version of",
					"both BungeeControl and ChatControl!",
					"",
					"Server: " + ((Server) connection).getInfo().getName(),
					"Error: " + t.getClass().getSimpleName() + ": " + t.getMessage());
		}
	}

	/*
	 * Handle and process incoming packet
	 */
	private void handle(Connection connection, IncomingMessage message) {

		// Get the raw data
		final byte[] data = message.getData();

		// Read the first three values of the packet, these are always the same
		this.senderUid = message.getSenderUid();
		this.serverName = Settings.getServerNameAlias(message.getServerName());

		final BungeePacket packet = (BungeePacket) message.getAction();

		if (packet != BungeePacket.PLAYERS_CLUSTER_DATA && packet != BungeePacket.PLAYERS_CLUSTER_HEADER)
			Debugger.debug("bungee", "Incoming packet " + packet + " from " + serverName);

		if (packet == BungeePacket.PLAYERS_CLUSTER_DATA)
			synchronized (this.clusteredData) {
				final SyncType syncType = SyncType.valueOf(message.readString());
				final SerializedMap dataMap = message.readMap();

				final SerializedMap oldData = this.clusteredData.getOrDefault(syncType, new SerializedMap());
				oldData.mergeFrom(dataMap);

				this.clusteredData.override(syncType, oldData);
			}

		else if (packet == BungeePacket.FORWARD_COMMAND) {
			final String server = message.readString();
			final String command = Common.colorize(message.readString().replace("{server_name}", serverName));

			if ("bungee".equals(server)) {
				if (BungeeControl.isRedisFound())
					Redis.dispatchCommand(command);
				else
					getProxy().getPluginManager().dispatchCommand(getProxy().getConsole(), command);
			}

			else
				forwardData(data, false);
		}

		else if (packet == BungeePacket.CONFIRM_PLAYER_READY) {
			final UUID uniqueId = message.readUUID();
			final String syncedCacheLine = message.readString();
			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uniqueId);

			if (player != null) {
				SyncedCache.uploadSingle(player.getName(), uniqueId, syncedCacheLine);

				SwitchListener.broadcastPendingMessage(player);
			}

			else
				Debugger.debug("player-message", "Failed finding player " + uniqueId + " for player message, assuming he disconnected. Quitting..");
		}

		else
			forwardData(data, packet == BungeePacket.DB_UPDATE);
	}

	/*
	 * Forward the given data with optional sender unique ID to all other servers
	 * or Redis
	 */
	private void forwardData(byte[] data, boolean forceSelf) {

		final String fromCluster = Settings.Clusters.getFromServerName(this.serverName);

		if (BungeeControl.isRedisFound())
			Redis.sendDataToOtherServers(this.senderUid, BungeeControl.CHANNEL, data);

		else
			for (final Map.Entry<String, ServerInfo> entry : getProxy().getServers().entrySet()) {
				final String iteratedName = entry.getKey();
				final ServerInfo iteratedServer = entry.getValue();
				final String iteratedCluster = Settings.Clusters.getFromServerName(iteratedName);

				if (iteratedServer.getPlayers().isEmpty()) {
					Debugger.debug("bungee", "\tDid not send to '" + iteratedName + "', the server is empty");

					continue;
				}

				if (!forceSelf && !iteratedCluster.equals(fromCluster)) {
					Debugger.debug("bungee", "\tDid not send to '" + iteratedName + "', the server has different cluster (" + iteratedCluster + " != " + fromCluster + ")");

					continue;
				}

				if (!forceSelf && iteratedServer.getAddress().equals(this.connection.getAddress())) {
					Debugger.debug("bungee", "\tDid not send to '" + iteratedName + "', the server equals sender");

					continue;
				}

				Debugger.debug("bungee", "\tForwarded to '" + iteratedName + "'");
				iteratedServer.sendData(BungeeControl.CHANNEL, data);
			}
	}
}