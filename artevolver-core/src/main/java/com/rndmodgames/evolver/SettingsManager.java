package com.rndmodgames.evolver;

import java.util.prefs.Preferences;

/**
 * Persists user-configurable parameters across sessions using java.util.prefs.Preferences.
 * All methods are static — there is one shared preference node for the application.
 *
 * On Windows this uses the registry; on Linux/Mac it uses ~/.java/.prefs/.
 * Falls back gracefully to hardcoded defaults if no saved prefs exist.
 */
public class SettingsManager {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(SettingsManager.class);

    private SettingsManager() {}

    // ═══════════════════════════════════════════════════════════════
    //  SIDEBAR PARAMETERS
    // ═══════════════════════════════════════════════════════════════

    public static void saveInt(String key, int value) { PREFS.putInt(key, value); }
    public static int loadInt(String key, int defaultValue) { return PREFS.getInt(key, defaultValue); }

    public static void saveDouble(String key, double value) { PREFS.putDouble(key, value); }
    public static double loadDouble(String key, double defaultValue) { return PREFS.getDouble(key, defaultValue); }

    public static void saveFloat(String key, float value) { PREFS.putFloat(key, value); }
    public static float loadFloat(String key, float defaultValue) { return PREFS.getFloat(key, defaultValue); }

    public static void saveBoolean(String key, boolean value) { PREFS.putBoolean(key, value); }
    public static boolean loadBoolean(String key, boolean defaultValue) { return PREFS.getBoolean(key, defaultValue); }

    public static void saveString(String key, String value) {
        if (value != null) PREFS.put(key, value);
        else PREFS.remove(key);
    }
    public static String loadString(String key, String defaultValue) { return PREFS.get(key, defaultValue); }

    // ═══════════════════════════════════════════════════════════════
    //  WELL-KNOWN KEYS
    // ═══════════════════════════════════════════════════════════════

    // Sidebar - Quality
    public static final String KEY_GRID_WIDTH = "gridWidth";
    public static final String KEY_GRID_HEIGHT = "gridHeight";
    public static final String KEY_PALETTES = "paletteRepetitions";
    public static final String KEY_INIT_METHOD = "initMethod";
    public static final String KEY_EVOLVE_METHOD = "evolveMethod";

    // Sidebar - GA
    public static final String KEY_POPULATION = "population";
    public static final String KEY_CROSSOVER_MAX = "crossoverMax";
    public static final String KEY_EVOLVE_ITERATIONS = "evolveIterations";
    public static final String KEY_MAX_ITERATIONS = "maxIterationsK";
    public static final String KEY_GRID_MUTATIONS = "gridMutations";
    public static final String KEY_GRID_MUTATION_DECAY = "gridMutationDecay";
    public static final String KEY_GRID_MUTATION_PCT = "gridMutationPct";
    public static final String KEY_RANDOM_MUTATIONS = "randomMutations";
    public static final String KEY_RANDOM_MUTATION_PCT = "randomMutationPct";
    public static final String KEY_CLOSE_MUTATIONS = "closeMutations";
    public static final String KEY_CLOSE_MUTATION_PCT = "closeMutationPct";
    public static final String KEY_TARGETED_SWAPS = "targetedSwaps";
    public static final String KEY_BLOCK_CROSSOVER = "blockCrossover";

    // Sidebar - Speed
    public static final String KEY_THREADS = "threads";
    public static final String KEY_EVOLUTION_FPS = "evolutionFps";
    public static final String KEY_GUI_FPS = "guiFps";
    public static final String KEY_VALIDATE_PERMUTATION = "validatePermutation";
    public static final String KEY_BENCHMARK_LOGGING = "benchmarkLogging";
    public static final String KEY_EXPORT_VIDEO = "exportVideo";

    // Display
    public static final String KEY_DRAW_MODE = "drawMode";
    public static final String KEY_HIDE_ELIMINATED = "hideEliminatedInGrid";

    // File picker
    public static final String KEY_LAST_IMAGE_DIR = "lastImageDir";

    // Window positions
    public static final String KEY_MAIN_X = "mainWindowX";
    public static final String KEY_MAIN_Y = "mainWindowY";
    public static final String KEY_MAIN_W = "mainWindowW";
    public static final String KEY_MAIN_H = "mainWindowH";
    public static final String KEY_TMW_X = "tournamentWindowX";
    public static final String KEY_TMW_Y = "tournamentWindowY";
    public static final String KEY_TMW_W = "tournamentWindowW";
    public static final String KEY_TMW_H = "tournamentWindowH";
    public static final String KEY_CHART_X = "chartWindowX";
    public static final String KEY_CHART_Y = "chartWindowY";
    public static final String KEY_CHART_W = "chartWindowW";
    public static final String KEY_CHART_H = "chartWindowH";

    // Evo Tournament settings
    public static final String KEY_EVO_CUTOFF = "evoCutoffSeconds";
    public static final String KEY_EVO_GRACE = "evoGraceTicks";
    public static final String KEY_EVO_MIN_CONTESTANTS = "evoMinContestants";
    public static final String KEY_EVO_SPAWNS = "evoSpawnsPerTick";
    public static final String KEY_EVO_ADAPTIVE_CUTOFF = "evoAdaptiveCutoff";
    public static final String KEY_EVO_ADAPTIVE_MIN = "evoAdaptiveMin";
    public static final String KEY_EVO_ADAPTIVE_MAX = "evoAdaptiveMax";
    public static final String KEY_EVO_MAX_LIFESPAN = "evoMaxLifespan";
    public static final String KEY_EVO_STALE_THRESHOLD = "evoStaleThreshold";
    public static final String KEY_EVO_MAX_PROMOTED = "evoMaxPromoted";
    public static final String KEY_EVO_PRESET_INTERVAL = "evoPresetInterval";
    public static final String KEY_EVO_RANKING_STRATEGY = "evoRankingStrategy";
    public static final String KEY_EVO_AUTO_TRANSITION = "evoAutoTransition";
    public static final String KEY_EVO_MUTATION_RATE = "evoMutationRate";
    public static final String KEY_EVO_MUTATION_STRENGTH = "evoMutationStrength";
    public static final String KEY_EVO_ANCESTRAL_CROSSOVER = "evoAncestralCrossover";
    public static final String KEY_EVO_ANCESTRY_DEPTH = "evoAncestryDepth";
    public static final String KEY_EVO_W_FITNESS = "evoWeightFitness";
    public static final String KEY_EVO_W_VELOCITY = "evoWeightVelocity";
    public static final String KEY_EVO_W_ACCELERATION = "evoWeightAcceleration";
    public static final String KEY_EVO_W_LINEAGE = "evoWeightLineage";
    public static final String KEY_EVO_LINEAGE_DECAY = "evoLineageDecay";
    public static final String KEY_EVO_VELOCITY_WINDOW = "evoVelocityWindow";
    public static final String KEY_EVO_ADAPTIVE_LIFETIME = "evoAdaptiveLifetime";
    public static final String KEY_EVO_ADAPTIVE_LIFETIME_MODE = "evoAdaptiveLifetimeMode";
    public static final String KEY_EVO_GROWTH_CAP = "evoGrowthCap";
    public static final String KEY_EVO_ANOMALY_THRESHOLD = "evoAnomalyThreshold";
    public static final String KEY_EVO_ABSOLUTE_MAX_LIFESPAN = "evoAbsoluteMaxLifespan";

    // Autopilot
    public static final String KEY_AUTOPILOT_CPU = "autopilotMaxCpu";
    public static final String KEY_AUTOPILOT_RAM = "autopilotMaxRam";
    public static final String KEY_AUTOPILOT_HEAP = "autopilotMaxHeap";

    // Dashboard
    public static final String KEY_AUTO_OPEN_DASHBOARD = "autoOpenDashboard";

    // ═══════════════════════════════════════════════════════════════
    //  RESET
    // ═══════════════════════════════════════════════════════════════

    public static void resetAll() {
        try {
            PREFS.clear();
            PREFS.flush();
            System.out.println("[Settings] All preferences cleared — defaults will be used on next launch.");
        } catch (Exception e) {
            System.err.println("[Settings] Failed to clear preferences: " + e.getMessage());
        }
    }

    public static boolean hasKey(String key) {
        return PREFS.get(key, null) != null;
    }

    public static void flush() {
        try {
            PREFS.flush();
        } catch (Exception e) {
            System.err.println("[Settings] Failed to flush preferences: " + e.getMessage());
        }
    }
}
