package com.discordnotifier;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("discordnotifier")
public interface DiscordNotifierConfig extends Config {

    // Leveling config section
    @ConfigSection(
            name = "Leveling",
            description = "The config for leveling notifications",
            position = 1,
            closedByDefault = true
    )
    String levelingConfig = "levelingConfig";

    @ConfigItem(
            keyName = "includeLeveling",
            name = "Send Leveling Notifications",
            description = "Send messages when you level up a skill.",
            section = levelingConfig,
            position = 1
    )
    default boolean includeLeveling() {
        return false;
    }

    @ConfigItem(
            keyName = "levelingWebhook",
            name = "Leveling Webhook URL",
            description = "The Discord Webhook URL(s) to send leveling messages to, separated by a newline.",
            section = levelingConfig,
            position = 2
    )
    String levelingWebhook();

    @ConfigItem(
            keyName = "minimumLevel",
            name = "Minimum level",
            description = "Levels greater than or equal to this value will send a message.",
            section = levelingConfig,
            position = 3
    )
    default int minLevel() {
        return 0;
    }

    @ConfigItem(
            keyName = "levelInterval",
            name = "Send every X levels",
            description = "Only levels that are a multiple of this value are sent. Level 99 will always be sent regardless of this value.",
            section = levelingConfig,
            position = 4
    )
    default int levelInterval() {
        return 1;
    }

    @ConfigItem(
            keyName = "linearLevelModifier",
            name = "Linear Level Modifier",
            description = "Send every `max(-.1x + linearLevelMax, 1)` levels. Will override `Send every X levels` if set to above zero.",
            section = levelingConfig,
            position = 5
    )
    default double linearLevelMax() {
        return 0;
    }

    @ConfigItem(
            keyName = "levelMessage",
            name = "Level Message",
            description = "Message to send to Discord on Level",
            section = levelingConfig,
            position = 6
    )
    default String levelMessage() {
        return "**$name** leveled $skill to $level";
    }

    @ConfigItem(
            keyName = "andLevelMessage",
            name = "And Level Message",
            description = "Message to send to Discord when Multi Skill Level",
            section = levelingConfig,
            position = 7
    )
    default String andLevelMessage() {
        return ", and $skill to $level";
    }

    @ConfigItem(
            keyName = "includeTotalLevelMessage",
            name = "Include total level with message",
            description = "Include total level in the message to send to Discord.",
            section = levelingConfig,
            position = 8
    )
    default boolean includeTotalLevel() {
        return false;
    }

    @ConfigItem(
            keyName = "totalLevelMessage",
            name = "Total Level Message",
            description = "Message to send to Discord when Total Level is included.",
            section = levelingConfig,
            position = 9
    )
    default String totalLevelMessage() {
        return " - **Total Level: $total**";
    }

    @ConfigItem(
            keyName = "sendLevelingScreenshot",
            name = "Include leveling screenshots",
            description = "Include a screenshot when leveling up.",
            section = levelingConfig,
            position = 100
    )
    default boolean sendLevelingScreenshot() {
        return false;
    }
    // End leveling config section

    // Questing config section
    @ConfigSection(
            name = "Questing",
            description = "The config for questing notifications",
            position = 2,
            closedByDefault = true
    )
    String questingConfig = "questingConfig";

    @ConfigItem(
            keyName = "includeQuests",
            name = "Send Quest Notifications",
            description = "Send messages when you complete a quest.",
            section = questingConfig
    )
    default boolean includeQuestComplete() {
        return false;
    }

    @ConfigItem(
            keyName = "questingWebhook",
            name = "Questing Webhook URL",
            description = "The Discord Webhook URL(s) to send questing messages to, separated by a newline.",
            section = questingConfig,
            position = 1
    )
    String questingWebhook();

    @ConfigItem(
            keyName = "questMessage",
            name = "Quest Message",
            description = "Message to send to Discord on Quest",
            section = questingConfig,
            position = 2
    )
    default String questMessage() {
        return "**$name** has just completed: **$quest**";
    }

    @ConfigItem(
            keyName = "sendQuestingScreenshot",
            name = "Include quest screenshots",
            description = "Include a screenshot with the Discord notification when leveling up.",
            section = questingConfig,
            position = 100
    )
    default boolean sendQuestingScreenshot() {
        return false;
    }
    // End questing config section

    // Death config section
    @ConfigSection(
            name = "Deaths",
            description = "The config for death notifications",
            position = 3,
            closedByDefault = true
    )
    String deathConfig = "deathConfig";

    @ConfigItem(
            keyName = "includeDeaths",
            name = "Send Death Notifications",
            description = "Send messages when you die to Discord.",
            section = deathConfig
    )
    default boolean includeDeaths() {
        return false;
    }

    @ConfigItem(
            keyName = "deathWebhook",
            name = "Death Webhook URL",
            description = "The Discord Webhook URL(s) to send death messages to, separated by a newline.",
            section = deathConfig,
            position = 1
    )
    String deathWebhook();

    @ConfigItem(
            keyName = "deathMessage",
            name = "Death Message",
            description = "Message to send to Discord on Death",
            section = deathConfig,
            position = 2
    )
    default String deathMessage() {
        return "**$name** has just died!";
    }

    @ConfigItem(
            keyName = "sendDeathScreenshot",
            name = "Include death screenshots",
            description = "Include a screenshot with the Discord notification when you die.",
            section = deathConfig,
            position = 100
    )
    default boolean sendDeathScreenshot() {
        return false;
    }
    // End death config section

    // Clue config section
    @ConfigSection(
            name = "Clue Scrolls",
            description = "The config for clue scroll notifications",
            position = 4,
            closedByDefault = true
    )
    String clueConfig = "clueConfig";

    @ConfigItem(
            keyName = "includeClues",
            name = "Send Clue Notifications",
            description = "Send messages when you complete a clue scroll.",
            section = clueConfig
    )
    default boolean includeClues() {
        return false;
    }

    @ConfigItem(
            keyName = "clueWebhook",
            name = "Clue Webhook URL",
            description = "The Discord Webhook URL(s) to send clue messages to, separated by a newline.",
            section = clueConfig,
            position = 1
    )
    String clueWebhook();

    @ConfigItem(
            keyName = "clueMessage",
            name = "Clue Message",
            description = "Message to send to Discord on Clue",
            section = clueConfig,
            position = 2
    )
    default String clueMessage() {
        return "**$name** has just completed $tier clue scroll! \nThe treasure was worth **$value coins!**";
    }

    @ConfigItem(
            keyName = "sendClueScreenshot",
            name = "Include Clue screenshots",
            description = "Include a screenshot with the Discord notification when you complete a clue.",
            section = clueConfig,
            position = 100
    )
    default boolean sendClueScreenshot() {
        return false;
    }
    // End clue config section

    // Pet config section
    @ConfigSection(
            name = "Pets",
            description = "The config for pet notifications",
            position = 5,
            closedByDefault = true
    )
    String petConfig = "petConfig";

    @ConfigItem(
            keyName = "includePets",
            name = "Send Pet Notifications",
            description = "Send messages when you receive a pet.",
            section = petConfig
    )
    default boolean includePets() {
        return false;
    }

    @ConfigItem(
            keyName = "petWebhook",
            name = "Pet Webhook URL",
            description = "The Discord Webhook URL(s) to send pet messages to, separated by a newline.",
            section = petConfig,
            position = 1
    )
    String petWebhook();

    @ConfigItem(
            keyName = "petMessage",
            name = "Pet Message",
            description = "Message to send to Discord on Pet",
            section = petConfig,
            position = 2
    )
    default String petMessage() {
        return "**$name** has just received a pet!";
    }

    @ConfigItem(
            keyName = "sendPetScreenshot",
            name = "Include Pet screenshots",
            description = "Include a screenshot with the Discord notification when you receive a pet.",
            section = petConfig,
            position = 100
    )
    default boolean sendPetScreenshot() {
        return false;
    }

    @ConfigSection(
            name = "Valuable Drops",
            description = "The config for valuable drop notifications",
            position = 6,
            closedByDefault = true
    )
    String valuableDropConfig = "valuableDropConfig";

    @ConfigItem(
            keyName = "valuableDropWebhook",
            name = "Valuable Drop Webhook URL",
            description = "The Discord Webhook URL(s) to send valuable drop messages to, separated by a newline.",
            section = valuableDropConfig,
            position = 1
    )
    String valuableDropWebhook();

    @ConfigItem(
            keyName = "includeValuableDrop",
            name = "Valuable Drop Notifications",
            description = "Message to send to Discord on valuable drops",
            section = valuableDropConfig,
            position = 2
    )
    default boolean includeValuableDrop() {
        return false;
    }

    @ConfigItem(
            keyName = "valuableDropThreshold",
            name = "Valuable Drop Threshold",
            description = "The minimum value of the drop for it to send a Discord notification",
            section = valuableDropConfig,
            position = 3
    )
    default int valuableDropThreshold() {
        return 0;
    }

    @ConfigItem(
            keyName = "valuableDropMessage",
            name = "Valuable Drop Message",
            description = "Message to send to Discord when you receive a valuable drop",
            section = valuableDropConfig,
            position = 4
    )
    default String valuableDropMessage() {
        return "**$name** just received a valuable drop: **$itemName!** \nApprox Value: **$itemValue coins**";
    }

    @ConfigItem(
            keyName = "sendValuableDropScreenshot",
            name = "Include valuable drop screenshots",
            description = "Include a screenshot with the Discord notification when you get a valuable drop",
            section = valuableDropConfig,
            position = 5
    )
    default boolean sendValuableDropScreenshot() {
        return false;
    }

    @ConfigSection(
            name = "Collection logs",
            description = "The config for collection logs",
            position = 7,
            closedByDefault = true
    )
    String collectionLogsConfig = "collectionLogsConfig";

    @ConfigItem(
            keyName = "collectionLogsWebhook",
            name = "Collection Logs Webhook URL",
            description = "The Discord Webhook URL(s) to send collection logs messages to, separated by a newline.",
            section = collectionLogsConfig,
            position = 1
    )
    String collectionLogsWebhook();

    @ConfigItem(
            keyName = "includeCollectionLogs",
            name = "Collection Log Notifications",
            description = "Message to send to Discord on collection logs completions",
            section = collectionLogsConfig,
            position = 2
    )
    default boolean includeCollectionLogs() {
        return false;
    }

    @ConfigItem(
            keyName = "collectionLogMessage",
            name = "Collection log Message",
            description = "Message to send to Discord on collection logs completions",
            section = collectionLogsConfig,
            position = 3
    )
    default String collectionLogMessage() {
        return "**$name** has just completed a collection log: **$entry**";
    }

    @ConfigItem(
            keyName = "sendCollectionLogScreenshot",
            name = "Include collection log screenshots",
            description = "Include a screenshot with the Discord notification when you fill a new collection log slot",
            section = collectionLogsConfig,
            position = 4
    )
    default boolean sendCollectionLogScreenshot() {
        return false;
    }

    @ConfigSection(
            name = "Combat Achievements",
            description = "The config for combat achievements",
            position = 8,
            closedByDefault = true
    )
    String combatAchievementsConfig = "combatAchievementsConfig";

    @ConfigItem(
            keyName = "combatAchievementsWebhook",
            name = "Combat Achievements Webhook URL",
            description = "The Discord Webhook URL(s) to send combat achievements messages to, separated by a newline.",
            section = combatAchievementsConfig,
            position = 1
    )
    String combatAchievementsWebhook();

    @ConfigItem(
            keyName = "includeCombatAchievements",
            name = "Combat Achievements Notifications",
            description = "Message to send to Discord on combat achievements completions",
            section = combatAchievementsConfig,
            position = 2
    )
    default boolean includeCombatAchievements() {
        return false;
    }

    @ConfigItem(
            keyName = "combatAchievementsMessage",
            name = "Combat Achievement Message",
            description = "Message to send to Discord on combat achievements completions",
            section = combatAchievementsConfig,
            position = 3
    )
    default String combatAchievementsMessage() {
        return "**$name** has just completed $tier combat achievement: **$achievement**";
    }

    @ConfigItem(
            keyName = "sendCombatAchievementScreenshot",
            name = "Include combat achievements screenshots",
            description = "Include a screenshot with the Discord notification when you complete a combat achievement",
            section = combatAchievementsConfig,
            position = 100
    )
    default boolean sendCombatAchievementsScreenshot() {
        return false;
    }
}
