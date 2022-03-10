package org.mineacademy.velocitycontrol.command;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.mineacademy.velocitycontrol.operator.PlayerMessages;
import org.mineacademy.velocitycontrol.settings.Settings;

public class ReloadCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        Settings.load();
        PlayerMessages.getInstance().load();
        invocation.source().sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&a[VelocityControl] Reloaded"));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("chatcontrol.command.reload");
    }
}
