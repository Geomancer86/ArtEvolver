package com.rndmodgames.evolver.clicker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core game state engine for the Cookie-Clicker-style "Evolution Clicker" mode.
 *
 * Design philosophy (channeling the greats):
 *   Carmack  — deep technical layers, every upgrade maps to real parameters
 *   Will Wright — emergent complexity from simple multiplicative rules
 *   Sid Meier — "one more upgrade" dopamine, interesting decisions
 *   Miyamoto  — immediate tactile feedback, juice on every action
 *   Kojima   — narrative meta-layer, the algorithm "becoming aware"
 *
 * The game ties DIRECTLY to the genetic algorithm: upgrades modify real
 * EvolutionConfig parameters, fitness improvements generate real currency,
 * and prestige resets push toward ever-higher fitness ceilings.
 */
public class ClickerState {

    // ════════════════════════════════════════════════════════════════
    //  CURRENCIES
    // ════════════════════════════════════════════════════════════════
    private double ep = 0;           // Evolution Points — main spendable currency
    private double totalEpEarned = 0;// Lifetime EP (never resets, used for prestige calc)
    private double mc = 0;           // Mutation Crystals — rare currency
    private double gf = 0;           // Genome Fragments — prestige currency
    private long totalClicks = 0;
    private long totalUpgradesBought = 0;
    private long gameStartMs = System.currentTimeMillis();
    private long totalPlayTimeMs = 0;
    private double highestFitness = 0;
    private long highestIterPerSec = 0;
    private int ascensionCount = 0;
    private int totalAchievementsUnlocked = 0;

    // Production rates (computed)
    private double epPerSecond = 0;
    private double epPerClick = 1;

    // Active events
    private final List<ActiveEvent> activeEvents = new ArrayList<>();
    private long lastEventCheckMs = 0;

    // Upgrade levels
    private final Map<String, Integer> upgradeLevels = new ConcurrentHashMap<>();

    // Unlocked achievements
    private final Set<String> unlockedAchievements = Collections.synchronizedSet(new LinkedHashSet<>());

    // Recently unlocked (for popup queue)
    private final List<String> achievementQueue = Collections.synchronizedList(new ArrayList<>());

    // ════════════════════════════════════════════════════════════════
    //  UPGRADE DEFINITIONS (52 upgrades across 8 categories)
    // ════════════════════════════════════════════════════════════════

    public static final UpgradeDef[] UPGRADES = {
        // ─── MUTATION LAB ───
        new UpgradeDef("random_swap_power", "Random Swap Power", "Mutation Lab",
                "Increases random mutation attempts. The foundation of all evolution.",
                10, 1.14, 200, "ep", "randomMutationChances", 50, "flat"),
        new UpgradeDef("grid_mutation", "Grid Awareness", "Mutation Lab",
                "Enables grid-based mutations that respect spatial structure.",
                25, 1.15, 200, "ep", "gridMutationChances", 2, "flat"),
        new UpgradeDef("close_mutation", "Precision Touch", "Mutation Lab",
                "Enables mutations that swap nearby similar colors for fine tuning.",
                50, 1.15, 200, "ep", "closeMutationChances", 1, "flat"),
        new UpgradeDef("targeted_swap", "Targeted Strikes", "Mutation Lab",
                "Identifies the worst-matching triangle and swaps it intelligently.",
                100, 1.16, 200, "ep", "targetedSwapAttempts", 1, "flat"),
        new UpgradeDef("mutation_efficiency", "Mutation Efficiency", "Mutation Lab",
                "Fine-tunes mutation percentages for more precise changes.",
                200, 1.18, 100, "ep", "mutationPercent", 0.0001, "flat"),
        new UpgradeDef("double_mutation", "Double Down", "Mutation Lab",
                "Chance to apply each mutation twice. Doubles the exploration.",
                500, 1.20, 50, "ep", "doubleMutationChance", 0.02, "flat"),
        new UpgradeDef("chain_reaction", "Chain Reaction", "Mutation Lab",
                "Successful mutations can trigger additional cascading mutations.",
                1000, 1.22, 50, "ep", "chainReactionChance", 0.01, "flat"),
        new UpgradeDef("mutation_mastery", "Mutation Mastery", "Mutation Lab",
                "Global multiplier to ALL mutation effectiveness.",
                2000, 1.25, 100, "ep", "mutationMultiplier", 0.05, "mult"),

        // ─── POPULATION LAB ───
        new UpgradeDef("population_size", "Population Growth", "Population Lab",
                "Larger populations explore more of the fitness landscape.",
                30, 1.30, 100, "ep", "population", 1, "flat"),
        new UpgradeDef("elite_retention", "Elite Retention", "Population Lab",
                "Keep more top performers between generations.",
                80, 1.20, 50, "ep", "eliteKeep", 1, "flat"),
        new UpgradeDef("diversity_engine", "Genetic Diversity", "Population Lab",
                "Maintains population diversity, preventing premature convergence.",
                150, 1.18, 100, "ep", "diversityBonus", 0.02, "flat"),
        new UpgradeDef("immigration", "Fresh Blood", "Population Lab",
                "Periodically introduces random new members to the population.",
                300, 1.20, 50, "ep", "immigrationRate", 0.01, "flat"),
        new UpgradeDef("crossover_partners", "Crossover Partners", "Population Lab",
                "More candidates for crossover means better genetic mixing.",
                500, 1.25, 50, "ep", "crossoverMax", 1, "flat"),
        new UpgradeDef("population_mastery", "Population Mastery", "Population Lab",
                "Global multiplier to population effectiveness.",
                2500, 1.28, 100, "ep", "populationMultiplier", 0.05, "mult"),

        // ─── CROSSOVER LAB ───
        new UpgradeDef("block_crossover", "Block Crossover", "Crossover Lab",
                "Enables spatial block crossover between parents.",
                60, 1.18, 100, "ep", "blockCrossoverStrength", 0.05, "flat"),
        new UpgradeDef("crossover_rate", "Crossover Frequency", "Crossover Lab",
                "More frequent crossover accelerates trait combination.",
                120, 1.16, 100, "ep", "crossoverRate", 0.02, "flat"),
        new UpgradeDef("multi_parent", "Multi-Parent Breeding", "Crossover Lab",
                "Blend genes from 3+ parents for richer offspring.",
                400, 1.22, 50, "ep", "multiParentCount", 1, "flat"),
        new UpgradeDef("adaptive_crossover", "Adaptive Crossover", "Crossover Lab",
                "Crossover rate adapts based on population fitness variance.",
                800, 1.24, 50, "ep", "adaptiveCrossoverStrength", 0.03, "flat"),
        new UpgradeDef("crossover_mastery", "Crossover Mastery", "Crossover Lab",
                "Global multiplier to all crossover operations.",
                3000, 1.28, 100, "ep", "crossoverMultiplier", 0.05, "mult"),

        // ─── INITIALIZATION ───
        new UpgradeDef("smart_init", "Smart Initialization", "Initialization",
                "Greedy color assignment gives a massive head start.",
                40, 1.20, 50, "ep", "smartInitQuality", 0.05, "flat"),
        new UpgradeDef("lap_solver", "LAP Solver", "Initialization",
                "Optimal initial assignment via the Hungarian algorithm.",
                200, 1.25, 50, "ep", "lapSolverPrecision", 0.05, "flat"),
        new UpgradeDef("hybrid_init", "Hybrid Init", "Initialization",
                "Combines multiple initialization strategies for best results.",
                500, 1.28, 30, "ep", "hybridInitStrength", 0.05, "flat"),
        new UpgradeDef("init_mastery", "Init Mastery", "Initialization",
                "Global multiplier to initialization quality.",
                1500, 1.30, 100, "ep", "initMultiplier", 0.05, "mult"),

        // ─── COMPUTING ───
        new UpgradeDef("thread_count", "Processing Power", "Computing",
                "Each thread evolves independently. More threads = more exploration.",
                20, 1.35, 100, "ep", "threads", 1, "flat"),
        new UpgradeDef("batch_size", "Batch Iterations", "Computing",
                "More iterations per evolution batch before checking results.",
                50, 1.15, 100, "ep", "evolveIterations", 1, "flat"),
        new UpgradeDef("memory_cache", "Memory Cache", "Computing",
                "Cache color objects to reduce garbage collection pressure.",
                100, 1.20, 50, "ep", "cacheSize", 256, "flat"),
        new UpgradeDef("cpu_affinity", "CPU Affinity", "Computing",
                "Better thread scheduling for consistent performance.",
                250, 1.22, 20, "ep", "cpuAffinityBonus", 0.05, "flat"),
        new UpgradeDef("parallel_eval", "Parallel Evaluation", "Computing",
                "Evaluate fitness in parallel across multiple cores.",
                600, 1.25, 50, "ep", "parallelEvalBonus", 0.03, "flat"),
        new UpgradeDef("computing_mastery", "Computing Mastery", "Computing",
                "Global multiplier to all computational throughput.",
                4000, 1.30, 100, "ep", "computingMultiplier", 0.05, "mult"),

        // ─── TOURNAMENT ───
        new UpgradeDef("contestant_slots", "Contestant Slots", "Tournament",
                "More contestants means more diverse parameter exploration.",
                100, 1.30, 50, "ep", "contestantSlots", 1, "flat"),
        new UpgradeDef("cutoff_speed", "Quick Culling", "Tournament",
                "Faster tournament cycles for more rapid parameter evolution.",
                200, 1.20, 50, "ep", "cutoffReduction", 2, "flat"),
        new UpgradeDef("spawn_rate", "Multi-Spawn", "Tournament",
                "Spawn multiple new contestants per culling cycle.",
                500, 1.25, 50, "ep", "spawnsPerTick", 1, "flat"),
        new UpgradeDef("adaptive_tournament", "Adaptive Intelligence", "Tournament",
                "Tournament automatically adjusts timing based on progress.",
                1000, 1.28, 50, "ep", "adaptiveBonus", 0.05, "flat"),
        new UpgradeDef("grace_extension", "Extended Grace", "Tournament",
                "New contestants get more time to prove themselves.",
                300, 1.18, 50, "ep", "graceBonus", 1, "flat"),
        new UpgradeDef("tournament_mastery", "Tournament Mastery", "Tournament",
                "Global multiplier to tournament evolution speed.",
                5000, 1.30, 100, "ep", "tournamentMultiplier", 0.05, "mult"),

        // ─── META-EVOLUTION ───
        new UpgradeDef("gene_crossover", "Gene Crossover Rate", "Meta-Evolution",
                "How aggressively the meta-GA crosses parameter genes.",
                300, 1.20, 100, "ep", "geneCrossoverRate", 0.01, "flat"),
        new UpgradeDef("gene_mutation", "Gene Mutation Range", "Meta-Evolution",
                "How far the meta-GA can explore parameter space.",
                400, 1.20, 100, "ep", "geneMutationRange", 0.01, "flat"),
        new UpgradeDef("composite_tuning", "Composite Weights", "Meta-Evolution",
                "Better ranking formula for identifying truly best contestants.",
                600, 1.22, 50, "ep", "compositeTuning", 0.02, "flat"),
        new UpgradeDef("ancestry_depth", "Deep Ancestry", "Meta-Evolution",
                "Multi-generational breeding with grandparent and great-grandparent genes.",
                1000, 1.25, 50, "ep", "ancestryDepthBonus", 1, "flat"),
        new UpgradeDef("convergence_detect", "Convergence Detection", "Meta-Evolution",
                "Detect and break out of local optima earlier.",
                800, 1.22, 50, "ep", "convergenceDetection", 0.03, "flat"),
        new UpgradeDef("meta_mastery", "Meta Mastery", "Meta-Evolution",
                "Global multiplier to meta-evolution intelligence.",
                6000, 1.32, 100, "ep", "metaMultiplier", 0.05, "mult"),

        // ─── SPECIAL (MC currency) ───
        new UpgradeDef("golden_mutation", "Golden Mutations", "Special",
                "Chance for mutations to be 10x more effective. Pure magic.",
                5, 1.40, 50, "mc", "goldenMutationChance", 0.005, "flat"),
        new UpgradeDef("critical_hit", "Critical Hits", "Special",
                "Chance for a massive fitness jump. Lightning strikes!",
                10, 1.45, 50, "mc", "criticalHitChance", 0.003, "flat"),
        new UpgradeDef("time_warp", "Time Warp", "Special",
                "Temporarily doubles evolution speed. Bends time itself.",
                15, 1.35, 50, "mc", "timeWarpStrength", 0.02, "flat"),
        new UpgradeDef("fitness_magnet", "Fitness Magnet", "Special",
                "Attracts nearby fitness peaks. The landscape bends toward you.",
                20, 1.50, 50, "mc", "fitnessMagnetStrength", 0.01, "flat"),
        new UpgradeDef("auto_click", "Auto-Clicker", "Special",
                "Generates automatic clicks. Evolution never stops.",
                3, 1.30, 100, "mc", "autoClickRate", 0.5, "flat"),
        new UpgradeDef("ep_overflow", "EP Overflow", "Special",
                "Multiply ALL Evolution Point gains. Pure exponential growth.",
                25, 1.50, 100, "mc", "epMultiplier", 0.10, "mult"),
        new UpgradeDef("lucky_star", "Lucky Star", "Special",
                "Better random events. Fortune favors the evolved.",
                8, 1.35, 50, "mc", "eventLuckBonus", 0.05, "flat"),
        new UpgradeDef("prestige_bonus", "Legacy Power", "Special",
                "Better prestige rewards. Your ancestors empower you.",
                30, 1.55, 50, "mc", "prestigeMultiplier", 0.10, "mult"),

        // ─── PRESTIGE (GF currency, available after first ascension) ───
        new UpgradeDef("eternal_speed", "Eternal Speed", "Prestige",
                "Permanent speed bonus that persists through ascensions.",
                5, 1.60, 100, "gf", "eternalSpeed", 0.05, "mult"),
        new UpgradeDef("eternal_fitness", "Eternal Fitness", "Prestige",
                "Permanent fitness ceiling bonus. Push past the limits.",
                10, 1.65, 100, "gf", "eternalFitness", 0.03, "mult"),
        new UpgradeDef("eternal_production", "Eternal Production", "Prestige",
                "Permanent EP production bonus. The wealth of ages.",
                8, 1.55, 100, "gf", "eternalEp", 0.08, "mult"),
        new UpgradeDef("eternal_fortune", "Eternal Fortune", "Prestige",
                "Permanent rare event chance bonus. Luck of the ancients.",
                15, 1.70, 50, "gf", "eternalLuck", 0.05, "mult"),
    };

    // ════════════════════════════════════════════════════════════════
    //  ACHIEVEMENT DEFINITIONS (120+ achievements)
    // ════════════════════════════════════════════════════════════════

    public static final AchievementDef[] ACHIEVEMENTS = generateAchievements();

    private static AchievementDef[] generateAchievements() {
        List<AchievementDef> list = new ArrayList<>();

        // Fitness milestones
        double[] fitTiers = {0.1, 0.5, 1, 2, 5, 10, 15, 20, 25, 30, 40, 50, 60, 70, 80, 90, 95, 99, 99.5, 99.9};
        String[] fitNames = {"First Breath", "Seeing Colors", "Faint Outline", "Sketchy",
                "Taking Shape", "Recognizable", "Getting There", "Double Take",
                "Quarter Century", "Third Life", "Almost Half", "Halfway Home",
                "Past the Peak", "Masterwork", "Museum Quality", "Perfection's Shadow",
                "Near Perfect", "Pixel Master", "Sub-Atomic", "Transcendent"};
        String[] fitIcons = {"\uD83C\uDF31", "\uD83C\uDF08", "\u270F\uFE0F", "\u2702\uFE0F",
                "\uD83D\uDD8C\uFE0F", "\uD83D\uDC41\uFE0F", "\uD83D\uDCAA", "\uD83C\uDFA8",
                "\uD83C\uDFC6", "\u2B50", "\uD83D\uDD25", "\uD83C\uDFAF",
                "\uD83D\uDE80", "\uD83D\uDC8E", "\uD83C\uDFDB\uFE0F", "\uD83D\uDC51",
                "\u2728", "\uD83E\uDDEC", "\u269B\uFE0F", "\uD83C\uDF1F"};
        for (int i = 0; i < fitTiers.length; i++) {
            double reward = 10 * Math.pow(2, i);
            list.add(new AchievementDef("fit_" + i, fitNames[i], fitIcons[i],
                    "Reach " + fitTiers[i] + "% fitness", "fitness", fitTiers[i], reward, 1 + i * 0.01, false));
        }

        // EP milestones
        double[] epTiers = {10, 100, 1000, 5000, 10000, 50000, 100000, 500000,
                1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12};
        String[] epNames = {"Pocket Change", "Piggy Bank", "First Thousand", "Stacked",
                "Ten Grand", "Fortune", "Hundred K", "Half Million",
                "Millionaire", "Tycoon", "Magnate", "Billionaire",
                "Trillionaire", "Galactic Wealth", "Universal Riches"};
        for (int i = 0; i < epTiers.length; i++) {
            list.add(new AchievementDef("ep_" + i, epNames[i], "\uD83D\uDCB0",
                    "Earn " + formatBigNumber(epTiers[i]) + " total EP", "totalEp", epTiers[i],
                    epTiers[i] * 0.1, 1 + i * 0.005, false));
        }

        // Click milestones
        long[] clickTiers = {1, 10, 50, 100, 500, 1000, 5000, 10000, 50000, 100000, 500000, 1000000};
        String[] clickNames = {"Curious", "Interested", "Engaged", "Dedicated",
                "Obsessed", "Thousand Clicks", "Click Master", "Ten Thousand Strong",
                "Carpal Tunnel", "Hundred Thousand", "Half Million Clicks", "The Millionth Click"};
        for (int i = 0; i < clickTiers.length; i++) {
            list.add(new AchievementDef("click_" + i, clickNames[i], "\uD83D\uDC46",
                    "Click " + formatBigNumber(clickTiers[i]) + " times", "clicks", clickTiers[i],
                    clickTiers[i] * 0.5, 1 + i * 0.003, false));
        }

        // Time milestones (seconds)
        long[] timeTiers = {60, 300, 900, 1800, 3600, 10800, 21600, 43200, 86400, 604800};
        String[] timeNames = {"Minute Man", "Five Minutes", "Quarter Hour", "Patient Soul",
                "Hour of Power", "Three Hours", "Half Day", "Dawn to Dusk",
                "Full Day", "Week Warrior"};
        for (int i = 0; i < timeTiers.length; i++) {
            list.add(new AchievementDef("time_" + i, timeNames[i], "\u23F0",
                    "Play for " + formatDuration(timeTiers[i]), "playTime", timeTiers[i],
                    timeTiers[i], 1 + i * 0.005, false));
        }

        // Generation milestones
        int[] genTiers = {1, 5, 10, 25, 50, 100, 250, 500, 1000, 10000};
        String[] genNames = {"First Generation", "Fifth Wave", "Decade", "Quarter Century",
                "Golden Jubilee", "Centennial", "Quarter Millennium", "Half Millennium",
                "Millennium", "Ten Thousand Generations"};
        for (int i = 0; i < genTiers.length; i++) {
            list.add(new AchievementDef("gen_" + i, genNames[i], "\uD83E\uDDEC",
                    "Reach generation " + genTiers[i], "generation", genTiers[i],
                    genTiers[i] * 5, 1 + i * 0.008, false));
        }

        // Upgrade milestones
        int[] upgTiers = {1, 5, 10, 25, 50, 100, 200, 500};
        String[] upgNames = {"First Purchase", "Getting Started", "Investor", "Tinkerer",
                "Engineer", "Hundred Upgrades", "Mad Scientist", "Upgrade Singularity"};
        for (int i = 0; i < upgTiers.length; i++) {
            list.add(new AchievementDef("upg_" + i, upgNames[i], "\u2B06\uFE0F",
                    "Buy " + upgTiers[i] + " total upgrades", "upgrades", upgTiers[i],
                    upgTiers[i] * 10, 1 + i * 0.01, false));
        }

        // Prestige milestones
        int[] presTiers = {1, 3, 5, 10, 25, 50, 100};
        String[] presNames = {"First Ascension", "Triple Ascended", "Quintuple",
                "Decade of Ascensions", "Silver Ascension", "Golden Ascension", "Centennial Ascension"};
        for (int i = 0; i < presTiers.length; i++) {
            list.add(new AchievementDef("pres_" + i, presNames[i], "\uD83C\uDF1F",
                    "Ascend " + presTiers[i] + " times", "ascensions", presTiers[i],
                    presTiers[i] * 100, 1 + i * 0.02, false));
        }

        // Speed milestones (iterations/sec)
        long[] speedTiers = {100, 1000, 5000, 10000, 50000, 100000, 500000, 1000000, 5000000, 10000000};
        String[] speedNames = {"Crawling", "Walking", "Running", "Sprinting",
                "Driving", "Flying", "Supersonic", "Light Speed",
                "Warp Drive", "Ludicrous Speed"};
        for (int i = 0; i < speedTiers.length; i++) {
            list.add(new AchievementDef("speed_" + i, speedNames[i], "\u26A1",
                    "Reach " + formatBigNumber(speedTiers[i]) + " iterations/sec", "iterPerSec", speedTiers[i],
                    speedTiers[i] * 0.01, 1 + i * 0.005, false));
        }

        // Discovery achievements (hidden until unlocked)
        list.add(new AchievementDef("disc_delta", "Delta Discovery", "\uD83D\uDD2C",
                "First time using Delta Evolution", "discovery", 1, 500, 1.05, true));
        list.add(new AchievementDef("disc_crossover", "Crossover Breakthrough", "\u2702\uFE0F",
                "First time enabling crossover", "discovery", 2, 300, 1.03, true));
        list.add(new AchievementDef("disc_tournament", "Tournament Begun", "\u2694\uFE0F",
                "Start your first tournament", "discovery", 3, 1000, 1.08, true));
        list.add(new AchievementDef("disc_elimination", "First Blood", "\uD83D\uDC80",
                "First contestant eliminated", "discovery", 4, 200, 1.02, true));
        list.add(new AchievementDef("disc_prestige", "Ascended", "\uD83C\uDF1F",
                "Perform your first ascension", "discovery", 5, 5000, 1.15, true));
        list.add(new AchievementDef("disc_golden", "Golden Touch", "\u2B50",
                "Trigger a Golden Mutation event", "discovery", 6, 2000, 1.10, true));
        list.add(new AchievementDef("disc_prehistoric", "Genesis", "\uD83C\uDF0B",
                "Complete Prehistoric Mode through all eras", "discovery", 7, 3000, 1.12, true));
        list.add(new AchievementDef("disc_autopilot", "Hands Off", "\u2699\uFE0F",
                "Enable Autopilot mode", "discovery", 8, 500, 1.05, true));
        list.add(new AchievementDef("disc_99", "The Last Percent", "\uD83D\uDCA5",
                "The final 1% is harder than the first 99%", "discovery", 9, 50000, 1.25, true));
        list.add(new AchievementDef("disc_allcategories", "Renaissance", "\uD83C\uDF10",
                "Buy at least one upgrade from every category", "discovery", 10, 10000, 1.15, true));

        return list.toArray(new AchievementDef[0]);
    }

    // ════════════════════════════════════════════════════════════════
    //  EVENT DEFINITIONS
    // ════════════════════════════════════════════════════════════════

    public static final EventDef[] EVENTS = {
        new EventDef("golden_mutation", "Golden Mutation", "\u2B50",
                "All mutations 10x more effective!", 30, 2.0, 0.003),
        new EventDef("fitness_surge", "Fitness Surge", "\uD83D\uDE80",
                "Guaranteed fitness improvements!", 45, 3.0, 0.002),
        new EventDef("ep_rain", "EP Rain", "\uD83D\uDCB0",
                "+500% EP production!", 30, 5.0, 0.004),
        new EventDef("population_boom", "Population Boom", "\uD83D\uDC76",
                "Free +1 to all populations!", 60, 1.5, 0.002),
        new EventDef("algorithm_insight", "Algorithm Insight", "\uD83D\uDCA1",
                "All algorithms boosted!", 45, 2.5, 0.0015),
        new EventDef("cosmic_alignment", "Cosmic Alignment", "\u2728",
                "Everything boosted 2x!", 120, 2.0, 0.001),
        new EventDef("time_dilation", "Time Dilation", "\u23F3",
                "Evolution speed doubled!", 60, 2.0, 0.002),
        new EventDef("mutation_frenzy", "Mutation Frenzy", "\uD83E\uDDA0",
                "Triple mutation rate!", 30, 3.0, 0.003),
    };

    // ════════════════════════════════════════════════════════════════
    //  GAME TICK (called every second)
    // ════════════════════════════════════════════════════════════════

    /**
     * Main game tick. Call every ~1 second.
     * Returns a list of events that occurred (for UI notifications).
     */
    public List<String> tick(double currentFitness, long currentIterPerSec, int currentGeneration) {
        List<String> notifications = new ArrayList<>();
        long now = System.currentTimeMillis();
        totalPlayTimeMs = now - gameStartMs;

        // Update stats
        if (currentFitness > highestFitness) {
            double gain = currentFitness - highestFitness;
            double bonusEp = gain * 10000 * getMultiplier("epMultiplier") * getPrestigeMultiplier();
            addEp(bonusEp);
            highestFitness = currentFitness;
        }
        if (currentIterPerSec > highestIterPerSec) {
            highestIterPerSec = currentIterPerSec;
        }

        // Base EP production
        computeEpPerSecond();
        addEp(epPerSecond);

        // Auto-clicks
        double autoClicks = getEffectValue("autoClickRate");
        if (autoClicks > 0) {
            addEp(epPerClick * autoClicks);
            totalClicks += (long) autoClicks;
        }

        // Event tick
        tickEvents(notifications);

        // Random event spawning
        if (now - lastEventCheckMs > 5000) {
            lastEventCheckMs = now;
            double luckBonus = 1.0 + getEffectValue("eventLuckBonus") + getEffectValue("eternalLuck");
            for (EventDef def : EVENTS) {
                if (Math.random() < def.baseChance * luckBonus) {
                    double strength = def.baseStrength * (1 + getEffectValue("eventLuckBonus"));
                    activeEvents.add(new ActiveEvent(def.id, def.name, def.icon,
                            def.durationSeconds, strength, now));
                    notifications.add("EVENT:" + def.icon + " " + def.name + " — " + def.description);
                }
            }
        }

        // Check achievements
        checkAchievements(currentFitness, currentIterPerSec, currentGeneration, notifications);

        return notifications;
    }

    /** Manual click action. Returns EP earned from this click. */
    public double click() {
        totalClicks++;
        double earned = epPerClick * getMultiplier("epMultiplier") * getPrestigeMultiplier();
        // Critical click chance
        double critChance = getEffectValue("criticalHitChance");
        if (critChance > 0 && Math.random() < critChance) {
            earned *= 10;
        }
        addEp(earned);
        return earned;
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
        double cost = getCost(upgradeId);
        return getCurrency(def.currency) >= cost;
    }

    /**
     * Attempts to purchase an upgrade. Returns true if successful.
     */
    public boolean buyUpgrade(String upgradeId) {
        if (!canAfford(upgradeId)) return false;
        UpgradeDef def = findUpgrade(upgradeId);
        double cost = getCost(upgradeId);
        spendCurrency(def.currency, cost);
        upgradeLevels.merge(upgradeId, 1, Integer::sum);
        totalUpgradesBought++;
        computeEpPerSecond();
        return true;
    }

    /** Buy max affordable levels of an upgrade. Returns number bought. */
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

    /** Genome Fragments earned if ascending now. */
    public double calcPrestigeReward() {
        double base = Math.sqrt(totalEpEarned / 1000.0);
        double multiplier = 1.0 + getEffectValue("prestigeMultiplier");
        return Math.floor(base * multiplier);
    }

    /**
     * Perform ascension. Resets EP, MC, upgrades (except prestige tier).
     * Awards Genome Fragments.
     */
    public boolean ascend() {
        double reward = calcPrestigeReward();
        if (reward < 1) return false;

        gf += reward;
        ascensionCount++;
        ep = 0;
        mc = 0;
        highestFitness = 0;

        // Reset non-prestige upgrade levels
        upgradeLevels.entrySet().removeIf(e -> {
            UpgradeDef def = findUpgrade(e.getKey());
            return def != null && !def.currency.equals("gf");
        });

        totalUpgradesBought = (int) upgradeLevels.values().stream().mapToInt(Integer::intValue).sum();
        computeEpPerSecond();
        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  COMPUTED VALUES
    // ════════════════════════════════════════════════════════════════

    private void computeEpPerSecond() {
        double base = 0.1; // Passive drip
        // Each upgrade contributes some passive EP
        for (UpgradeDef def : UPGRADES) {
            int level = getLevel(def.id);
            if (level > 0 && !def.currency.equals("gf")) {
                base += level * 0.1 * (1 + def.baseCost / 100.0);
            }
        }
        base *= getMultiplier("epMultiplier");
        base *= getPrestigeMultiplier();
        base *= getEventMultiplier("ep_rain");
        base *= getMultiplier("eternalEp");
        epPerSecond = base;

        // EP per click scales with upgrades
        epPerClick = 1.0 + totalUpgradesBought * 0.1;
        epPerClick *= getMultiplier("epMultiplier");
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

    private double getMultiplier(String effectId) {
        double total = 1.0;
        for (UpgradeDef def : UPGRADES) {
            if (def.effectTarget.equals(effectId) && def.effectType.equals("mult")) {
                total += getLevel(def.id) * def.effectPerLevel;
            }
        }
        // Achievement multipliers
        for (AchievementDef ad : ACHIEVEMENTS) {
            if (unlockedAchievements.contains(ad.id)) {
                total *= ad.multiplier;
            }
        }
        return total;
    }

    public double getPrestigeMultiplier() {
        return 1.0 + gf * 0.01 * (1 + getEffectValue("eternalEp"));
    }

    private double getEventMultiplier(String eventId) {
        double mult = 1.0;
        for (ActiveEvent ev : activeEvents) {
            if (ev.eventId.equals(eventId)) {
                mult *= ev.strength;
            }
        }
        return mult;
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

    private void checkAchievements(double fitness, long iterPerSec, int generation, List<String> notifications) {
        for (AchievementDef ad : ACHIEVEMENTS) {
            if (unlockedAchievements.contains(ad.id)) continue;

            boolean unlocked = false;
            switch (ad.conditionType) {
                case "fitness" -> unlocked = fitness * 100 >= ad.threshold;
                case "totalEp" -> unlocked = totalEpEarned >= ad.threshold;
                case "clicks" -> unlocked = totalClicks >= ad.threshold;
                case "playTime" -> unlocked = (totalPlayTimeMs / 1000) >= ad.threshold;
                case "generation" -> unlocked = generation >= ad.threshold;
                case "upgrades" -> unlocked = totalUpgradesBought >= ad.threshold;
                case "ascensions" -> unlocked = ascensionCount >= ad.threshold;
                case "iterPerSec" -> unlocked = iterPerSec >= ad.threshold;
            }

            if (unlocked) {
                unlockedAchievements.add(ad.id);
                achievementQueue.add(ad.id);
                totalAchievementsUnlocked++;
                addEp(ad.epReward);
                notifications.add("ACHIEVEMENT:" + ad.icon + " " + ad.name + " — " + ad.description);
            }
        }
    }

    /** Unlock a discovery achievement by number. */
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

    /** Pop achievement queue for UI display. */
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

    public void addMc(double amount) { mc += amount; }

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
        sb.append("  \"highestFitness\":").append(highestFitness).append(",\n");
        sb.append("  \"highestIterPerSec\":").append(highestIterPerSec).append(",\n");
        sb.append("  \"ascensionCount\":").append(ascensionCount).append(",\n");
        sb.append("  \"achievementsUnlocked\":").append(totalAchievementsUnlocked).append(",\n");
        sb.append("  \"achievementsTotal\":").append(ACHIEVEMENTS.length).append(",\n");
        sb.append("  \"epPerSecond\":").append(epPerSecond).append(",\n");
        sb.append("  \"epPerClick\":").append(epPerClick).append(",\n");
        sb.append("  \"prestigeReward\":").append(calcPrestigeReward()).append(",\n");
        sb.append("  \"prestigeMultiplier\":").append(getPrestigeMultiplier()).append(",\n");

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
    public double getHighestFitness() { return highestFitness; }
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
