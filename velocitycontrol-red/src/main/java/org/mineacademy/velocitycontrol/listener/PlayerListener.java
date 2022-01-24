package org.mineacademy.velocitycontrol.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.mineacademy.velocitycontrol.VelocityControl;
import org.mineacademy.velocitycontrol.model.ProxyPacket;
import org.mineacademy.velocitycontrol.settings.Settings;

public final class PlayerListener {

    /**
     * Notify downstream what server name alias we configured here
     *
     * @param event
     */
    @Subscribe
    public void onJoin(final ServerConnectedEvent event) {
        final Player player = event.getPlayer();
        final RegisteredServer server = event.getServer();

        // Prepare packet
        final OutgoingMessage message = new OutgoingMessage(ProxyPacket.SERVER_ALIAS);

        message.writeString(server.getServerInfo().getName());
        message.writeString(Settings.getServerNameAlias(server));

        VelocityControl.forwardMessage(message, player);
    }
}
