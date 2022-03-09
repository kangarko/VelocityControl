package org.mineacademy.velocitycontrol.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.mineacademy.velocitycontrol.VelocityControl;

public class CommandListener {

    @Subscribe
    public void onCommandEvent(CommandExecuteEvent event) {
        //Check send is a player and set player varialbe
        if(!(event.getCommandSource() instanceof Player)) return;
        Player commandSource = (Player) event.getCommandSource();

        //Check sender doesn't have a bypass permission
        if(commandSource.hasPermission("chatcontrol.bypass.spy")) return;

        //Loop through all players
        VelocityControl.getPlayers().forEach(player -> {
            //Don't send spy to themselves
            if(commandSource.getGameProfile().getId().equals(player.getUniqueId())) return;

            //Send spy message to all with permission
            if(player.hasPermission("chatcontrol.command.spy")) {
                String spyString = String.format("[Spy] [P] {0}: {1}", commandSource.getGameProfile().getName(), event.getCommand());
                player.sendMessage(Component.text(spyString));
            }
        });
    }

}
