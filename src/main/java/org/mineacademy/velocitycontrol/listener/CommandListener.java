package org.mineacademy.velocitycontrol.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.mineacademy.velocitycontrol.VelocityControl;
import org.mineacademy.velocitycontrol.settings.Settings;

import java.util.ArrayList;

public class CommandListener {

    @Subscribe(order = PostOrder.FIRST)
    public void onCommandEvent(CommandExecuteEvent event) {
        //If format is none we can turn it off
        final String spyFormat = Settings.getSettings().Spy.Format;
        if(spyFormat.equalsIgnoreCase("none")) return;

        //Lets not get commands that are forwarded!
        String commandOnly = event.getCommand().toLowerCase().split(" ")[0];
        if(!VelocityControl.getServer().getCommandManager().hasCommand(commandOnly)) return;

        //Check send is a player and set player varialbe
        if(!(event.getCommandSource() instanceof Player)) return;
        Player commandSource = (Player) event.getCommandSource();

        //Check sender doesn't have a bypass permission
        if(commandSource.hasPermission("chatcontrol.bypass.spy")) return;

        //Check if we are only spying specific commands
        ArrayList<String> spiedCommands = Settings.getSettings().Spy.Spied_Commands;
        if(spiedCommands != null && spiedCommands.size() > 0) {
            Boolean blackList = spiedCommands.contains("@blacklist");
            Boolean containCommand = spiedCommands.contains(commandOnly);

            //If we are a blacklist and we contain the command, let returns
            //If we are NOT a blacklist and DONT contain the command lets return;
            if((blackList && containCommand) || (!blackList && !containCommand)) return;
        }

        //Loop through all players
        VelocityControl.getPlayers().forEach(player -> {
            //Don't send spy to themselves
            //if(commandSource.getGameProfile().getId().equals(player.getUniqueId())) return;

            //Send spy message to all with permission
            if(player.hasPermission("chatcontrol.command.spy")) {
                String spyFormatFinal = spyFormat;
                spyFormatFinal = spyFormatFinal.replace("{player_name}", commandSource.getGameProfile().getName());
                spyFormatFinal = spyFormatFinal.replace("{message}", "/" + event.getCommand());
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(spyFormatFinal));
            }
        });
    }

}
