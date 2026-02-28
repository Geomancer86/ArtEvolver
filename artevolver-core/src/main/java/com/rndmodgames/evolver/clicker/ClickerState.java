package com.rndmodgames.evolver.clicker;

import com.rndmodgames.evolver.ImageEvolver;
import com.rndmodgames.evolver.Palette;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Base64;

/**
 * Game state for the Evolution Clicker.
 *
 * Design philosophy:
 *   Miyamoto — the click must feel satisfying. Miss streaks build tension,
 *              hits after droughts feel EARNED.
 *   Meier    — every upgrade is a meaningful decision, not a mandatory purchase.
 *   Miyazaki — the grind is the game. Low starting fitness is fair difficulty.
 *   Wright   — emergence: the image evolving from chaos IS the reward.
 *   Yokoi    — one mechanic (swap), deeply explored through upgrades.
 *
 * Starting position:
 *   ALWAYS begins with INIT_RANDOM (shuffled palette) → unordered triangles,
 *   low starting fitness. Prestige upgrades (Smart Genesis, LAP Genesis) unlock
 *   better initialization methods as earned rewards. All progress (EP, achievements,
 *   milestones) is measured as FITNESS GAIN from the starting point — never absolute
 *   fitness. A player who starts at 65% with random init earns 0 EP and 0 achievements
 *   until they improve the image through their own clicks.
 *
 * Three-tier loop:
 *   Small:  Click → earn EP → buy upgrades → click better
 *   Medium: Ascend → earn GF → buy prestige → restart same image with better init
 *   Big:    Complete masterpiece → new canvas → permanent Canvas Mastery bonus
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
    private double autoClickAccumulator = 0;

    // Masterpiece system — the big loop (Will Wright: emergent long-term goal)
    private int completedImages = 0;
    private double bestCompletionFitness = 0;

    // Gallery — every image tells a story (Kojima: hidden narrative in progression)
    private final List<GalleryEntry> gallery = Collections.synchronizedList(new ArrayList<>());
    private final Set<Long> usedImageFingerprints = Collections.synchronizedSet(new HashSet<>());
    private long currentImageFingerprint = 0;

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

    public record GalleryEntry(long fingerprint, String thumbnailBase64,
            double startFitness, double finalFitness, double fitnessGain,
            long totalClicks, long successSwaps, long playTimeMs,
            int ascensionCount, long timestamp) {}

    public record SampleProgress(double bestFitness, double bestGain, int playCount, boolean completed,
                                int ascensionsCount, long totalPlayTimeMs, long totalClicks) {
        public SampleProgress(double bestFitness, double bestGain, int playCount, boolean completed) {
            this(bestFitness, bestGain, playCount, completed, 0, 0, 0);
        }
    }

    private long lifetimeClicks;
    private double lifetimeEpEarned;
    private int lifetimeLongestMissStreak = 0;
    private final Set<String> discoveredSamples = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Map<String, SampleProgress> sampleProgress = new java.util.concurrent.ConcurrentHashMap<>();
    private String currentSampleId;

    // ════════════════════════════════════════════════════════════════
    //  UPGRADE DEFINITIONS
    //
    //  Sid Meier: "A game is a series of interesting decisions."
    //  Each upgrade changes HOW you click, not just how much you earn.
    //  Categories create natural decision points: do I invest in
    //  efficiency (Click Power), throughput (Automation), or
    //  intelligence (smarter targeting)?
    // ════════════════════════════════════════════════════════════════

    public static final UpgradeDef[] UPGRADES = {
        // ─── CLICK POWER (EP) ───
        // The core progression: make each click more effective.
        new UpgradeDef("retry_cycles", "Retry Cycles", "Click Power",
                "Try N random pairs per click, keep the best improving swap. " +
                "More cycles = higher chance of finding a good swap each click.",
                25, 1.28, 25, "ep", "retryCycles", 1, "flat"),
        new UpgradeDef("multi_swap", "Multi-Swap", "Click Power",
                "Attempt additional swaps per click. Each swap is independent — " +
                "some may hit, some may miss. Pairs well with Retry Cycles.",
                120, 1.35, 15, "ep", "multiSwap", 1, "flat"),
        new UpgradeDef("swap_reach", "Swap Reach", "Click Power",
                "Extend the distance you can swap. Base range is nearby triangles only. " +
                "Wider reach enables global color reorganization.",
                40, 1.18, 25, "ep", "swapReach", 1, "flat"),
        new UpgradeDef("click_ep", "Click Reward", "Click Power",
                "Increases base EP earned per click, hit or miss.",
                10, 1.14, 50, "ep", "clickEp", 0.5, "flat"),

        // ─── AUTOMATION (EP, expensive) ───
        // The idle layer. Slow but steady. Manual clicks always superior.
        new UpgradeDef("auto_clicker", "Auto-Clicker", "Automation",
                "Performs automatic clicks. Auto-clicks earn 50% EP " +
                "and never trigger critical hits. Your hands are the real power.",
                500, 1.22, 20, "ep", "autoRate", 0.2, "flat"),
        new UpgradeDef("auto_cycles", "Auto Precision", "Automation",
                "Auto-clickers gain retry cycles. Makes idle evolution " +
                "smarter, not faster. Pairs with Retry Cycles.",
                1200, 1.30, 10, "ep", "autoCycles", 1, "flat"),
        new UpgradeDef("auto_multi", "Auto Volume", "Automation",
                "Auto-clickers attempt extra swaps per tick. " +
                "More volume compensates for lower auto EP rate.",
                2500, 1.35, 10, "ep", "autoMulti", 1, "flat"),

        // ─── INTELLIGENCE (EP, mid-tier) ───
        // Make your clicks smarter, not just more numerous.
        new UpgradeDef("smart_pick", "Smart Pick", "Intelligence",
                "Chance to target the worst-matching triangle instead of random. " +
                "Worst triangles have the most to gain from a swap.",
                200, 1.22, 20, "ep", "smartPct", 0.04, "flat"),
        new UpgradeDef("streak_bonus", "Hot Hand", "Intelligence",
                "Consecutive successful swaps increase EP bonus. " +
                "Hit streaks become more rewarding the longer they last.",
                300, 1.25, 15, "ep", "streakBonus", 0.10, "flat"),
        new UpgradeDef("patience", "Patience", "Intelligence",
                "After 5+ consecutive misses, your next success earns bonus EP. " +
                "Even failure has value — it builds up potential energy.",
                180, 1.22, 10, "ep", "patienceBonus", 0.5, "flat"),

        // ─── SPECIAL (MC — rare currency) ───
        // Powerful but scarce. MC is precious.
        new UpgradeDef("critical_swap", "Critical Swap", "Special",
                "Chance for a critical click: 3x swap attempts. " +
                "Feels like lightning striking.",
                5, 1.45, 15, "mc", "critChance", 0.02, "flat"),
        new UpgradeDef("mc_finder", "Crystal Finder", "Special",
                "Small chance to discover Mutation Crystals per click. " +
                "The only reliable MC source outside events.",
                3, 1.40, 20, "mc", "mcChance", 0.002, "flat"),
        new UpgradeDef("ep_multiplier", "EP Overflow", "Special",
                "Multiplies ALL Evolution Point gains from every source.",
                15, 1.55, 30, "mc", "epMult", 0.08, "mult"),
        new UpgradeDef("lucky_events", "Lucky Star", "Special",
                "Increases random event spawn rate. Events are " +
                "powerful but fleeting — make the most of them.",
                8, 1.35, 15, "mc", "eventLuck", 0.05, "flat"),
        new UpgradeDef("auto_boost", "Turbo Auto", "Special",
                "Auto-clicker speed +50% per level. Turns idle " +
                "trickle into a steady stream.",
                20, 1.50, 10, "mc", "autoBoost", 0.5, "flat"),
        new UpgradeDef("chain_lightning", "Chain Lightning", "Special",
                "Critical hits have 15% chance to chain — trigger another critical burst. " +
                "Lightning can strike twice.",
                25, 1.60, 10, "mc", "chainChance", 0.15, "flat"),
        new UpgradeDef("resonance", "Resonance", "Special",
                "MC finder chance scales with EP Overflow level. Synergy between " +
                "power and discovery.",
                18, 1.50, 15, "mc", "resonance", 1, "flat"),

        // ─── PRESTIGE (GF — after first ascension) ───
        // Permanent power that transcends resets.
        new UpgradeDef("eternal_cycles", "Eternal Cycles", "Prestige",
                "Permanent retry cycles that persist through ascensions. " +
                "Every new run starts stronger.",
                5, 1.65, 20, "gf", "permCycles", 1, "flat"),
        new UpgradeDef("eternal_auto", "Eternal Speed", "Prestige",
                "Permanent auto-clicker speed bonus. Accumulates " +
                "across multiple ascensions.",
                8, 1.60, 20, "gf", "permAuto", 0.1, "flat"),
        new UpgradeDef("smart_init", "Smart Genesis", "Prestige",
                "Start with Smart initialization after ascension. " +
                "Much better starting fitness — the image begins recognizable.",
                25, 2.00, 1, "gf", "smartInit", 1, "flat"),
        new UpgradeDef("lap_init", "LAP Genesis", "Prestige",
                "Start with LAP Optimal initialization — provably the best " +
                "possible starting arrangement. The ultimate prestige reward.",
                100, 2.00, 1, "gf", "lapInit", 1, "flat"),
        new UpgradeDef("canvas_mastery", "Canvas Mastery", "Prestige",
                "Permanent +10% EP per completed masterpiece. " +
                "Each finished image makes the next one easier.",
                15, 1.70, 20, "gf", "canvasBonus", 0.10, "flat"),
        new UpgradeDef("eternal_reach", "Eternal Reach", "Prestige",
                "Permanent swap reach bonus that persists through ascensions. " +
                "Your arm grows longer with each rebirth.",
                10, 1.55, 15, "gf", "permReach", 1, "flat"),
        new UpgradeDef("golden_touch", "Golden Touch", "Prestige",
                "+5% EP from all sources per level. Unlocks after your first masterpiece. " +
                "Master artists convert effort into exponential gains.",
                20, 1.55, 10, "gf", "epMult", 0.05, "mult"),
        new UpgradeDef("eternal_fortune", "Eternal Fortune", "Prestige",
                "+3% EP per ascension per level. Your past lives compound. " +
                "Each rebirth makes the next run richer.",
                12, 1.70, 15, "gf", "ascensionEpBonus", 0.03, "flat"),
        new UpgradeDef("genesis_boost", "Genesis Boost", "Prestige",
                "+5% EP from fitness gains per level. Smart and LAP Genesis " +
                "starts pay dividends on every improvement.",
                30, 1.65, 10, "gf", "fitnessEpBonus", 0.05, "flat"),

        // ─── TREE UNLOCKS (deeper branches) ───
        new UpgradeDef("deep_focus", "Deep Focus", "Click Power",
                "Additional retry cycles per click. Unlocks when Multi-Swap is strong. " +
                "More attempts = more chances to find the perfect swap.",
                500, 1.35, 5, "ep", "retryCycles", 1, "flat"),
        new UpgradeDef("idle_mastery", "Idle Mastery", "Automation",
                "+0.15 auto-click rate per level. Unlocks when automation is mature. " +
                "Your idle evolution becomes a force of nature.",
                5000, 1.40, 5, "ep", "autoRate", 0.15, "flat"),
    };

    // ════════════════════════════════════════════════════════════════
    //  ACHIEVEMENT DEFINITIONS
    //
    //  Hideo Kojima: hidden stories in the progression.
    //  Hironobu Sakaguchi: emotional milestones that mark your journey.
    //
    //  CRITICAL DESIGN RULE: Fitness achievements use FITNESS GAIN from
    //  the starting point, NOT absolute fitness. Images naturally start
    //  at 50-70% absolute fitness due to palette affinity. If milestones
    //  were absolute, they'd pre-trigger instantly giving ~50K EP before
    //  the player clicks once. Every achievement must be EARNED.
    // ════════════════════════════════════════════════════════════════

    public static final AchievementDef[] ACHIEVEMENTS = generateAchievements();

    private static AchievementDef[] generateAchievements() {
        List<AchievementDef> list = new ArrayList<>();

        // Fitness GAIN milestones — measured from starting fitness, not absolute.
        // Miyazaki: every fraction of a percent is EARNED through your clicks.
        // An image that starts at 65% must GAIN these thresholds through swaps.
        double[] fitTiers = {0.1, 0.5, 1, 2, 3, 5, 7, 10, 15, 20, 25, 30, 40, 50, 60};
        String[] fitNames = {
            "First Glimpse",        // the tiniest improvement — proof it works
            "Visible Change",       // squint and you can tell
            "One Percent",          // the first real milestone
            "Noticeable",           // anyone can see the difference now
            "Real Progress",        // commitment is paying off
            "Five Percent Climb",   // a real transformation in progress
            "Meaningful Gain",      // the image is distinctly better
            "Ten Percent Better",   // a major achievement
            "Major Improvement",    // the image is clearly evolving
            "Transformation",       // unrecognizable from where you started
            "Remarkable",           // approaching perfection
            "Metamorphosis",        // the chrysalis has opened
            "Reinvented",           // a completely new image
            "Reborn",              // transcending the original chaos
            "Transcendent"          // the theoretical limit of gain
        };
        String[] fitIcons = {"\uD83C\uDF31", "\u270F\uFE0F", "\uD83C\uDF08", "\uD83D\uDD8C\uFE0F",
                "\uD83D\uDCCA", "\uD83D\uDC41\uFE0F", "\uD83D\uDCAA", "\uD83C\uDFA8",
                "\uD83C\uDFC6", "\uD83D\uDD25", "\uD83D\uDC8E", "\uD83D\uDC51",
                "\u2728", "\u269B\uFE0F", "\uD83C\uDF1F"};
        double[] fitRewards = {25, 50, 100, 200, 400, 1000, 2000, 5000, 10000,
                25000, 50000, 100000, 250000, 500000, 1000000};
        for (int i = 0; i < fitTiers.length; i++) {
            String desc = fitTiers[i] < 1
                    ? "Gain " + fitTiers[i] + "% fitness from start"
                    : "Gain " + (int) fitTiers[i] + "% fitness from start";
            list.add(new AchievementDef("fit_" + i, fitNames[i], fitIcons[i],
                    desc, "fitnessGain", fitTiers[i],
                    fitRewards[i], 1 + i * 0.005, false));
        }

        // Click milestones — the persistence arc
        long[] clickTiers = {10, 50, 200, 500, 1000, 5000, 10000, 50000, 100000};
        String[] clickNames = {"First Clicks", "Getting Started", "Persistent", "Dedicated",
                "Thousand Clicks", "Click Veteran", "Ten Thousand", "Fifty Thousand", "Click Legend"};
        for (int i = 0; i < clickTiers.length; i++) {
            list.add(new AchievementDef("click_" + i, clickNames[i], "\uD83D\uDC46",
                    clickTiers[i] + " clicks", "clicks", clickTiers[i],
                    clickTiers[i] * 0.3, 1 + i * 0.003, false));
        }

        // Successful swap milestones
        long[] swapTiers = {1, 10, 50, 200, 1000, 5000, 20000, 100000};
        String[] swapNames = {"First Improvement", "Color Mixer", "Swap Novice", "Swap Apprentice",
                "Swap Master", "Five Thousand", "Twenty Thousand", "Swap Legend"};
        for (int i = 0; i < swapTiers.length; i++) {
            list.add(new AchievementDef("swap_" + i, swapNames[i], "\uD83D\uDD00",
                    swapTiers[i] + " successful swaps", "successSwaps", swapTiers[i],
                    swapTiers[i] * 0.5, 1 + i * 0.004, false));
        }

        // EP milestones
        double[] epTiers = {100, 1000, 10000, 100000, 1e6, 1e8};
        String[] epNames = {"Pocket Change", "First Thousand", "Getting Rich", "Fortune",
                "Millionaire", "Tycoon"};
        for (int i = 0; i < epTiers.length; i++) {
            list.add(new AchievementDef("ep_" + i, epNames[i], "\uD83D\uDCB0",
                    "Earn " + fmtBig(epTiers[i]) + " total EP", "totalEp", epTiers[i],
                    epTiers[i] * 0.05, 1 + i * 0.005, false));
        }

        // Time milestones
        long[] timeTiers = {300, 1800, 3600, 10800, 43200};
        String[] timeNames = {"Five Minutes", "Half Hour", "Hour of Power", "Three Hours", "Half Day"};
        for (int i = 0; i < timeTiers.length; i++) {
            list.add(new AchievementDef("time_" + i, timeNames[i], "\u23F0",
                    "Play for " + fmtDuration(timeTiers[i]), "playTime", timeTiers[i],
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

        // Streak milestones (Miyazaki: perseverance rewarded)
        int[] streakTiers = {5, 10, 25, 50};
        String[] streakNames = {"Hot Streak", "On Fire", "Untouchable", "Perfection"};
        for (int i = 0; i < streakTiers.length; i++) {
            list.add(new AchievementDef("streak_" + i, streakNames[i], "\uD83D\uDD25",
                    streakTiers[i] + " consecutive successful swaps", "hitStreak", streakTiers[i],
                    streakTiers[i] * 20, 1 + (i + 1) * 0.01, false));
        }

        // Miss streak milestones (Miyazaki: even failure is acknowledged)
        int[] missTiers = {10, 25, 50, 100};
        String[] missNames = {"Stubborn", "The Wall", "Unbreakable Will", "Sisyphus"};
        for (int i = 0; i < missTiers.length; i++) {
            list.add(new AchievementDef("miss_" + i, missNames[i], "\uD83E\uDDF1",
                    "Endure " + missTiers[i] + " consecutive misses", "missStreak", missTiers[i],
                    missTiers[i] * 5, 1 + i * 0.005, false));
        }

        // Prestige milestones
        int[] presTiers = {1, 3, 5, 10};
        String[] presNames = {"First Ascension", "Triple Ascended", "Quintuple", "Decade"};
        for (int i = 0; i < presTiers.length; i++) {
            list.add(new AchievementDef("pres_" + i, presNames[i], "\uD83C\uDF1F",
                    "Ascend " + presTiers[i] + " times", "ascensions", presTiers[i],
                    presTiers[i] * 200, 1 + (i + 1) * 0.02, false));
        }

        // Hidden discovery achievements (Kojima: surprises)
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
        list.add(new AchievementDef("disc_drought_break", "Drought Breaker", "\uD83C\uDF27\uFE0F",
                "Land a successful swap after 20+ consecutive misses", "discovery", 6, 300, 1.06, true));

        // Masterpiece completion (Todd Howard: clear goals, freedom to pursue)
        list.add(new AchievementDef("disc_complete", "Completionist", "\uD83D\uDDBC\uFE0F",
                "Complete your first masterpiece", "discovery", 7, 500, 1.08, true));
        list.add(new AchievementDef("disc_perfect", "Perfectionist", "\uD83D\uDC4C",
                "Complete a masterpiece at 99%+ fitness", "discovery", 8, 2000, 1.15, true));

        // Completed images milestones
        int[] compTiers = {1, 3, 5, 10};
        String[] compNames = {"First Canvas", "Gallery Owner", "Museum Curator", "Grand Master"};
        String[] compIcons = {"\uD83D\uDDBC\uFE0F", "\uD83C\uDFDB\uFE0F", "\uD83C\uDFE0", "\uD83C\uDFF0"};
        for (int i = 0; i < compTiers.length; i++) {
            list.add(new AchievementDef("comp_" + i, compNames[i], compIcons[i],
                    "Complete " + compTiers[i] + " masterpiece" + (compTiers[i] > 1 ? "s" : ""),
                    "completedImages", compTiers[i],
                    compTiers[i] * 500, 1 + (i + 1) * 0.02, false));
        }

        // GF accumulation milestones
        double[] gfTiers = {10, 50, 200, 1000};
        String[] gfNames = {"Golden Start", "Golden Hoard", "Golden Age", "Gilded Legend"};
        for (int i = 0; i < gfTiers.length; i++) {
            list.add(new AchievementDef("gf_" + i, gfNames[i], "\uD83E\uDE99",
                    "Accumulate " + (int)gfTiers[i] + " GF", "totalGf", gfTiers[i],
                    gfTiers[i] * 10, 1 + i * 0.01, false));
        }

        return list.toArray(new AchievementDef[0]);
    }

    // ════════════════════════════════════════════════════════════════
    //  EVENTS — rare and impactful
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
        new EventDef("focus_mode", "Focus Mode", "\uD83C\uDFAF",
                "Swap distance halved, 2x retry cycles!", 20, 2.0, 0.0008),
        new EventDef("inspiration", "Inspiration", "\uD83D\uDCA1",
                "5x EP from all sources!", 15, 5.0, 0.0005),
    };

    // ════════════════════════════════════════════════════════════════
    //  ENGINE INITIALIZATION — full state reset
    // ════════════════════════════════════════════════════════════════

    public ClickerEngine getEngine() { return engine; }

    /**
     * Initializes in pixel mode for retro console presets.
     */
    public String initPixelMode(BufferedImage sourceImage, RetroPreset preset, String sampleId) {
        currentSampleId = sampleId;
        long fp = computeFingerprint(sourceImage);
        if (sampleId == null && usedImageFingerprints.contains(fp)) {
            return "This image has already been evolved. Load a different one for your next canvas.";
        }
        if (sampleId == null) {
            usedImageFingerprints.add(fp);
        }
        currentImageFingerprint = fp;

        this.palette = null;
        this.gridW = preset.getWidth();
        this.gridH = preset.getHeight();
        this.triWidth = 1;
        this.triHeight = 1;
        this.scale = 1;

        ep = 0;
        totalEpEarned = 0;
        mc = 0;
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

        upgradeLevels.entrySet().removeIf(e -> {
            UpgradeDef def = findUpgrade(e.getKey());
            return def != null && !def.currency.equals("gf");
        });
        totalUpgradesBought = upgradeLevels.values().stream().mapToInt(Integer::intValue).sum();
        unlockedAchievements.clear();

        if (engine == null) {
            engine = new ClickerEngine();
        }
        engine.initPixelMode(sourceImage, preset);

        if (currentSampleId != null) {
            SampleProgress prev = sampleProgress.get(currentSampleId);
            int plays = prev != null ? prev.playCount() + 1 : 1;
            sampleProgress.put(currentSampleId, new SampleProgress(
                    prev != null ? prev.bestFitness() : 0,
                    prev != null ? prev.bestGain() : 0,
                    plays,
                    prev != null ? prev.completed() : false,
                    prev != null ? prev.ascensionsCount() : 0,
                    prev != null ? prev.totalPlayTimeMs() : 0,
                    prev != null ? prev.totalClicks() : 0));
        }
        return null;
    }

    public String initEngine(BufferedImage sourceImage, Palette palette,
                             int gridW, int gridH,
                             float triWidth, float triHeight, float scale) {
        return initEngine(sourceImage, palette, gridW, gridH, triWidth, triHeight, scale, null);
    }

    public String initEngine(BufferedImage sourceImage, Palette palette,
                             int gridW, int gridH,
                             float triWidth, float triHeight, float scale, String sampleId) {
        currentSampleId = sampleId;
        long fp = computeFingerprint(sourceImage);
        if (sampleId == null && usedImageFingerprints.contains(fp)) {
            return "This image has already been evolved. Load a different one for your next canvas.";
        }
        if (sampleId == null) {
            usedImageFingerprints.add(fp);
        }
        currentImageFingerprint = fp;

        this.palette = palette;
        this.gridW = gridW;
        this.gridH = gridH;
        this.triWidth = triWidth;
        this.triHeight = triHeight;
        this.scale = scale;

        ep = 0;
        totalEpEarned = 0;
        mc = 0;
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

        upgradeLevels.entrySet().removeIf(e -> {
            UpgradeDef def = findUpgrade(e.getKey());
            return def != null && !def.currency.equals("gf");
        });
        totalUpgradesBought = upgradeLevels.values().stream().mapToInt(Integer::intValue).sum();
        unlockedAchievements.clear();

        if (engine == null) {
            engine = new ClickerEngine();
        }
        engine.init(sourceImage, palette, gridW, gridH, triWidth, triHeight, scale, getInitMethod());

        if (currentSampleId != null) {
            SampleProgress prev = sampleProgress.get(currentSampleId);
            int plays = prev != null ? prev.playCount() + 1 : 1;
            sampleProgress.put(currentSampleId, new SampleProgress(
                    prev != null ? prev.bestFitness() : 0,
                    prev != null ? prev.bestGain() : 0,
                    plays,
                    prev != null ? prev.completed() : false,
                    prev != null ? prev.ascensionsCount() : 0,
                    prev != null ? prev.totalPlayTimeMs() : 0,
                    prev != null ? prev.totalClicks() : 0));
        }
        return null;
    }

    /**
     * Default: INIT_RANDOM (shuffled palette) — always start from chaos.
     * Prestige unlocks: Smart Genesis → INIT_SMART, LAP Genesis → INIT_LAP_OPTIMAL.
     * Progress is always gain-based from whatever starting fitness this produces.
     */
    private int getInitMethod() {
        if (getLevel("lap_init") > 0) return ImageEvolver.INIT_LAP_OPTIMAL;
        if (getLevel("smart_init") > 0) return ImageEvolver.INIT_SMART;
        return ImageEvolver.INIT_RANDOM;
    }

    // ════════════════════════════════════════════════════════════════
    //  GAME TICK — called every ~1 second from /api/clicker/state
    // ════════════════════════════════════════════════════════════════

    public List<String> tick() {
        List<String> notifications = new ArrayList<>();
        long now = System.currentTimeMillis();
        totalPlayTimeMs = now - gameStartMs;

        computeEpPerSecond();
        addEp(epPerSecond);

        // Auto-clickers (Yokoi: same mechanic, automated)
        if (engine != null && engine.isInitialized()) {
            double autoRate = computeAutoClickRate();
            if (autoRate > 0) {
                autoClickAccumulator += autoRate;
                int autoClicks = (int) autoClickAccumulator;
                autoClickAccumulator -= autoClicks;

        if (autoClicks > 0) {
                    int autoCycles = 1 + (int) eff("autoCycles") + (int) eff("permCycles");
                    int autoMulti = 1 + (int) eff("autoMulti");
                    int distance = computeSwapDistance();

                    for (int c = 0; c < autoClicks; c++) {
                        ClickerEngine.ClickResult result = engine.performClick(
                                autoMulti, autoCycles, distance, 0);
                        double autoEp = result.fitnessGain > 0
                                ? computeEpForFitnessGain(result.fitnessGain) * 0.5
                                : 0.1;
                        addEp(autoEp);
                    }
                }
            }
        }

        if (currentSampleId != null && engine != null && engine.isInitialized()) {
            double f = engine.getFitness(), g = f - engine.getStartingFitness();
            SampleProgress prev = sampleProgress.get(currentSampleId);
            sampleProgress.put(currentSampleId, new SampleProgress(
                    Math.max(prev != null ? prev.bestFitness() : 0, f),
                    Math.max(prev != null ? prev.bestGain() : 0, g),
                    prev != null ? prev.playCount() : 1,
                    (prev != null && prev.completed()) || f >= MASTERPIECE_MIN_FITNESS,
                    prev != null ? prev.ascensionsCount() : 0,
                    prev != null ? prev.totalPlayTimeMs() : 0,
                    prev != null ? prev.totalClicks() : 0));
        }

        tickEvents(notifications);

        if (now - lastEventCheckMs > 10000) {
            lastEventCheckMs = now;
            double luckBonus = 1.0 + eff("eventLuck");
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
        double base = eff("autoRate");
        if (base <= 0) return 0;
        return base * (1.0 + eff("autoBoost")) * (1.0 + eff("permAuto"))
                * getEventMult("auto_frenzy");
    }

    /**
     * Swap distance computation.
     *
     * Gunpei Yokoi: simple mechanic, deep implications.
     * Base: nearby triangles only (~5% of grid).
     * Each level of Swap Reach extends by ~5%.
     * At high levels: unlimited (full grid).
     */
    private int computeSwapDistance() {
        int n = (engine != null) ? engine.getTriangleCount() : 4000;
        int reachLevel = (int) eff("swapReach") + (int) eff("permReach");
        int baseRange = Math.max(10, n / 20);
        int totalRange = baseRange + (int) (reachLevel * n * 0.05);
        return totalRange >= n ? 0 : totalRange;
    }

    // ════════════════════════════════════════════════════════════════
    //  CLICK — the core mechanic
    //
    //  Shinji Mikami: every click carries tension.
    //  Will it hit? Will the streak continue?
    //  The answer is uncertain — that's the game.
    // ════════════════════════════════════════════════════════════════

    public ClickResponse click() {
        if (engine == null || !engine.isInitialized()) {
            return new ClickResponse(0, 0, 0, 0, 0, false, 0, 0);
        }

        totalClicks++;
        lifetimeClicks++;

        int retryCycles = 1 + (int) eff("retryCycles") + (int) eff("permCycles");
        int swapsPerClick = 1 + (int) eff("multiSwap");
        int swapDistance = computeSwapDistance();
        double smartPct = eff("smartPct");

        boolean critical = false;
        double critChance = eff("critChance") * getEventMult("lucky_streak");
        if (critChance > 0 && Math.random() < critChance) {
            critical = true;
            swapsPerClick *= 3;
            triggerDiscovery(4);
        }

        if (getEventMult("swap_storm") > 1)
            swapsPerClick = (int) (swapsPerClick * getEventMult("swap_storm"));
        if (getEventMult("precision_wave") > 1)
            retryCycles += (int) getEventMult("precision_wave");
        if (getEventMult("focus_mode") > 1) {
            retryCycles *= 2;
            swapDistance = Math.max(10, swapDistance / 2);
        }

        // Record miss streak BEFORE the click for drought-break detection
        int missStreakBefore = engine.getCurrentMissStreak();

        ClickerEngine.ClickResult result = engine.performClick(
                swapsPerClick, retryCycles, swapDistance, smartPct);

        // Discovery: first successful swap
        if (result.successCount > 0 && engine.getSuccessfulSwaps() <= result.successCount) {
            triggerDiscovery(1);
        }
        // Discovery: drought breaker (success after 20+ misses)
        if (result.successCount > 0 && missStreakBefore >= 20) {
            triggerDiscovery(6);
        }

        // EP calculation (Meier: reward meaningful outcomes)
        double baseEp = 1.0 + eff("clickEp");
        double fitnessBonus = result.fitnessGain > 0
                ? computeEpForFitnessGain(result.fitnessGain) : 0;

        // Streak bonus (Hot Hand upgrade)
        double streakMult = 1.0;
        double streakBonusPct = eff("streakBonus");
        if (streakBonusPct > 0 && result.hitStreak > 1) {
            streakMult = 1.0 + streakBonusPct * Math.min(result.hitStreak, 50);
        }

        // Patience bonus (Mikami: tension converts to reward)
        double patienceMult = 1.0;
        double patienceVal = eff("patienceBonus");
        if (patienceVal > 0 && result.successCount > 0 && missStreakBefore >= 5) {
            patienceMult = 1.0 + patienceVal;
        }

        // Genesis Boost: fitness-based EP gains scale up
        double genesisMult = 1.0 + eff("fitnessEpBonus");
        fitnessBonus *= genesisMult;

        double earned = (baseEp + fitnessBonus) * streakMult * patienceMult
                * getEpMultiplier() * getPrestigeMultiplier()
                * getEventMult("golden_hour") * getEventMult("inspiration");
        if (critical) earned *= 2;

        // Chain Lightning: critical hits can chain (15% per level)
        double chainChance = eff("chainChance");
        if (critical && chainChance > 0 && Math.random() < chainChance) {
            int extraSwaps = swapsPerClick;
            ClickerEngine.ClickResult chainResult = engine.performClick(
                    extraSwaps, retryCycles, swapDistance, smartPct);
            double chainEp = (1.0 + eff("clickEp")) * 2 * getEpMultiplier() * getPrestigeMultiplier()
                    * getEventMult("golden_hour") * getEventMult("inspiration");
            if (chainResult.fitnessGain > 0) {
                chainEp += computeEpForFitnessGain(chainResult.fitnessGain) * genesisMult
                        * getEpMultiplier() * getPrestigeMultiplier()
                        * getEventMult("golden_hour") * getEventMult("inspiration");
            }
            earned += chainEp;
        }

        addEp(earned);

        // Lifetime miss streak for secret unlocks (Kojima: suffering rewards)
        if (engine != null && engine.getLongestMissStreak() > lifetimeLongestMissStreak) {
            lifetimeLongestMissStreak = engine.getLongestMissStreak();
        }

        // MC generation (Resonance: MC chance scales with EP Overflow level)
        double mcEarned = 0;
        double mcChance = eff("mcChance");
        double resonanceMult = 1.0;
        if (getLevel("resonance") >= 1 && getLevel("ep_multiplier") >= 1) {
            resonanceMult = 1.0 + getLevel("ep_multiplier") * 0.1;
        }
        mcChance *= resonanceMult;
        if (mcChance > 0 && Math.random() < mcChance) {
            mcEarned = 1;
            triggerDiscovery(5);
        }
        if (isEventActive("crystal_rain")) mcEarned += 0.2;
        mc += mcEarned;

        return new ClickResponse(earned, result.fitnessGain, result.newFitness,
                result.successCount, result.attemptCount, critical,
                result.missStreak, result.hitStreak);
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

    public boolean isMaxed(String upgradeId) {
        UpgradeDef def = findUpgrade(upgradeId);
        if (def == null) return false;
        return def.maxLevel > 0 && getLevel(upgradeId) >= def.maxLevel;
    }

    public boolean canAfford(String upgradeId) {
        UpgradeDef def = findUpgrade(upgradeId);
        if (def == null) return false;
        if (isMaxed(upgradeId)) return false;
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

        saveGallerySnapshot(true);

        gf += reward;
        ascensionCount++;
        ep = 0;
        totalEpEarned = 0;
        mc = 0;
        totalClicks = 0;
        totalAchievementsUnlocked = 0;
        autoClickAccumulator = 0;
        activeEvents.clear();
        achievementQueue.clear();

        upgradeLevels.entrySet().removeIf(e -> {
            UpgradeDef def = findUpgrade(e.getKey());
            return def != null && !def.currency.equals("gf");
        });
        totalUpgradesBought = upgradeLevels.values().stream().mapToInt(Integer::intValue).sum();
        unlockedAchievements.clear();
        computeEpPerSecond();

        engine = null;

        triggerDiscovery(3);
        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  MASTERPIECE COMPLETION — the big loop
    //
    //  Will Wright: emergent long-term goals.
    //  Todd Howard: "See that perfect image? You can complete it."
    //  Complete the current image at high fitness, earn massive GF,
    //  then load a new image for a fresh canvas with accumulated power.
    // ════════════════════════════════════════════════════════════════

    public static final double MASTERPIECE_MIN_FITNESS = 0.85;

    public double calcMasterpieceReward() {
        if (engine == null || !engine.isInitialized()) return 0;
        double fitness = engine.getFitness();
        if (fitness < MASTERPIECE_MIN_FITNESS) return 0;
        return Math.floor(fitness * 50 + completedImages * 5);
    }

    public MasterpieceResult completeMasterpiece() {
        if (engine == null || !engine.isInitialized()) return null;
        double fitness = engine.getFitness();
        if (fitness < MASTERPIECE_MIN_FITNESS) return null;

        saveGallerySnapshot();

        double gfReward = Math.floor(fitness * 50 + completedImages * 5);
        gf += gfReward;
        completedImages++;
        bestCompletionFitness = Math.max(bestCompletionFitness, fitness);

        triggerDiscovery(7);
        if (fitness >= 0.99) triggerDiscovery(8);

        // Full reset — new canvas required. Keep ascensionCount so prestige tab stays visible.
        ep = 0;
        totalEpEarned = 0;
        mc = 0;
        totalClicks = 0;
        totalUpgradesBought = 0;
        totalAchievementsUnlocked = 0;
        epPerSecond = 0;
        autoClickAccumulator = 0;
        activeEvents.clear();
        achievementQueue.clear();

        upgradeLevels.entrySet().removeIf(e -> {
            UpgradeDef def = findUpgrade(e.getKey());
            return def != null && !def.currency.equals("gf");
        });
        totalUpgradesBought = upgradeLevels.values().stream().mapToInt(Integer::intValue).sum();
        unlockedAchievements.clear();

        engine = null;

        return new MasterpieceResult(gfReward, fitness, completedImages);
    }

    public int getCompletedImages() { return completedImages; }
    public double getBestCompletionFitness() { return bestCompletionFitness; }

    // ════════════════════════════════════════════════════════════════
    //  GALLERY — every evolved image is preserved
    //
    //  Todd Howard: "See that gallery? You filled it."
    //  Each ascension and masterpiece saves a snapshot of your work.
    // ════════════════════════════════════════════════════════════════

    public static long computeFingerprint(BufferedImage img) {
        if (img == null) return 0;
        long hash = img.getWidth() * 31L + img.getHeight();
        int total = img.getWidth() * img.getHeight();
        int step = Math.max(1, total / 100);
        for (int i = 0; i < total; i += step) {
            int x = i % img.getWidth();
            int y = i / img.getWidth();
            hash = hash * 31 + img.getRGB(x, y);
        }
        return hash;
    }

    public boolean isImageAlreadyUsed(BufferedImage img) {
        return usedImageFingerprints.contains(computeFingerprint(img));
    }

    private void saveGallerySnapshot() {
        saveGallerySnapshot(false);
    }

    private void saveGallerySnapshot(boolean fromAscension) {
        if (engine == null || !engine.isInitialized()) return;
        double finalFit = engine.getFitness();
        double gain = finalFit - engine.getStartingFitness();
        if (currentSampleId != null) {
            SampleProgress prev = sampleProgress.get(currentSampleId);
            int plays = prev != null ? prev.playCount() : 1;
            boolean completed = finalFit >= MASTERPIECE_MIN_FITNESS;
            int asc = (prev != null ? prev.ascensionsCount() : 0) + (fromAscension ? 1 : 0);
            long totTime = (prev != null ? prev.totalPlayTimeMs() : 0) + totalPlayTimeMs;
            long totClicks = (prev != null ? prev.totalClicks() : 0) + totalClicks;
            sampleProgress.put(currentSampleId, new SampleProgress(
                    Math.max(prev != null ? prev.bestFitness() : 0, finalFit),
                    Math.max(prev != null ? prev.bestGain() : 0, gain),
                    plays, prev != null ? prev.completed() || completed : completed,
                    asc, totTime, totClicks));
        }
        byte[] thumb = engine.generateThumbnail(120);
        if (thumb == null) return;
        String b64 = Base64.getEncoder().encodeToString(thumb);
        gallery.add(new GalleryEntry(
                currentImageFingerprint, b64,
                engine.getStartingFitness(), finalFit, gain,
                totalClicks, engine.getSuccessfulSwaps(), totalPlayTimeMs,
                ascensionCount, System.currentTimeMillis()));
    }

    public List<GalleryEntry> getGallery() {
        return Collections.unmodifiableList(gallery);
    }

    public String getGalleryJson() {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("[");
        for (int i = 0; i < gallery.size(); i++) {
            GalleryEntry g = gallery.get(i);
            sb.append("{\"fingerprint\":").append(g.fingerprint()).append(",");
            sb.append("\"thumbnail\":\"data:image/jpeg;base64,").append(g.thumbnailBase64).append("\",");
            sb.append("\"startFitness\":").append(g.startFitness).append(",");
            sb.append("\"finalFitness\":").append(g.finalFitness).append(",");
            sb.append("\"fitnessGain\":").append(g.fitnessGain).append(",");
            sb.append("\"clicks\":").append(g.totalClicks).append(",");
            sb.append("\"swaps\":").append(g.successSwaps).append(",");
            sb.append("\"playTimeMs\":").append(g.playTimeMs).append(",");
            sb.append("\"ascensions\":").append(g.ascensionCount).append(",");
            sb.append("\"timestamp\":").append(g.timestamp).append("}");
            if (i < gallery.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public boolean isSampleUnlocked(SampleImageProvider.SampleDef def) {
        boolean primary = checkUnlock(def.unlockType(), def.unlockValue());
        if (primary) return true;
        if (def.unlockTypeAlt() != null && !def.unlockTypeAlt().isEmpty() && def.unlockValueAlt() >= 0) {
            return checkUnlock(def.unlockTypeAlt(), def.unlockValueAlt());
        }
        return false;
    }

    private boolean checkUnlock(String type, double value) {
        return switch (type) {
            case "always" -> true;
            case "clicks" -> lifetimeClicks >= (long) value;
            case "ep" -> lifetimeEpEarned >= value;
            case "ascensions" -> ascensionCount >= (int) value;
            case "masterpieces" -> completedImages >= (int) value;
            case "gf" -> gf >= value;
            case "miss_streak" -> lifetimeLongestMissStreak >= (int) value;
            default -> false;
        };
    }

    private String formatUnlockHint(SampleImageProvider.SampleDef d) {
        if ("always".equals(d.unlockType())) return "";
        String a = fmtCond(d.unlockType(), d.unlockValue());
        if (d.unlockTypeAlt() != null && !d.unlockTypeAlt().isEmpty() && d.unlockValueAlt() >= 0) {
            String b = fmtCond(d.unlockTypeAlt(), d.unlockValueAlt());
            return a + " or " + b;
        }
        return a;
    }

    private String fmtCond(String type, double val) {
        return switch (type) {
            case "clicks" -> String.format("%,d clicks", (long) val);
            case "ep" -> String.format("%,.0f EP", val);
            case "ascensions" -> (int) val + " ascensions";
            case "masterpieces" -> (int) val + " masterpieces";
            case "gf" -> (int) val + " GF";
            case "miss_streak" -> (int) val + " miss streak";
            default -> type + " ≥ " + val;
        };
    }

    public boolean isSampleDiscovered(SampleImageProvider.SampleDef def) {
        if (!def.secret()) return true;
        if (discoveredSamples.contains(def.id())) return true;
        if (isSampleUnlocked(def)) {
            discoveredSamples.add(def.id());
            return true;
        }
        return false;
    }

    public String getSamplesJson() {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("{\"samples\":[");
        var defs = SampleImageProvider.getAllDefs().stream().sorted(Comparator.comparingInt(SampleImageProvider.SampleDef::sortOrder)).toList();
        for (int i = 0; i < defs.size(); i++) {
            var d = defs.get(i);
            boolean discovered = isSampleDiscovered(d);
            boolean unlocked = isSampleUnlocked(d);
            SampleProgress prog = sampleProgress.get(d.id());
            sb.append("{\"id\":\"").append(d.id()).append("\",");
            sb.append("\"name\":\"").append(jsonEsc(discovered ? d.name() : "???")).append("\",");
            sb.append("\"desc\":\"").append(jsonEsc(discovered ? d.description() : "Meet the condition to reveal.")).append("\",");
            sb.append("\"category\":\"").append(d.category()).append("\",");
            sb.append("\"unlockType\":\"").append(d.unlockType()).append("\",");
            sb.append("\"unlockValue\":").append(d.unlockValue()).append(",");
            sb.append("\"unlockTypeAlt\":\"").append(d.unlockTypeAlt() != null ? d.unlockTypeAlt() : "").append("\",");
            sb.append("\"unlockValueAlt\":").append(d.unlockValueAlt() >= 0 ? d.unlockValueAlt() : -1).append(",");
            sb.append("\"unlockHint\":\"").append(jsonEsc(formatUnlockHint(d))).append("\",");
            sb.append("\"secret\":").append(d.secret()).append(",");
            sb.append("\"discovered\":").append(discovered).append(",");
            sb.append("\"unlocked\":").append(unlocked).append(",");
            sb.append("\"bestFitness\":").append(prog != null ? prog.bestFitness() : 0).append(",");
            sb.append("\"bestGain\":").append(prog != null ? prog.bestGain() : 0).append(",");
            sb.append("\"playCount\":").append(prog != null ? prog.playCount() : 0).append(",");
            sb.append("\"ascensionsCount\":").append(prog != null ? prog.ascensionsCount() : 0).append(",");
            sb.append("\"totalPlayTimeMs\":").append(prog != null ? prog.totalPlayTimeMs() : 0).append(",");
            sb.append("\"totalClicks\":").append(prog != null ? prog.totalClicks() : 0).append(",");
            sb.append("\"completed\":").append(prog != null && prog.completed()).append(",");
            sb.append("\"recommendedPreset\":").append(d.recommendedPreset() != null
                    ? "\"" + d.recommendedPreset() + "\"" : "null").append("}");
            if (i < defs.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════════
    //  COMPUTED VALUES
    // ════════════════════════════════════════════════════════════════

    /** Shorthand for getEffectValue */
    private double eff(String effectId) {
        double total = 0;
        for (UpgradeDef def : UPGRADES) {
            if (def.effectTarget.equals(effectId)) {
                total += getLevel(def.id) * def.effectPerLevel;
            }
        }
        return total;
    }

    private void computeEpPerSecond() {
        double base = 0;
        for (UpgradeDef def : UPGRADES) {
            int level = getLevel(def.id);
            if (level > 0 && !def.currency.equals("gf")) {
                base += level * 0.05 * (1 + def.baseCost / 200.0);
            }
        }
        base *= getEpMultiplier() * getPrestigeMultiplier() * getEventMult("golden_hour");
        epPerSecond = base;
    }

    private double getEpMultiplier() {
        double total = 1.0;
        for (UpgradeDef def : UPGRADES) {
            if (def.effectTarget.equals("epMult") && def.effectType.equals("mult")) {
                total += getLevel(def.id) * def.effectPerLevel;
            }
        }
        // Eternal Fortune: +3% EP per ascension per level
        double ascBonus = eff("ascensionEpBonus");
        if (ascBonus > 0 && ascensionCount > 0) {
            total *= 1.0 + ascensionCount * ascBonus;
        }
        // Canvas Mastery: bonus per completed masterpiece
        double canvasVal = eff("canvasBonus");
        if (canvasVal > 0 && completedImages > 0) {
            total += completedImages * canvasVal;
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

    private double getEventMult(String eventId) {
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
        return getEventMult(eventId) > 1.0;
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
        double startFit = (engine != null && engine.isInitialized()) ? engine.getStartingFitness() : 0;
        double fitnessGainPct = (fitness - startFit) * 100;
        long successSwaps = (engine != null) ? engine.getSuccessfulSwaps() : 0;
        int longestHit = (engine != null) ? engine.getLongestHitStreak() : 0;
        int longestMiss = (engine != null) ? engine.getLongestMissStreak() : 0;
        int newUnlocks = 0;

        for (AchievementDef ad : ACHIEVEMENTS) {
            if (unlockedAchievements.contains(ad.id)) continue;

            boolean unlocked = switch (ad.conditionType) {
                case "fitnessGain" -> fitnessGainPct >= ad.threshold;
                case "successSwaps" -> successSwaps >= ad.threshold;
                case "totalEp" -> totalEpEarned >= ad.threshold;
                case "clicks" -> totalClicks >= ad.threshold;
                case "playTime" -> (totalPlayTimeMs / 1000) >= ad.threshold;
                case "upgrades" -> totalUpgradesBought >= ad.threshold;
                case "ascensions" -> ascensionCount >= ad.threshold;
                case "hitStreak" -> longestHit >= ad.threshold;
                case "missStreak" -> longestMiss >= ad.threshold;
                case "completedImages" -> completedImages >= ad.threshold;
                case "totalGf" -> gf >= ad.threshold;
                default -> false;
            };

            if (unlocked) {
                unlockedAchievements.add(ad.id);
                totalAchievementsUnlocked++;
                addEp(ad.epReward);
                newUnlocks++;
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
        lifetimeEpEarned += amount;
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
        sb.append("  \"autoClickRate\":").append(computeAutoClickRate()).append(",\n");
        sb.append("  \"autoClickerLevel\":").append(getLevel("auto_clicker")).append(",\n");
        sb.append("  \"prestigeReward\":").append(calcPrestigeReward()).append(",\n");
        sb.append("  \"prestigeMultiplier\":").append(getPrestigeMultiplier()).append(",\n");

        boolean engineReady = engine != null && engine.isInitialized();
        sb.append("  \"initialized\":").append(engineReady).append(",\n");
        sb.append("  \"fitness\":").append(engineReady ? engine.getFitness() : 0).append(",\n");
        sb.append("  \"startingFitness\":").append(engineReady ? engine.getStartingFitness() : 0).append(",\n");
        double fitnessGain = engineReady ? (engine.getFitness() - engine.getStartingFitness()) : 0;
        sb.append("  \"fitnessGain\":").append(fitnessGain).append(",\n");
        sb.append("  \"totalSwaps\":").append(engineReady ? engine.getTotalSwaps() : 0).append(",\n");
        sb.append("  \"successSwaps\":").append(engineReady ? engine.getSuccessfulSwaps() : 0).append(",\n");
        sb.append("  \"triangleCount\":").append(engineReady ? engine.getTriangleCount() : 0).append(",\n");
        sb.append("  \"missStreak\":").append(engineReady ? engine.getCurrentMissStreak() : 0).append(",\n");
        sb.append("  \"hitStreak\":").append(engineReady ? engine.getCurrentHitStreak() : 0).append(",\n");
        sb.append("  \"longestHitStreak\":").append(engineReady ? engine.getLongestHitStreak() : 0).append(",\n");
        sb.append("  \"longestMissStreak\":").append(engineReady ? engine.getLongestMissStreak() : 0).append(",\n");

        // Swap reach info
        int swapDist = computeSwapDistance();
        int n = engineReady ? engine.getTriangleCount() : 0;
        String reachLabel = swapDist == 0 ? "Unlimited" : ((int)(100.0 * swapDist / Math.max(1, n)) + "%");
        sb.append("  \"swapReach\":\"").append(reachLabel).append("\",\n");
        sb.append("  \"completedImages\":").append(completedImages).append(",\n");
        sb.append("  \"bestCompletionFitness\":").append(bestCompletionFitness).append(",\n");
        sb.append("  \"masterpieceReward\":").append(calcMasterpieceReward()).append(",\n");
        sb.append("  \"masterpieceMinFitness\":").append(MASTERPIECE_MIN_FITNESS).append(",\n");
        sb.append("  \"galleryCount\":").append(gallery.size()).append(",\n");

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

    /**
     * Tree-based visibility: upgrades unlock only when their conditions are met.
     * Miyamoto: discovery beats overwhelming choice. Will Wright: meaningful gates.
     */
    private boolean isUpgradeVisible(UpgradeDef u) {
        switch (u.currency) {
            case "ep" -> {
                return switch (u.id) {
                    case "click_ep", "retry_cycles" -> true;
                    case "swap_reach" -> getLevel("retry_cycles") >= 1;
                    case "multi_swap" -> getLevel("swap_reach") >= 1;
                    case "deep_focus" -> getLevel("multi_swap") >= 5;
                    case "auto_clicker" -> totalEpEarned >= 400 || getLevel("retry_cycles") >= 5;
                    case "auto_cycles" -> getLevel("auto_clicker") >= 1;
                    case "auto_multi" -> getLevel("auto_cycles") >= 1;
                    case "idle_mastery" -> getLevel("auto_multi") >= 3;
                    case "smart_pick" -> getLevel("retry_cycles") >= 3;
                    case "streak_bonus" -> getLevel("smart_pick") >= 1;
                    case "patience" -> getLevel("streak_bonus") >= 1;
                    default -> true;
                };
            }
            case "mc" -> {
                if (mc <= 0 && totalEpEarned < 2000) return false;
                return switch (u.id) {
                    case "critical_swap", "mc_finder" -> true;
                    case "ep_multiplier", "lucky_events" -> getLevel("critical_swap") >= 1 || getLevel("mc_finder") >= 1;
                    case "auto_boost" -> getLevel("ep_multiplier") >= 1 || getLevel("lucky_events") >= 1;
                    case "chain_lightning" -> getLevel("auto_boost") >= 3;
                    case "resonance" -> getLevel("ep_multiplier") >= 1 && getLevel("mc_finder") >= 1;
                    default -> true;
                };
            }
            case "gf" -> {
                if (ascensionCount == 0) return false;
                return switch (u.id) {
                    case "eternal_cycles", "eternal_auto", "eternal_reach" -> true;
                    case "smart_init" -> getLevel("eternal_cycles") >= 1 || getLevel("eternal_auto") >= 1 || getLevel("eternal_reach") >= 1;
                    case "lap_init" -> getLevel("smart_init") >= 1;
                    case "canvas_mastery" -> completedImages >= 1;
                    case "golden_touch" -> getLevel("canvas_mastery") >= 1;
                    case "eternal_fortune" -> getLevel("eternal_auto") >= 5;
                    case "genesis_boost" -> getLevel("lap_init") >= 1;
                    default -> true;
                };
            }
            default -> { return true; }
        }
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
                                int successCount, int attemptCount, boolean critical,
                                int missStreak, int hitStreak) {}

    public record MasterpieceResult(double gfReward, double finalFitness, int totalCompleted) {}

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

    static String fmtBig(double n) {
        if (n >= 1e12) return String.format("%.1fT", n / 1e12);
        if (n >= 1e9) return String.format("%.1fB", n / 1e9);
        if (n >= 1e6) return String.format("%.1fM", n / 1e6);
        if (n >= 1e3) return String.format("%.1fK", n / 1e3);
        return String.format("%.0f", n);
    }

    private static String fmtDuration(long seconds) {
        if (seconds >= 86400) return (seconds / 86400) + " day" + (seconds >= 172800 ? "s" : "");
        if (seconds >= 3600) return (seconds / 3600) + " hour" + (seconds >= 7200 ? "s" : "");
        if (seconds >= 60) return (seconds / 60) + " minute" + (seconds >= 120 ? "s" : "");
        return seconds + " second" + (seconds != 1 ? "s" : "");
    }
}
