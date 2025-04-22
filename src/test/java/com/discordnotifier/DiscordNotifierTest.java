package com.discordnotifier;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class DiscordNotifierTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(DiscordNotifierPlugin.class);
        RuneLite.main(args);
    }
}