package de.gurki.buddy;

import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.api.ModInitializer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.gurki.buddy.command.BuddyCommand;


public class AmethystBuddy implements ModInitializer
{
	public static final String MOD_ID = "de.gurki.buddy";
	public static final Logger LOGGER = LogManager.getLogger( MOD_ID );

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register( BuddyCommand::register );
	}
}
