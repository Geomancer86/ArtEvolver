package com.rndmodgames.evolver.clicker;

import com.rndmodgames.evolver.ImageEvolver;
import com.rndmodgames.evolver.Palette;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core game state for the Evolution Clicker.
 *
 * Design: each click attempts a random two-triangle color swap. The swap may or
 * may not improve fitness. Upgrades unlock retry cycles, more swaps per click,
 * distance control, and smart targeting — all starting from a pure-random baseline.
 *
 * All game state resets when a new image is loaded (initEngine). Prestige upgrades
 * persist across ascensions but not across image changes.
 *
 * Pacing modeled after Cookie Clicker: slow start, exponential growth, long gaps
 * between meaningful upgrades.
 */
public class ClickerState {

    // ════════════════════════════════════════════════════════════════
    //  CURRENCIES
    // ════════════════════════════════════════════════════════════════
    private double ep = 0;
    private double totalEpEarned = 0;
    private double mc = 0;
    private double gf = 0;
    private long totalClicks = 0;
    private long totalUpgradesBought = 0;
    private long gameStartMs = System.currentTimeMillis();
    private long totalPlayTimeMs = 0;
    private int ascensionCount = 0;
    private int totalAchievementsUnlocked = 0;

    private double epPerSecond = 0;

    // Auto-clicker accumulator
    private double autoClickAccumulator = 0;

    // Active events
    private final List<ActiveEvent> activeEvents = new ArrayList<>();
    private long lastEventCheckMs = 0;

    // Upgrade levels
    private final Map<String, Integer> upgradeLevels = new ConcurrentHashMap<>();

    // Unlocked achievements
    private final Set<String> unlockedAchievements = Collections.synchronizedSet(new LinkedHashSet<>());
    private final List<String> achievementQueue = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_POPUPS_PER_TICK = 1;

    // Engine reference
    private ClickerEngine engine;

    // Init params (for prestige reset)
    private Palette palette;
    private int gridW, gridH;
    private float triWidth, triHeight, scale;

    // ════════════════════════════════════════════════════════════════
    //  UPGRADE DEFINITIONS — progression-focused, all start from zero
    // ════════════════════════════════════════════════════════════════

    public static final UpgradeDef[] UPGRADES = {
        // ─── CLICK POWER (EP) ── unlocks gradually
        new UpgradeDef("retry_cycles", "Retry Cycles", "Click Power",
                "Try multiple random swaps per click and keep the best one. More cycles = better odds.",
                25, 1.25, 20, "ep", "retryCycles", 1, "flat"),
        new UpgradeDef("multi_swap", "Multi-Swap", "Click Power",
                "Perform additional swap attempts per click.",
                100, 1.35, 15, "ep", "multiSwap", 1, "flat"),
        new UpgradeDef("swap_distance", "Swap Range", "Click Power",
                "Limit swap distance to nearby triangles for more coherent improvements.",
                50, 1.20, 20, "ep", "swapDistance", 1, "flat"),
        new UpgradeDef("click_value", "Click Reward", "Click Power",
                "Increases base EP earned per click.",
                10, 1.12, 50, "ep", "clickValue", 0.5, "flat"),

        // ─── AUTOMATION (EP, expensive) ───
        new UpgradeDef("auto_clicker", "Auto-Clicker", "Automation",
                "Automatic clicks per second. Starts slow.",
                500, 1.25, 15, "ep", "autoClicker", 0.2, "flat"),
        new UpgradeDef("auto_cycles", "Auto Retry", "Automation",
                "Auto-clickers get extra retry cycles.",
                1000, 1.30, 10, "ep", "autoCycles", 1, "flat"),
        new UpgradeDef("auto_multi", "Auto Multi-Swap", "Automation",
                "Auto-clickers perform extra swaps per tick.",
                2000, 1.35, 10, "ep", "autoMulti", 1, "flat"),

        // ─── INTELLIGENCE (EP, mid-tier) ───
        new UpgradeDef("smart_pick", "Smart Pick", "Intelligence",
                "Chance to target the worst-matching triangle instead of random.",
                200, 1.22, 15, "ep", "smartPick", 0.05, "flat"),
        new UpgradeDef("local_focus", "Local Focus", "Intelligence",
                "Prefer swapping nearby triangles for spatial coherence.",
                150, 1.20, 15, "ep", "localFocus", 1, "flat"),

        // ─── SPECIAL (MC — rare currency) ───
        new UpgradeDef("critical_swap", "Critical Swap", "Special",
                "Chance for a critical click that does 3x swap attempts.",
                5, 1.45, 15, "mc", "criticalSwap", 0.02, "flat"),
        new UpgradeDef("mc_finder", "Crystal Finder", "Special",
                "Small chance to find Mutation Crystals per click.",
                3, 1.40, 20, "mc", "mcFinder", 0.002, "flat"),
        new UpgradeDef("ep_multiplier", "EP Overflow", "Special",
                "Multiplies ALL Evolution Point gains.",
                15, 1.55, 30, "mc", "epMultiplier", 0.08, "mult"),
        new UpgradeDef("lucky_events", "Lucky Star", "Special",
                "Increases random event spawn rate.",
                8, 1.35, 15, "mc", "luckyEvents", 0.05, "flat"),
        new UpgradeDef("auto_boost", "Turbo Auto", "Special",
                "Auto-clicker speed increased by 50% per level.",
                20, 1.50, 10, "mc", "autoBoost", 0.5, "flat"),

        // ─── PRESTIGE (GF — after first ascension) ───
        new UpgradeDef("eternal_cycles", "Eternal Cycles", "Prestige",
                "Permanent extra retry cycles that persist through ascensions.",
                5, 1.65, 20, "gf", "eternalCycles", 1, "flat"),
        new UpgradeDef("eternal_auto", "Eternal Speed", "Prestige",
                "Permanent auto-clicker speed bonus.",
                8, 1.60, 20, "gf", "eternalAuto", 0.1, "flat"),
        new UpgradeDef("smart_init", "Smart Genesis", "Prestige",
                "Start with Smart initialization after ascension (much better starting fitness).",
                25, 2.00, 1, "gf", "smartInit", 1, "flat"),
        new UpgradeDef("lap_init", "LAP Genesis", "Prestige",
                "Start with LAP Optimal initialization (provably best starting arrangement).",
                100, 2.00, 1, "gf", "lapInit", 1, "flat"),
    };

    // ════════════════════════════════════════════════════════════════
    //  ACHIEVEMENT DEFINITIONS — higher thresholds, slower pacing
    // ════════════════════════════════════════════════════════════════

    public static final AchievementDef[] ACHIEVEMENTS = generateAchievements();

    private static AchievementDef[] generateAchievements() {
        List<AchievementDef> list = new ArrayList<>();

        // Fitness milestones — these are HARD to reach from a random start
        double[] fitTiers = {5, 10, 15, 20, 25, 30, 35, 40, 50, 60, 70, 80, 90, 95, 99};
        String[] fitNames = {"First Light", "Faint Outline", "Seeing Colors", "Taking Shape",
                "Quarter Way", "Recognizable", "Getting Clearer", "Almost Half",
                "Halfway Home", "Past the Peak", "Masterwork", "Museum Quality",
                "Near Perfect", "Pixel Master", "Transcendent"};
        String[] fitIcons = {"\uD83C\uDF31", "\u270F\uFE0F", "\uD83C\uDF08", "\uD83D\uDD8C\uFE0F",
                "\uD83D\uDCCA", "\uD83D\uDC41\uFE0F", "\uD83D\uDCAA", "\uD83C\uDFA8",
                "\uD83C\uDFC6", "\uD83D\uDD25", "\uD83D\uDC8E", "\uD83D\uDC51",
                "\u2728", "\u269B\uFE0F", "\uD83C\uDF1F"};
        for (int i = 0; i < fitTiers.length; i++) {
            double reward = 50 * Math.pow(1.8, i);
            list.add(new AchievementDef("fit_" + i, fitNames[i], fitIcons[i],
                    "Reach " + fitTiers[i] + "% fitness", "fitness", fitTiers[i],
                    reward, 1 + i * 0.005, false));
        }

        // Click milestones — high numbers
        long[] clickTiers = {10, 50, 200, 500, 1000, 5000, 10000, 50000, 100000};
        String[] clickNames = {"First Clicks", "Getting Started", "Persistent", "Dedicated",
                "Thousand Clicks", "Click Veteran", "Ten Thousand", "Fifty Thousand", "Click Legend"};
        for (int i = 0; i < clickTiers.length; i++) {
            list.add(new AchievementDef("click_" + i, clickNames[i], "\uD83D\uDC46",
                    "Click " + formatBigNumber(clickTiers[i]) + " times", "clicks", clickTiers[i],
                    clickTiers[i] * 0.3, 1 + i * 0.003, false));
        }

        // Successful swap milestones
        long[] swapTiers = {1, 10, 50, 200, 1000, 5000, 20000, 100000};
        String[] swapNames = {"First Improvement", "Color Mixer", "Swap Novice", "Swap Apprentice",
                "Swap Master", "Five Thousand", "Twenty Thousand", "Swap Legend"};
        for (int i = 0; i < swapTiers.length; i++) {
            list.add(new AchievementDef("swap_" + i, swapNames[i], "\uD83D\uDD00",
                    formatBigNumber(swapTiers[i]) + " successful swaps", "successSwaps", swapTiers[i],
                    swapTiers[i] * 0.5, 1 + i * 0.004, false));
        }

        // EP milestones
        double[] epTiers = {100, 1000, 10000, 100000, 1e6, 1e8};
        String[] epNames = {"Pocket Change", "First Thousand", "Getting Rich", "Fortune",
                "Millionaire", "Tycoon"};
        for (int i = 0; i < epTiers.length; i++) {
            list.add(new AchievementDef("ep_" + i, epNames[i], "\uD83D\uDCB0",
                    "Earn " + formatBigNumber(epTiers[i]) + " total EP", "totalEp", epTiers[i],
                    epTiers[i] * 0.05, 1 + i * 0.005, false));
        }

        // Time milestones
        long[] timeTiers = {300, 1800, 3600, 10800, 43200};
        String[] timeNames = {"Five Minutes", "Half Hour", "Hour of Power", "Three Hours", "Half Day"};
        for (int i = 0; i < timeTiers.length; i++) {
            list.add(new AchievementDef("time_" + i, timeNames[i], "\u23F0",
                    "Play for " + formatDuration(timeTiers[i]), "playTime", timeTiers[i],
                    timeTiers[i] * 0.5, 1 + i * 0.005, false));
        }

        // Upgrade milestones
        int[] upgTiers = {1, 5, 15, 30, 50};
        String[] upgNames = {"First Purchase", "Investor", "Tinkerer", "Engineer", "Mad Scientist"};
        for (int i = 0; i < upgTiers.length; i++) {
            list.add(new AchievementDef("upg_" + i, upgNames[i], "\u2B06\uFE0F",
                    "Buy " + upgTiers[i] + " total upgrades", "upgrades", upgTiers[i],
                    upgTiers[i] * 15, 1 + i * 0.008, false));
        }

        // Prestige milestones
        int[] presTiers = {1, 3, 5, 10};
        String[] presNames = {"First Ascension", "Triple Ascended", "Quintuple", "Decade"};
        for (int i = 0; i < presTiers.length; i++) {
            list.add(new AchievementDef("pres_" + i, presNames[i], "\uD83C\uDF1F",
                    "Ascend " + presTiers[i] + " times", "ascensions", presTiers[i],
                    presTiers[i] * 200, 1 + (i + 1) * 0.02, false));
        }

        // Hidden discovery achievements
        list.add(new AchievementDef("disc_first_good", "Lucky Swap", "\uD83C\uDF40",
                "Your first improving swap!", "discovery", 1, 25, 1.02, true));
        list.add(new AchievementDef("disc_auto", "Hands Free", "\uD83E\uDD16",
                "Purchase your first auto-clicker", "discovery", 2, 100, 1.03, true));
        list.add(new AchievementDef("disc_prestige", "Ascended", "\uD83C\uDF1F",
                "Perform your first ascension", "discovery", 3, 500, 1.10, true));
        list.add(new AchievementDef("disc_critical", "Critical Strike!", "\u26A1",
                "Land your first critical click", "discovery", 4, 200, 1.05, true));
        list.add(new AchievementDef("disc_mc", "Crystal Drop", "\uD83D\uDC8E",
                "Find your first Mutation Crystal", "discovery", 5, 100, 1.05, true));

        return list.toArray(new AchievementDef[0]);
    }

    // ════════════════════════════════════════════════════════════════
    //  EVENT DEFINITIONS — rare, impactful, slow spawn rate
    // ════════════════════════════════════════════════════════════════

    public static final EventDef[] EVENTS = {
        new EventDef("swap_storm", "Swap Storm", "\uD83C\uDF2A\uFE0F",
                "2x swaps per click!", 25, 2.0, 0.001),
        new EventDef("golden_hour", "Golden Hour", "\u2B50",
                "3x EP from all sources!", 20, 3.0, 0.001),
        new EventDef("crystal_rain", "Crystal Rain", "\uD83D\uDC8E",
                "MC drops with every click!", 30, 1.0, 0.0008),
        new EventDef("auto_frenzy", "Auto Frenzy", "\u26A1",
                "2x auto-clicker speed!", 25, 2.0, 0.001),
        new EventDef("precision_wave", "Precision Wave", "\uD83C\uDFAF",
                "Extra retry cycles!", 30, 3.0, 0.0005),
        new EventDef("lucky_streak", "Lucky Streak", "\uD83C\uDF40",
                "2x critical chance!", 20, 2.0, 0.001),
    };

    // ════════════════════════════════════════════════════════════════
    //  ENGINE INITIALIZATION — resets ALL game state
    // ════════════════════════════════════════════════════════════════

    public ClickerEngine getEngine() { return engine; }

    /**
     * Initializes the clicker engine AND resets all game state.
     * Called when a new image is loaded or the clicker page is opened.
     */
    public void initEngine(BufferedImage sourceImage, Palette palette,
                           int gridW, int gridH,
                           float triWidth, float triHeight, float scale) {
        this.palette = palette;
        this.gridW = gridW;
        this.gridH = gridH;
        this.triWidth = triWidth;
        this.triHeight = triHeight;
        this.scale = scale;

        // Full reset of all currencies and progress
        ep = 0;
        totalEpEarned = 0;
        mc = 0;
        // GF persists across inits (prestige currency)
        totalClicks = 0;
        totalUpgradesBought = 0;
        gameStartMs = System.currentTimeMillis();
        totalPlayTimeMs = 0;
        totalAchievementsUnlocked = 0;
        epPerSecond = 0;
        autoClickAccumulator = 0;
        activeEvents.clear();
        lastEventCheckMs = 0;
        achievementQueue.clear();

        // Clear non-prestige upgrades
        upgradeLevels.entrySet().removeIf(e -> {
            UpgradeDef def = findUpgrade(e.getKey());
            return def != null && !def.currency.equals("gf");
        });
        // Recalculate totalUpgradesBought from remaining prestige upgrades
        totalUpgradesBought = upgradeLevels.values().stream().mapToInt(Integer::intValue).sum();

        // Clear non-prestige achievements
        unlockedAchievements.clear();

        if (engine == null) {
            engine = new ClickerEngine();
        }

        int initMethod = getInitMethod();
        engine.init(sourceImage, palette, gridW, gridH, triWidth, triHeight, scale, initMethod);
    }

    private int getInitMethod() {
        if (getLevel("lap_init") > 0) return ImageEvolver.INIT_LAP_OPTIMAL;
        if (getLevel("smart_init") > 0) return ImageEvolver.INIT_SMART;
        return ImageEvolver.INIT_RANDOM;
    }

    // ════════════════════════════════════════════════════════════════
    //  GAME TICK — called every ~1 second
    // ════════════════════════════════════════════════════════════════

    public List<String> tick() {
        List<String> notifications = new ArrayList<>();
        long now = System.currentTimeMillis();
        totalPlayTimeMs = now - gameStartMs;

        computeEpPerSecond();
        addEp(epPerSecond);

        // Auto-clickers
        if (engine != null && engine.isInitialized()) {
            double autoRate = computeAutoClickRate();
            if (autoRate > 0) {
                autoClickAccumulator += autoRate;
                int autoClicks = (int) autoClickAccumulator;
                autoClickAccumulator -= autoClicks;

                if (autoClicks > 0) {
                    int autoCycles = 1 + (int) getEffectValue("autoCycles") + (int) getEffectValue("eternalCycles");
                    int autoMulti = 1 + (int) getEffectValue("autoMulti");
                    int distance = computeSwapDistance();
                    double smartPct = 0;

                    for (int c = 0; c < autoClicks; c++) {
                        ClickerEngine.ClickResult result = engine.performClick(
                                autoMulti, autoCycles, distance, smartPct);
                        if (result.fitnessGain > 0) {
                            double epGain = computeEpForFitnessGain(result.fitnessGain) * 0.5;
                            addEp(epGain);
                        } else {
                            addEp(0.1);
                        }
                    }
                }
            }
        }

        // Events
        tickEvents(notifications);

        // Random event spawning (every 10 seconds, not 5)
        if (now - lastEventCheckMs > 10000) {
            lastEventCheckMs = now;
            double luckBonus = 1.0 + getEffectValue("luckyEvents");
            for (EventDef def : EVENTS) {
                if (Math.random() < def.baseChance * luckBonus) {
                    activeEvents.add(new ActiveEvent(def.id, def.name, def.icon,
                            def.durationSeconds, def.baseStrength, now));
                    notifications.add("EVENT:" + def.icon + " " + def.name + " \u2014 " + def.description);
                }
            }
        }

        checkAchievements(notifications);

        return notifications;
    }

    private double computeAutoClickRate() {
        double base = getEffectValue("autoClicker");
        if (base <= 0) return 0;
        double boost = 1.0 + getEffectValue("autoBoost");
        double eternal = 1.0 + getEffectValue("eternalAuto");
        double eventMult = getEventMultiplier("auto_frenzy");
        return base * boost * eternal * eventMult;
    }

    private int computeSwapDistance() {
        int localFocus = (int) getEffectValue("localFocus");
        if (localFocus <= 0) return 0;
        int n = (engine != null) ? engine.getTriangleCount() : 4000;
        return Math.max(10, n / (2 + localFocus));
    }

    // ════════════════════════════════════════════════════════════════
    //  CLICK — core mechanic
    // ════════════════════════════════════════════════════════════════

    public ClickResponse click() {
        if (engine == null || !engine.isInitialized()) {
            return new ClickResponse(0, 0, 0, 0, 0, false);
        }

        totalClicks++;

        // Compute parameters from upgrades
        int retryCycles = 1 + (int) getEffectValue("retryCycles") + (int) getEffectValue("eternalCycles");
        int swapsPerClick = 1 + (int) getEffectValue("multiSwap");
        int swapDistance = computeSwapDistance();
        double smartPct = getEffectValue("smartPick");

        // Critical swap check
        boolean critical = false;
        double critChance = getEffectValue("criticalSwap");
        critChance *= getEventMultiplier("lucky_streak");
        if (critChance > 0 && Math.random() < critChance) {
            critical = true;
            swapsPerClick *= 3;
            triggerDiscovery(4);
        }

        // Swap storm event
        double stormMult = getEventMultiplier("swap_storm");
        if (stormMult > 1) swapsPerClick = (int) (swapsPerClick * stormMult);

        // Precision wave event adds cycles
        double precisionMult = getEventMultiplier("precision_wave");
        if (precisionMult > 1) retryCycles += (int) precisionMult;

        // Perform the click
        ClickerEngine.ClickResult result = engine.performClick(
                swapsPerClick, retryCycles, swapDistance, smartPct);

        // Discovery: first successful swap
        if (result.successCount > 0 && engine.getSuccessfulSwaps() <= result.successCount) {
            triggerDiscovery(1);
        }

        // EP calculation — small base + bonus for fitness improvement
        double baseEp = 1.0 + getEffectValue("clickValue");
        double fitnessBonus = 0;
        if (result.fitnessGain > 0) {
            fitnessBonus = computeEpForFitnessGain(result.fitnessGain);
        }
        double earned = (baseEp + fitnessBonus) * getEpMultiplier() * getPrestigeMultiplier()
                * getEventMultiplier("golden_hour");
        if (critical) earned *= 2;
        addEp(earned);

        // MC generation (rare)
        double mcChance = getEffectValue("mcFinder");
        double mcEarned = 0;
        if (mcChance > 0 && Math.random() < mcChance) {
            mcEarned = 1;
            triggerDiscovery(5);
        }
        if (isEventActive("crystal_rain")) {
            mcEarned += 0.2;
        }
        mc += mcEarned;

        return new ClickResponse(earned, result.fitnessGain, result.newFitness,
                result.successCount, result.attemptCount, critical);
    }

    private double computeEpForFitnessGain(double gain) {
        return gain * 50000;
    }

    // ════════════════════════════════════════════════════════════════
    //  UPGRADES
    // ════════════════════════════════════════════════════════════════

    public int getLevel(String upgradeId) {
        return upgradeLevels.getOrDefault(upgradeId, 0);
    }

    public double getCost(String upgradeId) {
        UpgradeDef def = findUpgrade(upgradeId);
        if (def == null) return Double.MAX_VALUE;
        return def.baseCost * Math.pow(def.costGrowth, getLevel(upgradeId));
    }

    public boolean canAfford(String upgradeId) {
        UpgradeDef def = findUpgrade(upgradeId);
        if (def == null) return false;
        int level = getLevel(upgradeId);
        if (def.maxLevel > 0 && level >= def.maxLevel) return false;
        return getCurrency(def.currency) >= getCost(upgradeId);
    }

    public boolean buyUpgrade(String upgradeId) {
        if (!canAfford(upgradeId)) return false;
        UpgradeDef def = findUpgrade(upgradeId);
        spendCurrency(def.currency, getCost(upgradeId));
        upgradeLevels.merge(upgradeId, 1, Integer::sum);
        totalUpgradesBought++;
        computeEpPerSecond();

        if (upgradeId.equals("auto_clicker") && getLevel("auto_clicker") == 1) {
            triggerDiscovery(2);
        }
        return true;
    }

    public int buyMax(String upgradeId) {
        int count = 0;
        while (canAfford(upgradeId)) {
            if (!buyUpgrade(upgradeId)) break;
            count++;
        }
        return count;
    }

    // ════════════════════════════════════════════════════════════════
    //  PRESTIGE / ASCENSION
    // ════════════════════════════════════════════════════════════════

    public double calcPrestigeReward() {
        return Math.floor(Math.sqrt(totalEpEarned / 5000.0));
    }

    public boolean ascend() {
        double reward = calcPrestigeReward();
        if (reward < 1) return false;

        gf += reward;
        ascensionCount++;

        // Reset non-prestige state
        ep = 0;
        totalEpEarned = 0;
        mc = 0;
        autoClickAccumulator = 0;
        activeEvents.clear();

        // Reset non-prestige upgrades
        upgradeLevels.entrySet().removeIf(e -> {
            UpgradeDef def = findUpgrade(e.getKey());
            return def != null && !def.currency.equals("gf");
        });
        totalUpgradesBought = upgradeLevels.values().stream().mapToInt(Integer::intValue).sum();
        computeEpPerSecond();

        // Reset engine with potentially better init
        if (engine != null && palette != null) {
            engine.reset(getInitMethod(), palette, gridW, gridH, triWidth, triHeight, scale);
        }

        triggerDiscovery(3);
        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  COMPUTED VALUES
    // ════════════════════════════════════════════════════════════════

    private void computeEpPerSecond() {
        double base = 0;
        for (UpgradeDef def : UPGRADES) {
            int level = getLevel(def.id);
            if (level > 0 && !def.currency.equals("gf")) {
                base += level * 0.05 * (1 + def.baseCost / 200.0);
            }
        }
        base *= getEpMultiplier() * getPrestigeMultiplier();
        base *= getEventMultiplier("golden_hour");
        epPerSecond = base;
    }

    public double getEffectValue(String effectId) {
        double total = 0;
        for (UpgradeDef def : UPGRADES) {
            if (def.effectTarget.equals(effectId)) {
                total += getLevel(def.id) * def.effectPerLevel;
            }
        }
        return total;
    }

    private double getEpMultiplier() {
        double total = 1.0;
        for (UpgradeDef def : UPGRADES) {
            if (def.effectTarget.equals("epMultiplier") && def.effectType.equals("mult")) {
                total += getLevel(def.id) * def.effectPerLevel;
            }
        }
        for (AchievementDef ad : ACHIEVEMENTS) {
            if (unlockedAchievements.contains(ad.id)) {
                total *= ad.multiplier;
            }
        }
        return total;
    }

    public double getPrestigeMultiplier() {
        return 1.0 + gf * 0.005;
    }

    private double getEventMultiplier(String eventId) {
        double mult = 1.0;
        long now = System.currentTimeMillis();
        for (ActiveEvent ev : activeEvents) {
            if (ev.eventId.equals(eventId) && (now - ev.startMs) < ev.durationSeconds * 1000L) {
                mult *= ev.strength;
            }
        }
        return mult;
    }

    private boolean isEventActive(String eventId) {
        return getEventMultiplier(eventId) > 1.0;
    }

    // ════════════════════════════════════════════════════════════════
    //  EVENTS
    // ════════════════════════════════════════════════════════════════

    private void tickEvents(List<String> notifications) {
        long now = System.currentTimeMillis();
        activeEvents.removeIf(ev -> {
            boolean expired = (now - ev.startMs) > ev.durationSeconds * 1000L;
            if (expired) notifications.add("EVENT_END:" + ev.name + " has ended");
            return expired;
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  ACHIEVEMENTS — rate-limited popup queue
    // ════════════════════════════════════════════════════════════════

    private void checkAchievements(List<String> notifications) {
        double fitness = (engine != null && engine.isInitialized()) ? engine.getFitness() : 0;
        long successSwaps = (engine != null) ? engine.getSuccessfulSwaps() : 0;
        int newUnlocks = 0;

        for (AchievementDef ad : ACHIEVEMENTS) {
            if (unlockedAchievements.contains(ad.id)) continue;

            boolean unlocked = switch (ad.conditionType) {
                case "fitness" -> fitness * 100 >= ad.threshold;
                case "successSwaps" -> successSwaps >= ad.threshold;
                case "totalEp" -> totalEpEarned >= ad.threshold;
                case "clicks" -> totalClicks >= ad.threshold;
                case "playTime" -> (totalPlayTimeMs / 1000) >= ad.threshold;
                case "upgrades" -> totalUpgradesBought >= ad.threshold;
                case "ascensions" -> ascensionCount >= ad.threshold;
                default -> false;
            };

            if (unlocked) {
                unlockedAchievements.add(ad.id);
                totalAchievementsUnlocked++;
                addEp(ad.epReward);
                newUnlocks++;
                // Rate limit: only queue a limited number of popups per tick
                if (newUnlocks <= MAX_POPUPS_PER_TICK) {
                    achievementQueue.add(ad.id);
                }
                notifications.add("ACHIEVEMENT:" + ad.icon + " " + ad.name);
            }
        }
    }

    public void triggerDiscovery(int discoveryId) {
        for (AchievementDef ad : ACHIEVEMENTS) {
            if (ad.conditionType.equals("discovery") && ad.threshold == discoveryId) {
                if (!unlockedAchievements.contains(ad.id)) {
                    unlockedAchievements.add(ad.id);
                    achievementQueue.add(ad.id);
                    totalAchievementsUnlocked++;
                    addEp(ad.epReward);
                }
            }
        }
    }

    public List<String> drainAchievementQueue() {
        List<String> result = new ArrayList<>(achievementQueue);
        achievementQueue.clear();
        return result;
    }

    // ════════════════════════════════════════════════════════════════
    //  CURRENCY HELPERS
    // ════════════════════════════════════════════════════════════════

    private void addEp(double amount) {
        ep += amount;
        totalEpEarned += amount;
    }

    private double getCurrency(String type) {
        return switch (type) {
            case "ep" -> ep;
            case "mc" -> mc;
            case "gf" -> gf;
            default -> 0;
        };
    }

    private void spendCurrency(String type, double amount) {
        switch (type) {
            case "ep" -> ep -= amount;
            case "mc" -> mc -= amount;
            case "gf" -> gf -= amount;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  JSON SERIALIZATION
    // ════════════════════════════════════════════════════════════════

    public String toJson() {
        StringBuilder sb = new StringBuilder(16384);
        sb.append("{\n");
        sb.append("  \"ep\":").append(ep).append(",\n");
        sb.append("  \"totalEpEarned\":").append(totalEpEarned).append(",\n");
        sb.append("  \"mc\":").append(mc).append(",\n");
        sb.append("  \"gf\":").append(gf).append(",\n");
        sb.append("  \"totalClicks\":").append(totalClicks).append(",\n");
        sb.append("  \"totalUpgrades\":").append(totalUpgradesBought).append(",\n");
        sb.append("  \"playTimeMs\":").append(totalPlayTimeMs).append(",\n");
        sb.append("  \"ascensionCount\":").append(ascensionCount).append(",\n");
        sb.append("  \"achievementsUnlocked\":").append(totalAchievementsUnlocked).append(",\n");
        sb.append("  \"achievementsTotal\":").append(ACHIEVEMENTS.length).append(",\n");
        sb.append("  \"epPerSecond\":").append(epPerSecond).append(",\n");
        sb.append("  \"prestigeReward\":").append(calcPrestigeReward()).append(",\n");
        sb.append("  \"prestigeMultiplier\":").append(getPrestigeMultiplier()).append(",\n");

        boolean engineReady = engine != null && engine.isInitialized();
        sb.append("  \"initialized\":").append(engineReady).append(",\n");
        sb.append("  \"fitness\":").append(engineReady ? engine.getFitness() : 0).append(",\n");
        sb.append("  \"totalSwaps\":").append(engineReady ? engine.getTotalSwaps() : 0).append(",\n");
        sb.append("  \"successSwaps\":").append(engineReady ? engine.getSuccessfulSwaps() : 0).append(",\n");
        sb.append("  \"triangleCount\":").append(engineReady ? engine.getTriangleCount() : 0).append(",\n");

        // Upgrades
        sb.append("  \"upgrades\":[\n");
        for (int i = 0; i < UPGRADES.length; i++) {
            UpgradeDef u = UPGRADES[i];
            int level = getLevel(u.id);
            double cost = getCost(u.id);
            boolean affordable = canAfford(u.id);
            boolean visible = isUpgradeVisible(u);
            sb.append("    {\"id\":\"").append(u.id).append("\",");
            sb.append("\"name\":\"").append(jsonEsc(u.name)).append("\",");
            sb.append("\"cat\":\"").append(jsonEsc(u.category)).append("\",");
            sb.append("\"desc\":\"").append(jsonEsc(u.description)).append("\",");
            sb.append("\"level\":").append(level).append(",");
            sb.append("\"max\":").append(u.maxLevel).append(",");
            sb.append("\"cost\":").append(cost).append(",");
            sb.append("\"currency\":\"").append(u.currency).append("\",");
            sb.append("\"affordable\":").append(affordable).append(",");
            sb.append("\"visible\":").append(visible).append("}");
            if (i < UPGRADES.length - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // Achievements
        sb.append("  \"achievements\":[\n");
        for (int i = 0; i < ACHIEVEMENTS.length; i++) {
            AchievementDef a = ACHIEVEMENTS[i];
            boolean unlocked = unlockedAchievements.contains(a.id);
            boolean visible = unlocked || !a.hidden;
            sb.append("    {\"id\":\"").append(a.id).append("\",");
            sb.append("\"name\":\"").append(jsonEsc(a.name)).append("\",");
            sb.append("\"icon\":\"").append(jsonEsc(a.icon)).append("\",");
            sb.append("\"desc\":\"").append(jsonEsc(a.description)).append("\",");
            sb.append("\"unlocked\":").append(unlocked).append(",");
            sb.append("\"visible\":").append(visible).append("}");
            if (i < ACHIEVEMENTS.length - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // Events
        sb.append("  \"events\":[\n");
        long now = System.currentTimeMillis();
        for (int i = 0; i < activeEvents.size(); i++) {
            ActiveEvent ev = activeEvents.get(i);
            long remainMs = (ev.durationSeconds * 1000L) - (now - ev.startMs);
            sb.append("    {\"id\":\"").append(ev.eventId).append("\",");
            sb.append("\"name\":\"").append(jsonEsc(ev.name)).append("\",");
            sb.append("\"icon\":\"").append(jsonEsc(ev.icon)).append("\",");
            sb.append("\"remainSec\":").append(Math.max(0, remainMs / 1000)).append(",");
            sb.append("\"strength\":").append(ev.strength).append("}");
            if (i < activeEvents.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // Achievement popups (rate-limited)
        List<String> queue = drainAchievementQueue();
        sb.append("  \"achievementPopups\":[\n");
        for (int i = 0; i < queue.size(); i++) {
            AchievementDef ad = findAchievement(queue.get(i));
            if (ad != null) {
                sb.append("    {\"name\":\"").append(jsonEsc(ad.name)).append("\",");
                sb.append("\"icon\":\"").append(jsonEsc(ad.icon)).append("\",");
                sb.append("\"desc\":\"").append(jsonEsc(ad.description)).append("\"}");
                if (i < queue.size() - 1) sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n");

        sb.append("}");
        return sb.toString();
    }

    private boolean isUpgradeVisible(UpgradeDef u) {
        if (u.currency.equals("gf") && ascensionCount == 0) return false;
        if (u.currency.equals("mc") && mc <= 0 && totalEpEarned < 2000) return false;
        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  ACCESSORS
    // ════════════════════════════════════════════════════════════════

    public double getEp() { return ep; }
    public double getMc() { return mc; }
    public double getGf() { return gf; }
    public double getEpPerSecond() { return epPerSecond; }
    public long getTotalClicks() { return totalClicks; }
    public int getAscensionCount() { return ascensionCount; }
    public int getTotalAchievementsUnlocked() { return totalAchievementsUnlocked; }
    public List<ActiveEvent> getActiveEvents() { return Collections.unmodifiableList(activeEvents); }

    // ════════════════════════════════════════════════════════════════
    //  RECORDS
    // ════════════════════════════════════════════════════════════════

    public record UpgradeDef(String id, String name, String category, String description,
                             double baseCost, double costGrowth, int maxLevel,
                             String currency, String effectTarget, double effectPerLevel,
                             String effectType) {}

    public record AchievementDef(String id, String name, String icon, String description,
                                 String conditionType, double threshold,
                                 double epReward, double multiplier, boolean hidden) {}

    public record EventDef(String id, String name, String icon, String description,
                           int durationSeconds, double baseStrength, double baseChance) {}

    public record ActiveEvent(String eventId, String name, String icon,
                              int durationSeconds, double strength, long startMs) {}

    public record ClickResponse(double earned, double fitnessGain, double newFitness,
                                int successCount, int attemptCount, boolean critical) {}

    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════

    private static UpgradeDef findUpgrade(String id) {
        for (UpgradeDef u : UPGRADES) if (u.id.equals(id)) return u;
        return null;
    }

    private static AchievementDef findAchievement(String id) {
        for (AchievementDef a : ACHIEVEMENTS) if (a.id.equals(id)) return a;
        return null;
    }

    private static String jsonEsc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static String formatBigNumber(double n) {
        if (n >= 1e12) return String.format("%.1fT", n / 1e12);
        if (n >= 1e9) return String.format("%.1fB", n / 1e9);
        if (n >= 1e6) return String.format("%.1fM", n / 1e6);
        if (n >= 1e3) return String.format("%.1fK", n / 1e3);
        return String.format("%.0f", n);
    }

    private static String formatDuration(long seconds) {
        if (seconds >= 86400) return (seconds / 86400) + " day" + (seconds >= 172800 ? "s" : "");
        if (seconds >= 3600) return (seconds / 3600) + " hour" + (seconds >= 7200 ? "s" : "");
        if (seconds >= 60) return (seconds / 60) + " minute" + (seconds >= 120 ? "s" : "");
        return seconds + " second" + (seconds != 1 ? "s" : "");
    }
}
