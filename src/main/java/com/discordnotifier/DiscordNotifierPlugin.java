package com.discordnotifier;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.Text;
import okhttp3.*;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.runelite.api.widgets.WidgetID.QUEST_COMPLETED_GROUP_ID;
import static net.runelite.http.api.RuneLiteAPI.GSON;

@Slf4j
@PluginDescriptor(name = "Discord Notifier")
public class DiscordNotifierPlugin extends Plugin {
    private Hashtable<String, Integer> currentLevels;
    private ArrayList<String> leveledSkills;
    private boolean shouldSendLevelMessage = false;
    private boolean shouldSendQuestMessage = false;
    private boolean notificationStarted = false;
    private int ticksWaited = 0;

    private String collectedTier = "";
    private String collectedValue = "";

    private static final Pattern QUEST_PATTERN_1 = Pattern.compile(".+?ve\\.*? (?<verb>been|rebuilt|.+?ed)? ?(?:the )?'?(?<quest>.+?)'?(?: quest)?[!.]?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUEST_PATTERN_2 = Pattern.compile("'?(?<quest>.+?)'?(?: quest)? (?<verb>[a-z]\\w+?ed)?(?: f.*?)?[!.]?$", Pattern.CASE_INSENSITIVE);
    private static final ImmutableList<String> RFD_TAGS = ImmutableList.of("Another Cook", "freed", "defeated", "saved");
    private static final ImmutableList<String> WORD_QUEST_IN_NAME_TAGS = ImmutableList.of("Another Cook", "Doric", "Heroes", "Legends", "Observatory", "Olaf", "Waterfall");
    private static final Pattern COLLECTION_LOG_ITEM_REGEX = Pattern.compile("New item added to your collection log:.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMBAT_TASK_REGEX = Pattern.compile("Congratulations, you've completed an? (?:\\w+) combat task:.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern WELL_DONE_REGEX = Pattern.compile("Well done, you've completed the Treasure Trail!", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLUE_SCROLL_REGEX = Pattern.compile("You have completed (\\d+) (Beginner|Easy|Medium|Hard|Elite|Master) Treasure Trails\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern TREASURE_VALUE_REGEX = Pattern.compile("Your treasure is worth around (.*?) coins!", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUABLE_DROP_REGEX = Pattern.compile(".*Valuable drop: ([^<>]+?\\(((?:\\d+,?)+) coins\\))(?:</col>)?");

    private static final ImmutableList<String> PET_MESSAGES = ImmutableList.of("You have a funny feeling like you're being followed", "You feel something weird sneaking into your backpack", "You have a funny feeling like you would have been followed");

    @Inject
    private Client client;

    @Inject
    private DiscordNotifierConfig config;

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private DrawManager drawManager;

    @Provides
    DiscordNotifierConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DiscordNotifierConfig.class);
    }

    private static Map<String, String> caTierMap;

    static {
        caTierMap = new HashMap<>();
        caTierMap.put("1", "an easy");
        caTierMap.put("2", "a medium");
        caTierMap.put("3", "a hard");
        caTierMap.put("4", "an elite");
        caTierMap.put("5", "a master");
        caTierMap.put("6", "a grandmaster");
    }

    @Override
    protected void startUp() throws Exception {
        currentLevels = new Hashtable<String, Integer>();
        leveledSkills = new ArrayList<String>();
    }

    @Override
    protected void shutDown() throws Exception {
    }

    @Subscribe
    public void onUsernameChanged(UsernameChanged usernameChanged) {
        resetState();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState().equals(GameState.LOGIN_SCREEN)) {
            resetState();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {

        if (shouldSendQuestMessage && config.includeQuestComplete() && client.getWidget(WidgetInfo.QUEST_COMPLETED_NAME_TEXT) != null) {
            shouldSendQuestMessage = false;
            String text = client.getWidget(WidgetInfo.QUEST_COMPLETED_NAME_TEXT).getText();
            String questName = parseQuestCompletedWidget(text);
            sendQuestMessage(questName);
        }

        if (!shouldSendLevelMessage) {
            return;
        }

        if (ticksWaited < 2) {
            ticksWaited++;
            return;
        }

        shouldSendLevelMessage = false;
        ticksWaited = 0;
        sendLevelMessage();
    }

    @Subscribe
    public void onStatChanged(net.runelite.api.events.StatChanged statChanged) {
        if (!config.includeLeveling()) {
            return;
        }

        String skillName = statChanged.getSkill().getName();
        int newLevel = statChanged.getLevel();

        // .contains wasn't behaving so I went with == null
        Integer previousLevel = currentLevels.get(skillName);
        if (previousLevel == null || previousLevel == 0) {
            currentLevels.put(skillName, newLevel);
            return;
        }

        if (previousLevel != newLevel) {
            currentLevels.put(skillName, newLevel);

            // Certain activities can multilevel, check if any of the levels are valid for the message.
            for (int level = previousLevel + 1; level <= newLevel; level++) {
                if (shouldSendForThisLevel(level)) {
                    leveledSkills.add(skillName);
                    shouldSendLevelMessage = true;
                    break;
                }
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        String chatMessage = event.getMessage();
        chatMessage = Text.removeFormattingTags(chatMessage);

        Matcher valueMatcher = TREASURE_VALUE_REGEX.matcher(chatMessage);
        if (valueMatcher.find() && config.includeClues()) {
            collectedValue = valueMatcher.group(1);
            sendClueMessage(collectedTier, collectedValue);
        }

        Matcher clueMatcher = CLUE_SCROLL_REGEX.matcher(chatMessage);
        if (clueMatcher.find()) {
            String tier = clueMatcher.group(2);
            if (tier.equals("easy") || tier.equals("elite")) {
                collectedTier = "an " + tier;
            } else {
                collectedTier = "a " + tier;
            }
        }

        if (config.includePets() && PET_MESSAGES.stream().anyMatch(chatMessage::contains)) {
            sendPetMessage();
        }

        if (config.includeValuableDrop()) {
            Matcher valuableMatcher = VALUABLE_DROP_REGEX.matcher(chatMessage);
            if (valuableMatcher.find()) {
                int valuableDropValue = Integer.parseInt(valuableMatcher.group(2).replaceAll(",", ""));
                if (valuableDropValue >= config.valuableDropThreshold()) {
                    String[] valuableDrop = valuableMatcher.group(1).split(" \\(");
                    String valuableDropName = (String) Array.get(valuableDrop, 0);
                    String valuableDropValueString = valuableMatcher.group(2);
                    sendValuableDropMessage(valuableDropName, valuableDropValueString);
                }
            }
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath actorDeath) {
        if (!config.includeDeaths()) {
            return;
        }

        Actor actor = actorDeath.getActor();
        if (actor instanceof Player) {
            Player player = (Player) actor;
            if (player == client.getLocalPlayer()) {
                sendDeathMessage();
            }
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        int groupId = event.getGroupId();

        if (groupId == QUEST_COMPLETED_GROUP_ID) {
            shouldSendQuestMessage = true;
        }
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired scriptPreFired) {
        switch (scriptPreFired.getScriptId()) {
            case ScriptID.NOTIFICATION_START:
                notificationStarted = true;
                break;
            case ScriptID.NOTIFICATION_DELAY:
                if (!notificationStarted) {
                    return;
                }

                String topText = client.getVarcStrValue(VarClientStr.NOTIFICATION_TOP_TEXT);
                String bottomText = client.getVarcStrValue(VarClientStr.NOTIFICATION_BOTTOM_TEXT);

                if (topText.equalsIgnoreCase("Collection log") && config.includeCollectionLogs() && config.sendCollectionLogScreenshot()) {
                    String entry = Text.removeTags(bottomText).substring("New item:".length());
                    sendCollectionLogMessage(entry);
                }

                if (topText.equalsIgnoreCase("Combat Task Completed!") && config.includeCombatAchievements() && config.sendCombatAchievementsScreenshot() && client.getVarbitValue(Varbits.COMBAT_ACHIEVEMENTS_POPUP) == 0) {
                    String[] s = bottomText.split("<.*?>");
                    String task = s[1].replaceAll("[:?]", "");
                    String tier = caTierMap.get(s[2].stripLeading().split("")[1]);

                    sendCombatAchievementMessage(task, tier);
                }

                notificationStarted = false;
                break;
        }
    }

    private boolean shouldSendForThisLevel(int level) {
        return level >= config.minLevel() && levelMeetsIntervalRequirement(level);
    }

    private boolean levelMeetsIntervalRequirement(int level) {
        int levelInterval = config.levelInterval();

        if (config.linearLevelMax() > 0) {
            levelInterval = (int) Math.max(Math.ceil(-.1 * level + config.linearLevelMax()), 1);
        }

        return levelInterval <= 1 || level == 99 || level % levelInterval == 0;
    }

    private void sendQuestMessage(String questName) {
        String localName = client.getLocalPlayer().getName();

        String questMessageString = config.questMessage().replaceAll("\\$name", localName).replaceAll("\\$quest", questName);

        DiscordWebhookBody discordWebhookBody = new DiscordWebhookBody();
        discordWebhookBody.setContent(questMessageString);
        sendWebhook(discordWebhookBody, config.sendQuestingScreenshot(), config.questingWebhook());
    }

    private void sendCombatAchievementMessage(String task, String tier) {
        String localName = client.getLocalPlayer().getName();

        String combatAchievementMessageString = config.combatAchievementsMessage().replaceAll("\\$name", localName).replaceAll("\\$tier", tier).replaceAll("\\$achievement", task);

        DiscordWebhookBody discordWebhookBody = new DiscordWebhookBody();
        discordWebhookBody.setContent(combatAchievementMessageString);
        sendWebhook(discordWebhookBody, config.sendCombatAchievementsScreenshot(), config.combatAchievementsWebhook());
    }

    private void sendValuableDropMessage(String itemName, String itemValue) {
        String localName = client.getLocalPlayer().getName();

        String valuableDropMessageString = config.valuableDropMessage()
                .replaceAll("\\$name", localName)
                .replaceAll("\\$itemName", itemName)
                .replaceAll("\\$itemValue", itemValue);

        DiscordWebhookBody discordWebhookBody = new DiscordWebhookBody();
        discordWebhookBody.setContent(valuableDropMessageString);
        sendWebhook(discordWebhookBody, config.sendValuableDropScreenshot(), config.valuableDropWebhook());
    }

    private void sendCollectionLogMessage(String entry) {
        String localName = client.getLocalPlayer().getName();

        String collectionLogMessageString = config.collectionLogMessage().replaceAll("\\$name", localName).replaceAll("\\$entry", entry);

        DiscordWebhookBody discordWebhookBody = new DiscordWebhookBody();
        discordWebhookBody.setContent(collectionLogMessageString);
        sendWebhook(discordWebhookBody, config.sendCollectionLogScreenshot(), config.collectionLogsWebhook());
    }

    private void sendDeathMessage() {
        String localName = client.getLocalPlayer().getName();

        String deathMessageString = config.deathMessage().replaceAll("\\$name", localName);

        DiscordWebhookBody discordWebhookBody = new DiscordWebhookBody();
        discordWebhookBody.setContent(deathMessageString);
        sendWebhook(discordWebhookBody, config.sendDeathScreenshot(), config.deathWebhook());
    }

    private void sendClueMessage(String tier, String value) {
        String localName = client.getLocalPlayer().getName();

        String clueMessage = config.clueMessage().replaceAll("\\$name", localName).replaceAll("\\$tier", tier).replaceAll("\\$value", value);

        DiscordWebhookBody discordWebhookBody = new DiscordWebhookBody();
        discordWebhookBody.setContent(clueMessage);
        sendWebhook(discordWebhookBody, config.sendClueScreenshot(), config.clueWebhook());
    }

    private void sendLevelMessage() {
        String localName = client.getLocalPlayer().getName();

        String levelUpString = config.levelMessage().replaceAll("\\$name", localName);

        String[] skills = new String[leveledSkills.size()];
        skills = leveledSkills.toArray(skills);
        leveledSkills.clear();

        for (int i = 0; i < skills.length; i++) {
            if (i != 0) {
                levelUpString += config.andLevelMessage();
            }

            if (config.includeTotalLevel()) {
                levelUpString += config.totalLevelMessage();
            }

            String fixed = levelUpString.replaceAll("\\$skill", skills[i]).replaceAll("\\$level", currentLevels.get(skills[i]).toString()).replaceAll("\\$total", Integer.toString(client.getTotalLevel()));

            levelUpString = fixed;
        }

        DiscordWebhookBody discordWebhookBody = new DiscordWebhookBody();
        discordWebhookBody.setContent(levelUpString);
        sendWebhook(discordWebhookBody, config.sendLevelingScreenshot(), config.levelingWebhook());
    }

    private void sendPetMessage() {
        String localName = client.getLocalPlayer().getName();

        String petMessageString = config.petMessage().replaceAll("\\$name", localName);

        DiscordWebhookBody discordWebhookBody = new DiscordWebhookBody();
        discordWebhookBody.setContent(petMessageString);
        sendWebhook(discordWebhookBody, config.sendPetScreenshot(), config.petWebhook());
    }

    private void sendWebhook(DiscordWebhookBody discordWebhookBody, boolean sendScreenshot, String configUrl) {
        if (Strings.isNullOrEmpty(configUrl)) {
            return;
        }

        List<String> webhookUrls = Arrays.asList(configUrl.split("\n")).stream().filter(u -> u.length() > 0).map(u -> u.trim()).collect(Collectors.toList());

        for (String webhookUrl : webhookUrls) {
            HttpUrl url = HttpUrl.parse(webhookUrl);
            MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("payload_json", GSON.toJson(discordWebhookBody));

            if (sendScreenshot) {
                sendWebhookWithScreenshot(url, requestBodyBuilder);
            } else {
                buildRequestAndSend(url, requestBodyBuilder);
            }
        }
    }

    private void sendWebhookWithScreenshot(HttpUrl url, MultipartBody.Builder requestBodyBuilder) {
        drawManager.requestNextFrameListener(image -> {
            BufferedImage bufferedImage = (BufferedImage) image;
            byte[] imageBytes;
            try {
                imageBytes = convertImageToByteArray(bufferedImage);
            } catch (IOException e) {
                log.warn("Error converting image to byte array", e);
                return;
            }

            requestBodyBuilder.addFormDataPart("file", "image.png", RequestBody.create(MediaType.parse("image/png"), imageBytes));

            buildRequestAndSend(url, requestBodyBuilder);
        });
    }

    private void buildRequestAndSend(HttpUrl url, MultipartBody.Builder requestBodyBuilder) {
        RequestBody requestBody = requestBodyBuilder.build();

        Request request = new Request.Builder().url(url).post(requestBody).build();

        sendRequest(request);
    }

    private void sendRequest(Request request) {
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.debug("Error submitting webhook", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.close();
            }
        });
    }

    private static byte[] convertImageToByteArray(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private void resetState() {
        currentLevels.clear();
        leveledSkills.clear();
        shouldSendLevelMessage = false;
        shouldSendQuestMessage = false;
        ticksWaited = 0;
    }

    static String parseQuestCompletedWidget(final String text) {
        // "You have completed The Corsair Curse!"
        final Matcher questMatch1 = QUEST_PATTERN_1.matcher(text);
        // "'One Small Favour' completed!"
        final Matcher questMatch2 = QUEST_PATTERN_2.matcher(text);
        final Matcher questMatchFinal = questMatch1.matches() ? questMatch1 : questMatch2;

        if (!questMatchFinal.matches()) {
            return "Unable to find quest name!";
        }

        String quest = questMatchFinal.group("quest");
        String verb = questMatchFinal.group("verb") != null ? questMatchFinal.group("verb") : "";

        if (verb.contains("kind of")) {
            quest += " partial completion";
        } else if (verb.contains("completely")) {
            quest += " II";
        }

        if (RFD_TAGS.stream().anyMatch((quest + verb)::contains)) {
            quest = "Recipe for Disaster - " + quest;
        }

        if (WORD_QUEST_IN_NAME_TAGS.stream().anyMatch(quest::contains)) {
            quest += " Quest";
        }

        return quest;
    }
}
