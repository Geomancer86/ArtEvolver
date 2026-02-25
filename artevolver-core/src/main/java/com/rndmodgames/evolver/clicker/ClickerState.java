package com.rndmodgames.evolver.clicker;

import com.rndmodgames.evolver.Palette;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core game state for the "Evolution Clicker" mode.
 *
 * Each click performs real color swaps on triangles via ClickerEngine/DeltaFitnessEngine.
 * Upgrades increase swap power, unlock auto-clickers, and improve swap strategies.
 * Prestige resets the image arrangement for faster subsequent runs.
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
    private double epPerClick = 1;

    // Combo tracking
    private long lastClickMs = 0;
    private int comboCount = 0;
    private static final long COMBO_TIMEOUT_MS = 2000;

    // Auto-clicker accumulator (fractional clicks)
    private double autoClickAccumulator = 0;

    // Active events
    private final List<ActiveEvent> activeEvents = new ArrayList<>();
    private long lastEventCheckMs = 0;

    // Upgrade levels
    private final Map<String, Integer> upgradeLevels = new ConcurrentHashMap<>();

    // Unlocked achievements
    private final Set<String> unlockedAchievements = Collections.synchronizedSet(new LinkedHashSet<>());
    private final List<String> achievementQueue = Collections.synchronizedList(new ArrayList<>());

    // Engine reference (set externally via initEngine)
    private ClickerEngine engine;

    // Init params (kept for prestige reset)
    private Palette palette;
    private int gridW, gridH;
    private float triWidth, triHeight, scale;

    // ════════════════════════════════════════════════════════════════
    //  UPGRADE DEFINITIONS (32 upgrades, 5 categories)
    // ════════════════════════════════════════════════════════════════

    public static final UpgradeDef[] UPGRADES = {
        // ─── CLICK POWER (EP) ───
        new UpgradeDef("swap_power", "Swap Power", "Click Power",
                "Extra triangle swaps per click. More swaps = faster progress.",
                15, 1.15, 50, "ep", "swapPower", 1, "flat"),
        new UpgradeDef("search_depth", "Search Depth", "Click Power",
                "More attempts to find an improving swap. Higher = always finds one.",
                10, 1.12, 50, "ep", "searchDepth", 100, "flat"),
        new UpgradeDef("smart_targeting", "Smart Targeting", "Click Power",
                "Target the worst-matching triangle first for better swaps.",
                50, 1.18, 20, "ep", "smartTargeting", 0.05, "flat"),
        new UpgradeDef("local_search", "Local Search", "Click Power",
                "Prefer swapping nearby triangles for coherent improvements.",
                40, 1.16, 20, "ep", "localSearch", 0.05, "flat"),
        new UpgradeDef("best_of_n", "Best of N", "Click Power",
                "Try N candidate swaps and pick the most improving one.",
                100, 1.22, 10, "ep", "bestOfN", 1, "flat"),
        new UpgradeDef("click_value", "Click Value", "Click Power",
                "Increases base EP earned per click.",
                8, 1.10, 100, "ep", "clickValue", 1, "flat"),
        new UpgradeDef("critical_click", "Critical Click", "Click Power",
                "Chance for a critical click that performs 5x swaps.",
                200, 1.20, 20, "ep", "criticalClick", 0.02, "flat"),
        new UpgradeDef("click_combo", "Click Combo", "Click Power",
                "Rapid clicks build a combo multiplier for more EP.",
                60, 1.14, 20, "ep", "clickCombo", 0.05, "flat"),

        // ─── AUTOMATION (EP) ───
        new UpgradeDef("auto_clicker", "Auto-Clicker", "Automation",
                "Automatic clicks per second. Evolution never sleeps.",
                100, 1.18, 20, "ep", "autoClicker", 0.5, "flat"),
        new UpgradeDef("auto_power", "Auto Power", "Automation",
                "Each auto-click performs additional swaps.",
                200, 1.20, 20, "ep", "autoPower", 1, "flat"),
        new UpgradeDef("auto_smart", "Auto Intelligence", "Automation",
                "Auto-clickers use smart targeting for better results.",
                500, 1.22, 10, "ep", "autoSmart", 0.05, "flat"),
        new UpgradeDef("turbo_burst", "Turbo Burst", "Automation",
                "Manual clicks boost auto-clicker speed 2x for 5 seconds.",
                300, 1.20, 10, "ep", "turboBurst", 0.05, "flat"),
        new UpgradeDef("parallel_auto", "Parallel Auto", "Automation",
                "Independent auto-clicker streams working simultaneously.",
                800, 1.30, 5, "ep", "parallelAuto", 1, "flat"),
        new UpgradeDef("auto_efficiency", "Auto Efficiency", "Automation",
                "EP earned from auto-clicks is increased.",
                150, 1.15, 20, "ep", "autoEfficiency", 0.10, "flat"),

        // ─── FITNESS ENGINEERING (EP) ───
        new UpgradeDef("grid_focus", "Grid Focus", "Fitness",
                "Focus swaps on the worst-performing grid region.",
                80, 1.16, 20, "ep", "gridFocus", 0.05, "flat"),
        new UpgradeDef("region_lock", "Region Lock", "Fitness",
                "Lock the best regions from being disturbed by swaps.",
                400, 1.25, 5, "ep", "regionLock", 1, "flat"),
        new UpgradeDef("color_affinity", "Color Affinity", "Fitness",
                "Prefer swapping colors that are very different for bigger gains.",
                120, 1.18, 10, "ep", "colorAffinity", 0.05, "flat"),
        new UpgradeDef("init_quality", "Init Quality", "Fitness",
                "Better starting color arrangement: Random -> Smart -> LAP.",
                500, 1.50, 3, "ep", "initQuality", 1, "flat"),
        new UpgradeDef("fitness_momentum", "Fitness Momentum", "Fitness",
                "Consecutive improvements build momentum for better swaps.",
                250, 1.20, 10, "ep", "fitnessMomentum", 0.03, "flat"),
        new UpgradeDef("precision_mode", "Precision Mode", "Fitness",
                "Reduce search space to high-error triangles for better average gain.",
                180, 1.18, 10, "ep", "precisionMode", 0.05, "flat"),

        // ─── SPECIAL (MC currency) ───
        new UpgradeDef("golden_swap", "Golden Swap", "Special",
                "Chance for a golden swap with 10x fitness improvement.",
                5, 1.40, 20, "mc", "goldenSwap", 0.005, "flat"),
        new UpgradeDef("mc_finder", "Crystal Finder", "Special",
                "Chance to find Mutation Crystals with each click.",
                3, 1.35, 30, "mc", "mcFinder", 0.003, "flat"),
        new UpgradeDef("time_warp", "Time Warp", "Special",
                "Increases auto-clicker speed by 5% per level.",
                10, 1.35, 20, "mc", "timeWarp", 0.05, "flat"),
        new UpgradeDef("fitness_magnet", "Fitness Magnet", "Special",
                "Swaps are drawn toward the optimal arrangement.",
                15, 1.45, 20, "mc", "fitnessMagnet", 0.01, "flat"),
        new UpgradeDef("lucky_events", "Lucky Star", "Special",
                "Increases random event spawn rate.",
                8, 1.30, 20, "mc", "luckyEvents", 0.05, "flat"),
        new UpgradeDef("ep_multiplier", "EP Overflow", "Special",
                "Multiplies ALL Evolution Point gains.",
                20, 1.50, 50, "mc", "epMultiplier", 0.10, "mult"),
        new UpgradeDef("swap_frenzy", "Swap Frenzy", "Special",
                "Chance for double swaps on each click.",
                12, 1.40, 20, "mc", "swapFrenzy", 0.02, "flat"),
        new UpgradeDef("crystal_touch", "Crystal Touch", "Special",
                "Each click generates a small amount of Mutation Crystals.",
                25, 1.55, 20, "mc", "crystalTouch", 0.1, "flat"),

        // ─── PRESTIGE (GF currency) ───
        new UpgradeDef("eternal_power", "Eternal Power", "Prestige",
                "Permanent swap power that persists through ascensions.",
                5, 1.60, 50, "gf", "eternalPower", 1, "flat"),
        new UpgradeDef("eternal_speed", "Eternal Speed", "Prestige",
                "Permanent auto-clicker speed bonus.",
                8, 1.55, 50, "gf", "eternalSpeed", 0.05, "mult"),
        new UpgradeDef("eternal_start", "Eternal Genesis", "Prestige",
                "Permanently better starting arrangements after ascension.",
                10, 1.65, 30, "gf", "eternalStart", 0.03, "mult"),
        new UpgradeDef("eternal_luck", "Eternal Fortune", "Prestige",
                "Permanent event luck bonus.",
                15, 1.70, 20, "gf", "eternalLuck", 0.05, "mult"),
    };

    // ════════════════════════════════════════════════════════════════
    //  ACHIEVEMENT DEFINITIONS
    // ════════════════════════════════════════════════════════════════

    public static final AchievementDef[] ACHIEVEMENTS = generateAchievements();

    private static AchievementDef[] generateAchievements() {
        List<AchievementDef> list = new ArrayList<>();

        double[] fitTiers = {1, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 99};
        String[] fitNames = {"First Light", "Seeing Colors", "Faint Outline", "Sketchy",
                "Taking Shape", "Quarter Way", "Recognizable", "Getting Clearer",
                "Almost Half", "Halfway There", "Halfway Home", "Past the Peak",
                "Coming Together", "Fine Detail", "Masterwork", "Three Quarters",
                "Museum Quality", "Perfection's Shadow", "Near Perfect", "Pixel Master", "Transcendent"};
        String[] fitIcons = {"\uD83C\uDF31", "\uD83C\uDF08", "\u270F\uFE0F", "\u2702\uFE0F",
                "\uD83D\uDD8C\uFE0F", "\uD83D\uDCCA", "\uD83D\uDC41\uFE0F", "\uD83D\uDCAA",
                "\uD83C\uDFA8", "\u2B50", "\uD83C\uDFC6", "\uD83D\uDD25",
                "\uD83C\uDFAF", "\uD83D\uDD2C", "\uD83D\uDC8E", "\uD83C\uDFDB\uFE0F",
                "\uD83D\uDC51", "\u2728", "\uD83E\uDDEC", "\u269B\uFE0F", "\uD83C\uDF1F"};
        for (int i = 0; i < fitTiers.length; i++) {
            double reward = 10 * Math.pow(2, i);
            list.add(new AchievementDef("fit_" + i, fitNames[i], fitIcons[i],
                    "Reach " + fitTiers[i] + "% fitness", "fitness", fitTiers[i], reward, 1 + i * 0.01, false));
        }

        long[] swapTiers = {10, 100, 500, 1000, 5000, 10000, 50000, 100000, 500000, 1000000};
        String[] swapNames = {"First Swaps", "Getting Started", "Color Mixer", "Swap Centurion",
                "Five Thousand", "Swap Master", "Fifty Thousand", "Hundred K Swaps",
                "Half Million", "Swap Millionaire"};
        for (int i = 0; i < swapTiers.length; i++) {
            list.add(new AchievementDef("swap_" + i, swapNames[i], "\uD83D\uDD00",
                    "Perform " + formatBigNumber(swapTiers[i]) + " total swaps", "swaps", swapTiers[i],
                    swapTiers[i] * 0.5, 1 + i * 0.005, false));
        }

        long[] clickTiers = {1, 10, 50, 100, 500, 1000, 5000, 10000, 50000, 100000, 500000, 1000000};
        String[] clickNames = {"Curious", "Interested", "Engaged", "Dedicated",
                "Obsessed", "Thousand Clicks", "Click Master", "Ten Thousand Strong",
                "Carpal Tunnel", "Hundred Thousand", "Half Million Clicks", "The Millionth Click"};
        for (int i = 0; i < clickTiers.length; i++) {
            list.add(new AchievementDef("click_" + i, clickNames[i], "\uD83D\uDC46",
                    "Click " + formatBigNumber(clickTiers[i]) + " times", "clicks", clickTiers[i],
                    clickTiers[i] * 0.5, 1 + i * 0.003, false));
        }

        double[] epTiers = {100, 1000, 10000, 100000, 1e6, 1e7, 1e8, 1e9, 1e10, 1e12};
        String[] epNames = {"Pocket Change", "First Thousand", "Ten Grand", "Fortune",
                "Millionaire", "Tycoon", "Magnate", "Billionaire", "Trillionaire", "Universal Riches"};
        for (int i = 0; i < epTiers.length; i++) {
            list.add(new AchievementDef("ep_" + i, epNames[i], "\uD83D\uDCB0",
                    "Earn " + formatBigNumber(epTiers[i]) + " total EP", "totalEp", epTiers[i],
                    epTiers[i] * 0.1, 1 + i * 0.005, false));
        }

        long[] timeTiers = {60, 300, 900, 1800, 3600, 10800, 43200, 86400};
        String[] timeNames = {"One Minute", "Five Minutes", "Quarter Hour", "Patient Soul",
                "Hour of Power", "Three Hours", "Half Day", "Full Day"};
        for (int i = 0; i < timeTiers.length; i++) {
            list.add(new AchievementDef("time_" + i, timeNames[i], "\u23F0",
                    "Play for " + formatDuration(timeTiers[i]), "playTime", timeTiers[i],
                    timeTiers[i], 1 + i * 0.005, false));
        }

        int[] upgTiers = {1, 5, 10, 25, 50, 100, 200};
        String[] upgNames = {"First Purchase", "Getting Started", "Investor", "Tinkerer",
                "Engineer", "Hundred Upgrades", "Mad Scientist"};
        for (int i = 0; i < upgTiers.length; i++) {
            list.add(new AchievementDef("upg_" + i, upgNames[i], "\u2B06\uFE0F",
                    "Buy " + upgTiers[i] + " total upgrades", "upgrades", upgTiers[i],
                    upgTiers[i] * 10, 1 + i * 0.01, false));
        }

        int[] presTiers = {1, 3, 5, 10, 25, 50, 100};
        String[] presNames = {"First Ascension", "Triple Ascended", "Quintuple",
                "Decade of Ascensions", "Silver Ascension", "Golden Ascension", "Centennial"};
        for (int i = 0; i < presTiers.length; i++) {
            list.add(new AchievementDef("pres_" + i, presNames[i], "\uD83C\uDF1F",
                    "Ascend " + presTiers[i] + " times", "ascensions", presTiers[i],
                    presTiers[i] * 100, 1 + i * 0.02, false));
        }

        int[] autoTiers = {1, 5, 10};
        String[] autoNames = {"First Auto-Click", "Auto Army", "Auto Singularity"};
        for (int i = 0; i < autoTiers.length; i++) {
            list.add(new AchievementDef("auto_" + i, autoNames[i], "\u2699\uFE0F",
                    "Reach " + autoTiers[i] + " auto-clicks per second", "autoRate", autoTiers[i],
                    autoTiers[i] * 200, 1 + (i + 1) * 0.02, false));
        }

        // Discovery achievements
        list.add(new AchievementDef("disc_first_swap", "First Improvement", "\uD83D\uDD2C",
                "Your first improving color swap!", "discovery", 1, 50, 1.05, true));
        list.add(new AchievementDef("disc_combo5", "Combo Master", "\uD83D\uDD25",
                "Reach a 5x click combo", "discovery", 2, 200, 1.05, true));
        list.add(new AchievementDef("disc_critical", "Critical Strike!", "\u26A1",
                "Land your first critical click", "discovery", 3, 300, 1.05, true));
        list.add(new AchievementDef("disc_prestige", "Ascended", "\uD83C\uDF1F",
                "Perform your first ascension", "discovery", 4, 5000, 1.15, true));
        list.add(new AchievementDef("disc_golden", "Golden Touch", "\u2B50",
                "Trigger a Golden Hour event", "discovery", 5, 2000, 1.10, true));
        list.add(new AchievementDef("disc_mc_drop", "Crystal Drop", "\uD83D\uDC8E",
                "Find your first Mutation Crystal from clicking", "discovery", 6, 500, 1.08, true));
        list.add(new AchievementDef("disc_auto", "Hands Free", "\uD83E\uDD16",
                "Purchase your first auto-clicker", "discovery", 7, 300, 1.05, true));
        list.add(new AchievementDef("disc_allcats", "Renaissance", "\uD83C\uDF10",
                "Buy at least one upgrade from every category", "discovery", 8, 10000, 1.15, true));
        list.add(new AchievementDef("disc_combo10", "Combo King", "\uD83D\uDCA5",
                "Reach a 10x click combo", "discovery", 9, 1000, 1.10, true));
        list.add(new AchievementDef("disc_99", "The Last Percent", "\uD83C\uDFAF",
                "The final 1% is harder than the first 99%", "discovery", 10, 50000, 1.25, true));

        return list.toArray(new AchievementDef[0]);
    }

    // ════════════════════════════════════════════════════════════════
    //  EVENT DEFINITIONS (rethemed for clicker)
    // ════════════════════════════════════════════════════════════════

    public static final EventDef[] EVENTS = {
        new EventDef("swap_storm", "Swap Storm", "\uD83C\uDF2A\uFE0F",
                "3x swaps per click!", 30, 3.0, 0.003),
        new EventDef("golden_hour", "Golden Hour", "\u2B50",
                "5x EP from all sources!", 30, 5.0, 0.002),
        new EventDef("crystal_rain", "Crystal Rain", "\uD83D\uDC8E",
                "MC drops with every click!", 45, 1.0, 0.002),
        new EventDef("auto_frenzy", "Auto Frenzy", "\u26A1",
                "3x auto-clicker speed!", 30, 3.0, 0.002),
        new EventDef("precision_wave", "Precision Wave", "\uD83C\uDFAF",
                "All swap attempts find improvements!", 60, 1.0, 0.001),
        new EventDef("combo_master", "Combo Freeze", "\u2744\uFE0F",
                "Combo never expires!", 45, 1.0, 0.002),
        new EventDef("time_dilation", "Time Dilation", "\u23F3",
                "Everything boosted 2x!", 120, 2.0, 0.001),
        new EventDef("lucky_streak", "Lucky Streak", "\uD83C\uDF40",
                "3x critical click chance!", 30, 3.0, 0.003),
    };

    // ════════════════════════════════════════════════════════════════
    //  ENGINE INITIALIZATION
    // ════════════════════════════════════════════════════════════════

    public ClickerEngine getEngine() { return engine; }

    /**
     * Initializes the clicker engine with the given source image and parameters.
     * Called when the clicker page is first opened.
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

        if (engine == null) {
            engine = new ClickerEngine();
        }

        int initMethod = getInitMethod();
        engine.init(sourceImage, palette, gridW, gridH, triWidth, triHeight, scale, initMethod);
    }

    private int getInitMethod() {
        int qualityLevel = (int) getEffectValue("initQuality");
        double eternalStart = getEffectValue("eternalStart");
        qualityLevel += (int) eternalStart;
        return Math.min(qualityLevel, 2); // 0=Random, 1=Smart, 2=LAP
    }

    // ════════════════════════════════════════════════════════════════
    //  GAME TICK (called every ~1 second from state endpoint)
    // ════════════════════════════════════════════════════════════════

    public List<String> tick() {
        List<String> notifications = new ArrayList<>();
        long now = System.currentTimeMillis();
        totalPlayTimeMs = now - gameStartMs;

        // EP per second from passive + upgrades
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
                    int autoPower = 1 + (int) getEffectValue("autoPower") + (int) getEffectValue("eternalPower");
                    int maxAttempts = 100 + (int) getEffectValue("searchDepth");
                    double autoSmartPct = getEffectValue("autoSmart");

                    double autoEpMult = 1.0 + getEffectValue("autoEfficiency");
                    int streams = 1 + (int) getEffectValue("parallelAuto");

                    for (int stream = 0; stream < streams; stream++) {
                        for (int c = 0; c < autoClicks; c++) {
                            ClickerEngine.ClickResult result = engine.performClick(
                                    autoPower, maxAttempts, autoSmartPct, 0, 1);
                            if (result.fitnessGain > 0) {
                                double epGain = Math.max(1, result.fitnessGain * 10000)
                                        * getEpMultiplier() * autoEpMult * getPrestigeMultiplier();
                                epGain *= getEventMultiplier("golden_hour");
                                epGain *= getEventMultiplier("time_dilation");
                                addEp(epGain);
                            }
                        }
                    }
                }
            }
        }

        // Event tick
        tickEvents(notifications);

        // Random event spawning
        if (now - lastEventCheckMs > 5000) {
            lastEventCheckMs = now;
            double luckBonus = 1.0 + getEffectValue("luckyEvents") + getEffectValue("eternalLuck");
            for (EventDef def : EVENTS) {
                if (Math.random() < def.baseChance * luckBonus) {
                    double strength = def.baseStrength;
                    activeEvents.add(new ActiveEvent(def.id, def.name, def.icon,
                            def.durationSeconds, strength, now));
                    notifications.add("EVENT:" + def.icon + " " + def.name + " \u2014 " + def.description);
                }
            }
        }

        // Check achievements
        checkAchievements(notifications);

        return notifications;
    }

    private double computeAutoClickRate() {
        double base = getEffectValue("autoClicker");
        if (base <= 0) return 0;
        double timeWarp = 1.0 + getEffectValue("timeWarp");
        double eternalSpeed = 1.0 + getEffectValue("eternalSpeed");
        double turboMult = isTurboBurstActive() ? 2.0 : 1.0;
        double eventMult = getEventMultiplier("auto_frenzy") * getEventMultiplier("time_dilation");
        return base * timeWarp * eternalSpeed * turboMult * eventMult;
    }

    private long turboBurstUntilMs = 0;
    private boolean isTurboBurstActive() {
        return System.currentTimeMillis() < turboBurstUntilMs;
    }

    // ════════════════════════════════════════════════════════════════
    //  CLICK (called from /api/clicker/click)
    // ════════════════════════════════════════════════════════════════

    /**
     * Perform a manual click. Returns a ClickResponse with EP earned and details.
     */
    public ClickResponse click() {
        if (engine == null || !engine.isInitialized()) {
            return new ClickResponse(0, 0, 0, 0, false);
        }

        totalClicks++;
        long now = System.currentTimeMillis();

        // Combo system
        if (now - lastClickMs < COMBO_TIMEOUT_MS) {
            comboCount++;
        } else {
            comboCount = 1;
        }
        if (isEventActive("combo_master")) {
            comboCount = Math.max(comboCount, 5);
        }
        lastClickMs = now;

        // Discovery: combo milestones
        if (comboCount >= 5) triggerDiscovery(2);
        if (comboCount >= 10) triggerDiscovery(9);

        // Turbo burst activation
        double turboLevel = getEffectValue("turboBurst");
        if (turboLevel > 0) {
            turboBurstUntilMs = now + 5000;
        }

        // Compute swap parameters from upgrades
        int swapsPerClick = 1 + (int) getEffectValue("swapPower") + (int) getEffectValue("eternalPower");
        int maxAttempts = 100 + (int) getEffectValue("searchDepth");
        double smartPct = getEffectValue("smartTargeting");
        double localPct = getEffectValue("localSearch");
        int bestOfN = 1 + (int) getEffectValue("bestOfN");

        // Critical click
        boolean critical = false;
        double critChance = getEffectValue("criticalClick");
        critChance *= getEventMultiplier("lucky_streak");
        if (critChance > 0 && Math.random() < critChance) {
            critical = true;
            swapsPerClick *= 5;
            triggerDiscovery(3);
        }

        // Swap storm event
        double stormMult = getEventMultiplier("swap_storm");
        swapsPerClick = (int) (swapsPerClick * stormMult);

        // Swap frenzy chance
        double frenzyChance = getEffectValue("swapFrenzy");
        if (frenzyChance > 0 && Math.random() < frenzyChance) {
            swapsPerClick *= 2;
        }

        // Precision wave = massive search depth
        if (isEventActive("precision_wave")) {
            maxAttempts *= 10;
        }

        // Perform the actual click via engine
        ClickerEngine.ClickResult result = engine.performClick(
                swapsPerClick, maxAttempts, smartPct, localPct, bestOfN);

        // Discovery: first swap
        if (result.swapsApplied > 0 && engine.getTotalSwaps() <= result.swapsApplied) {
            triggerDiscovery(1);
        }

        // EP calculation
        double comboMult = 1.0 + (comboCount - 1) * getEffectValue("clickCombo");
        double clickVal = 1.0 + getEffectValue("clickValue");
        double fitnessBonus = Math.max(1, result.fitnessGain * 10000);
        double earned = fitnessBonus * clickVal * getEpMultiplier() * comboMult
                * getPrestigeMultiplier()
                * getEventMultiplier("golden_hour")
                * getEventMultiplier("time_dilation");
        if (critical) earned *= 2;
        addEp(earned);

        // MC generation
        double mcChance = getEffectValue("mcFinder");
        double crystalFlat = getEffectValue("crystalTouch");
        double mcEarned = crystalFlat;
        if (mcChance > 0 && Math.random() < mcChance) {
            mcEarned += 1;
            triggerDiscovery(6);
        }
        if (isEventActive("crystal_rain")) {
            mcEarned += 0.5;
        }
        mc += mcEarned;

        return new ClickResponse(earned, result.fitnessGain, result.newFitness,
                result.swapsApplied, critical);
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
        int level = getLevel(upgradeId);
        return def.baseCost * Math.pow(def.costGrowth, level);
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
        double cost = getCost(upgradeId);
        spendCurrency(def.currency, cost);
        upgradeLevels.merge(upgradeId, 1, Integer::sum);
        totalUpgradesBought++;
        computeEpPerSecond();

        // Discovery: first auto-clicker
        if (upgradeId.equals("auto_clicker") && getLevel("auto_clicker") == 1) {
            triggerDiscovery(7);
        }
        // Discovery: all categories
        checkAllCategoriesPurchased();

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

    private void checkAllCategoriesPurchased() {
        Set<String> cats = new HashSet<>();
        for (UpgradeDef u : UPGRADES) cats.add(u.category);
        Set<String> bought = new HashSet<>();
        for (UpgradeDef u : UPGRADES) {
            if (getLevel(u.id) > 0) bought.add(u.category);
        }
        if (bought.containsAll(cats)) triggerDiscovery(8);
    }

    // ════════════════════════════════════════════════════════════════
    //  PRESTIGE / ASCENSION
    // ════════════════════════════════════════════════════════════════

    public double calcPrestigeReward() {
        double base = Math.sqrt(totalEpEarned / 1000.0);
        double multiplier = 1.0 + getEffectValue("eternalLuck") * 0.5;
        return Math.floor(base * multiplier);
    }

    public boolean ascend() {
        double reward = calcPrestigeReward();
        if (reward < 1) return false;

        gf += reward;
        ascensionCount++;
        ep = 0;
        mc = 0;
        totalEpEarned = 0;
        comboCount = 0;
        autoClickAccumulator = 0;

        // Reset non-prestige upgrade levels
        upgradeLevels.entrySet().removeIf(e -> {
            UpgradeDef def = findUpgrade(e.getKey());
            return def != null && !def.currency.equals("gf");
        });

        totalUpgradesBought = upgradeLevels.values().stream().mapToInt(Integer::intValue).sum();
        computeEpPerSecond();

        // Reset engine with potentially better init (from prestige upgrades)
        if (engine != null && palette != null) {
            int initMethod = getInitMethod();
            engine.reset(initMethod, palette, gridW, gridH, triWidth, triHeight, scale);
        }

        triggerDiscovery(4);
        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  COMPUTED VALUES
    // ════════════════════════════════════════════════════════════════

    private void computeEpPerSecond() {
        double base = 0.1;
        for (UpgradeDef def : UPGRADES) {
            int level = getLevel(def.id);
            if (level > 0 && !def.currency.equals("gf")) {
                base += level * 0.1 * (1 + def.baseCost / 100.0);
            }
        }
        base *= getEpMultiplier();
        base *= getPrestigeMultiplier();
        base *= getEventMultiplier("golden_hour");
        epPerSecond = base;

        epPerClick = 1.0 + getEffectValue("clickValue");
        epPerClick *= getEpMultiplier();
        epPerClick *= getPrestigeMultiplier();
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
        return 1.0 + gf * 0.01;
    }

    private double getEventMultiplier(String eventId) {
        double mult = 1.0;
        for (ActiveEvent ev : activeEvents) {
            if (ev.eventId.equals(eventId)) mult *= ev.strength;
        }
        return mult;
    }

    private boolean isEventActive(String eventId) {
        long now = System.currentTimeMillis();
        for (ActiveEvent ev : activeEvents) {
            if (ev.eventId.equals(eventId) && (now - ev.startMs) < ev.durationSeconds * 1000L) {
                return true;
            }
        }
        return false;
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
    //  ACHIEVEMENTS
    // ════════════════════════════════════════════════════════════════

    private void checkAchievements(List<String> notifications) {
        double fitness = (engine != null && engine.isInitialized()) ? engine.getFitness() : 0;
        long totalSwaps = (engine != null) ? engine.getTotalSwaps() : 0;
        double autoRate = computeAutoClickRate();

        for (AchievementDef ad : ACHIEVEMENTS) {
            if (unlockedAchievements.contains(ad.id)) continue;

            boolean unlocked = switch (ad.conditionType) {
                case "fitness" -> fitness * 100 >= ad.threshold;
                case "swaps" -> totalSwaps >= ad.threshold;
                case "totalEp" -> totalEpEarned >= ad.threshold;
                case "clicks" -> totalClicks >= ad.threshold;
                case "playTime" -> (totalPlayTimeMs / 1000) >= ad.threshold;
                case "upgrades" -> totalUpgradesBought >= ad.threshold;
                case "ascensions" -> ascensionCount >= ad.threshold;
                case "autoRate" -> autoRate >= ad.threshold;
                default -> false;
            };

            if (unlocked) {
                unlockedAchievements.add(ad.id);
                achievementQueue.add(ad.id);
                totalAchievementsUnlocked++;
                addEp(ad.epReward);
                notifications.add("ACHIEVEMENT:" + ad.icon + " " + ad.name + " \u2014 " + ad.description);
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
        sb.append("  \"epPerClick\":").append(epPerClick).append(",\n");
        sb.append("  \"prestigeReward\":").append(calcPrestigeReward()).append(",\n");
        sb.append("  \"prestigeMultiplier\":").append(getPrestigeMultiplier()).append(",\n");
        sb.append("  \"combo\":").append(comboCount).append(",\n");

        // Engine state
        boolean engineReady = engine != null && engine.isInitialized();
        sb.append("  \"initialized\":").append(engineReady).append(",\n");
        sb.append("  \"fitness\":").append(engineReady ? engine.getFitness() : 0).append(",\n");
        sb.append("  \"totalSwaps\":").append(engineReady ? engine.getTotalSwaps() : 0).append(",\n");
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
            sb.append("\"visible\":").append(visible).append(",");
            sb.append("\"effectType\":\"").append(u.effectType).append("\",");
            sb.append("\"effectTarget\":\"").append(u.effectTarget).append("\",");
            sb.append("\"effectPerLevel\":").append(u.effectPerLevel).append("}");
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
            sb.append("\"visible\":").append(visible).append(",");
            sb.append("\"reward\":").append(a.epReward).append(",");
            sb.append("\"mult\":").append(a.multiplier).append("}");
            if (i < ACHIEVEMENTS.length - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // Active events
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

        // Achievement popup queue
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
        if (u.currency.equals("mc") && mc <= 0 && totalEpEarned < 500) return false;
        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  ACCESSORS
    // ════════════════════════════════════════════════════════════════

    public double getEp() { return ep; }
    public double getMc() { return mc; }
    public double getGf() { return gf; }
    public double getEpPerSecond() { return epPerSecond; }
    public double getEpPerClick() { return epPerClick; }
    public long getTotalClicks() { return totalClicks; }
    public int getAscensionCount() { return ascensionCount; }
    public int getTotalAchievementsUnlocked() { return totalAchievementsUnlocked; }
    public int getComboCount() { return comboCount; }
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
                                int swapsApplied, boolean critical) {}

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
