package org.mineacademy.bungeecontrol.hook;

import org.mineacademy.bfo.model.Replacer;
import org.mineacademy.bungeecontrol.SyncedCache;
import org.mineacademy.bungeecontrol.settings.Settings;

import de.simonsator.partyandfriends.api.pafplayers.DisplayNameProvider;
import de.simonsator.partyandfriends.api.pafplayers.OnlinePAFPlayer;
import de.simonsator.partyandfriends.api.pafplayers.PAFPlayer;
import de.simonsator.partyandfriends.api.pafplayers.PAFPlayerClass;

public final class PartiesAndFriendsHook implements DisplayNameProvider {

	@Override
	public String getDisplayName(PAFPlayer player) {
		final SyncedCache cache = SyncedCache.fromName(player.getName());

		return cache != null ? Replacer.replaceVariables(Settings.Integration.PARTIES_PLAYER_NAME, cache.toVariables()) : player.getName();
	}

	@Override
	public String getDisplayName(OnlinePAFPlayer player) {
		final SyncedCache cache = SyncedCache.fromName(player.getName());

		return cache != null ? Replacer.replaceVariables(Settings.Integration.PARTIES_PLAYER_NAME, cache.toVariables()) : player.getName();
	}

	public static void register() {
		final PartiesAndFriendsHook hook = new PartiesAndFriendsHook();

		//Main.getInstance().registerExtension(hook);
		PAFPlayerClass.setDisplayNameProvider(hook);
	}
}
