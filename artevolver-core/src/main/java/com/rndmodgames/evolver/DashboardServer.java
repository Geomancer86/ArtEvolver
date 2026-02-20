package com.rndmodgames.evolver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import com.rndmodgames.evolver.clicker.ClickerState;

/**
 * Embedded HTTP server that serves a real-time leaderboard dashboard.
 * Uses Java's built-in com.sun.net.httpserver — zero external dependencies.
 *
 * Endpoints:
 *   GET /            → dashboard HTML page
 *   GET /api/state   → JSON with full tournament state
 *   GET /api/image/{id}        → current best PNG thumbnail
 *   GET /api/export/{id}       → high-quality PNG for eliminated contestants
 */
public class DashboardServer {

    private static final DecimalFormat DF2 = new DecimalFormat("0.00");
    private static final DecimalFormat DF4 = new DecimalFormat("0.0000");

    private HttpServer server;
    private int port;
    private final ArtEvolver artEvolver;
    private final List<TournamentContestant> contestants;
    private final TournamentManagerWindow managerWindow;
    private final ClickerState clickerState = new ClickerState();

    public DashboardServer(ArtEvolver artEvolver,
                           List<TournamentContestant> contestants,
                           TournamentManagerWindow managerWindow) {
        this.artEvolver = artEvolver;
        this.contestants = contestants;
        this.managerWindow = managerWindow;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();

        server.createContext("/", this::handleDashboard);
        server.createContext("/clicker", this::handleClicker);
        server.createContext("/api/state", this::handleState);
        server.createContext("/api/clicker/state", this::handleClickerState);
        server.createContext("/api/clicker/click", this::handleClickerClick);
        server.createContext("/api/clicker/buy", this::handleClickerBuy);
        server.createContext("/api/clicker/prestige", this::handleClickerPrestige);
        server.createContext("/api/image/", this::handleImage);
        server.createContext("/api/export/", this::handleExport);

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("[Dashboard] Server started on http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            System.out.println("[Dashboard] Server stopped");
        }
    }

    public int getPort() { return port; }
    public String getUrl() { return "http://localhost:" + port; }

    public void openInBrowser() {
        try {
            Desktop.getDesktop().browse(java.net.URI.create(getUrl()));
        } catch (Exception e) {
            System.err.println("[Dashboard] Could not open browser: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HANDLERS
    // ═══════════════════════════════════════════════════════════════

    private void handleDashboard(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { sendError(ex, 405); return; }

        try (InputStream is = getClass().getResourceAsStream("/dashboard.html")) {
            byte[] html;
            if (is != null) {
                html = is.readAllBytes();
            } else {
                html = getFallbackHtml().getBytes(StandardCharsets.UTF_8);
            }
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            ex.sendResponseHeaders(200, html.length);
            ex.getResponseBody().write(html);
        }
        ex.close();
    }

    private void handleState(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { sendError(ex, 405); return; }

        String json = buildStateJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private void handleImage(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { sendError(ex, 405); return; }

        String path = ex.getRequestURI().getPath();
        String id = path.substring("/api/image/".length());
        TournamentContestant tc = findContestant(id);
        if (tc == null) { sendError(ex, 404); return; }

        BufferedImage img = tc.getBestImage();
        if (img == null) { sendError(ex, 204); return; }

        // Scale to thumbnail (max 300px wide)
        int thumbW = Math.min(300, img.getWidth());
        int thumbH = (int) ((double) img.getHeight() / img.getWidth() * thumbW);
        BufferedImage thumb = new BufferedImage(thumbW, thumbH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, thumbW, thumbH, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(thumb, "png", baos);
        byte[] data = baos.toByteArray();

        ex.getResponseHeaders().set("Content-Type", "image/png");
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private void handleExport(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { sendError(ex, 405); return; }

        String path = ex.getRequestURI().getPath();
        String id = path.substring("/api/export/".length());
        TournamentContestant tc = findContestant(id);
        if (tc == null) { sendError(ex, 404); return; }

        BufferedImage img = tc.getBestImage();
        if (img == null) { sendError(ex, 204); return; }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] data = baos.toByteArray();

        String status = tc.isPromoted() ? "_promoted" : tc.isEliminated() ? "_eliminated" : "_best";
        String filename = tc.getName().replaceAll("[^a-zA-Z0-9_-]", "_") + status + ".png";
        ex.getResponseHeaders().set("Content-Type", "image/png");
        ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    // ═══════════════════════════════════════════════════════════════
    //  CLICKER HANDLERS
    // ═══════════════════════════════════════════════════════════════

    private void handleClicker(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { sendError(ex, 405); return; }
        try (InputStream is = getClass().getResourceAsStream("/clicker.html")) {
            byte[] html;
            if (is != null) {
                html = is.readAllBytes();
            } else {
                html = "<html><body><h1>clicker.html not found</h1></body></html>".getBytes(StandardCharsets.UTF_8);
            }
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            ex.sendResponseHeaders(200, html.length);
            ex.getResponseBody().write(html);
        }
        ex.close();
    }

    private void handleClickerState(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { sendError(ex, 405); return; }

        // Tick the game with real evolution data
        double fitness = 0;
        long iterPerSec = 0;
        int generation = 0;
        for (TournamentContestant c : contestants) {
            if (!c.isFinished()) {
                fitness = Math.max(fitness, c.getBestScore());
                long elapsed = System.currentTimeMillis() - c.getStartTimeMs();
                if (elapsed > 0) iterPerSec += c.getTotalIterations() * 1000 / elapsed;
            }
        }
        EvolutionaryTournament evo = managerWindow.getEvoTournament();
        if (evo != null) generation = evo.getGeneration();

        clickerState.tick(fitness, iterPerSec, generation);

        String json = clickerState.toJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private void handleClickerClick(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST") && !ex.getRequestMethod().equals("GET")) {
            sendError(ex, 405); return;
        }
        double earned = clickerState.click();
        String json = "{\"earned\":" + earned + ",\"ep\":" + clickerState.getEp() + "}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private void handleClickerBuy(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getQuery();
        String id = null;
        boolean max = false;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv[0].equals("id") && kv.length > 1) id = kv[1];
                if (kv[0].equals("max")) max = true;
            }
        }
        if (id == null) { sendError(ex, 400); return; }

        String json;
        if (max) {
            int count = clickerState.buyMax(id);
            json = "{\"bought\":" + count + ",\"ep\":" + clickerState.getEp() + "}";
        } else {
            boolean success = clickerState.buyUpgrade(id);
            json = "{\"success\":" + success + ",\"ep\":" + clickerState.getEp() + "}";
        }
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private void handleClickerPrestige(HttpExchange ex) throws IOException {
        boolean success = clickerState.ascend();
        String json = "{\"success\":" + success + ",\"gf\":" + clickerState.getGf()
                + ",\"ascensions\":" + clickerState.getAscensionCount() + "}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    public ClickerState getClickerState() { return clickerState; }

    // ═══════════════════════════════════════════════════════════════
    //  JSON BUILDER
    // ═══════════════════════════════════════════════════════════════

    private String buildStateJson() {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("{\n");

        // System monitor
        SystemMonitor mon = managerWindow.getSystemMonitor();
        mon.poll();
        sb.append("  \"system\": {\n");
        sb.append("    \"cpuProcess\": ").append(DF2.format(mon.getCpuUsagePercent())).append(",\n");
        sb.append("    \"cpuSystem\": ").append(DF2.format(mon.getSystemCpuPercent())).append(",\n");
        sb.append("    \"heapUsedMB\": ").append(mon.getHeapUsedMB()).append(",\n");
        sb.append("    \"heapMaxMB\": ").append(mon.getHeapMaxMB()).append(",\n");
        sb.append("    \"heapPercent\": ").append(DF2.format(mon.getHeapUsagePercent())).append(",\n");
        sb.append("    \"ramPercent\": ").append(DF2.format(mon.getRamUsagePercent())).append(",\n");
        sb.append("    \"ramTotalMB\": ").append(mon.getTotalPhysicalMemMB()).append(",\n");
        sb.append("    \"diskPercent\": ").append(DF2.format(mon.getDiskUsagePercent())).append(",\n");
        sb.append("    \"diskFreeMB\": ").append(mon.getDiskFreeMB()).append(",\n");
        sb.append("    \"threads\": ").append(mon.getThreadCount()).append(",\n");
        sb.append("    \"peakThreads\": ").append(mon.getPeakThreadCount()).append(",\n");
        sb.append("    \"cores\": ").append(mon.getAvailableProcessors()).append("\n");
        sb.append("  },\n");

        // Tournament state
        EvolutionaryTournament evo = managerWindow.getEvoTournament();
        sb.append("  \"tournament\": {\n");
        if (evo != null) {
            sb.append("    \"active\": true,\n");
            sb.append("    \"generation\": ").append(evo.getGeneration()).append(",\n");
            sb.append("    \"secondsUntilNext\": ").append(evo.getSecondsUntilNextTick()).append(",\n");
            sb.append("    \"cutoffSeconds\": ").append(evo.getCutoffSeconds()).append(",\n");
            sb.append("    \"adaptiveCutoff\": ").append(evo.isAdaptiveCutoff()).append(",\n");
            sb.append("    \"spawnsPerTick\": ").append(evo.getSpawnsPerTick()).append(",\n");
            sb.append("    \"bestEverName\": ").append(jsonStr(evo.getBestEverName())).append(",\n");
            sb.append("    \"bestEverScore\": ").append(DF4.format(evo.getBestEverScore() * 100)).append("\n");
        } else {
            sb.append("    \"active\": false\n");
        }
        sb.append("  },\n");

        // Prehistoric mode
        PrehistoricMode pm = managerWindow.getPrehistoricMode();
        sb.append("  \"prehistoric\": {\n");
        if (pm != null && pm.isActive()) {
            sb.append("    \"active\": true,\n");
            sb.append("    \"era\": ").append(pm.getCurrentEra()).append(",\n");
            sb.append("    \"eraName\": ").append(jsonStr(pm.getCurrentEraName())).append(",\n");
            sb.append("    \"autoAdvance\": ").append(pm.isAutoAdvance()).append("\n");
        } else {
            sb.append("    \"active\": false\n");
        }
        sb.append("  },\n");

        // Autopilot
        sb.append("  \"autopilot\": ").append(managerWindow.isAutopilotActive()).append(",\n");

        // Timestamp
        sb.append("  \"timestamp\": ").append(System.currentTimeMillis()).append(",\n");

        // Contestants
        sb.append("  \"contestants\": [\n");
        List<TournamentContestant> sorted = new ArrayList<>(contestants);
        sorted.sort((a, b) -> {
            int statusA = a.isEliminated() ? 2 : a.isPromoted() ? 1 : 0;
            int statusB = b.isEliminated() ? 2 : b.isPromoted() ? 1 : 0;
            if (statusA != statusB) return statusA - statusB;
            double sa = a.isFinished() ? a.getFinalScore() : a.getBestScore();
            double sb2 = b.isFinished() ? b.getFinalScore() : b.getBestScore();
            return Double.compare(sb2, sa);
        });

        for (int i = 0; i < sorted.size(); i++) {
            TournamentContestant c = sorted.get(i);
            EvolutionConfig cfg = c.getConfig();
            FitnessTracker ft = c.getFitnessTracker();
            Color col = c.getChartColor();

            double score = c.isFinished() ? c.getFinalScore() : c.getBestScore();
            double velocity = ft != null ? ft.getVelocity() : 0;
            double acceleration = ft != null ? ft.getAcceleration() : 0;
            double peakFitness = ft != null ? ft.getPeakFitness() : 0;

            sb.append("    {\n");
            sb.append("      \"id\": ").append(jsonStr(c.getId())).append(",\n");
            sb.append("      \"name\": ").append(jsonStr(c.getName())).append(",\n");
            sb.append("      \"rank\": ").append(i + 1).append(",\n");
            sb.append("      \"score\": ").append(DF4.format(score * 100)).append(",\n");
            sb.append("      \"velocity\": ").append(DF4.format(velocity * 100)).append(",\n");
            sb.append("      \"acceleration\": ").append(DF4.format(acceleration * 100)).append(",\n");
            sb.append("      \"peakFitness\": ").append(DF4.format(peakFitness * 100)).append(",\n");
            sb.append("      \"totalIterations\": ").append(c.getTotalIterations()).append(",\n");
            sb.append("      \"goodIterations\": ").append(c.getGoodIterations()).append(",\n");
            sb.append("      \"eliminated\": ").append(c.isEliminated()).append(",\n");
            sb.append("      \"promoted\": ").append(c.isPromoted()).append(",\n");
            sb.append("      \"running\": ").append(c.isRunning()).append(",\n");
            sb.append("      \"protected\": ").append(c.isProtected()).append(",\n");
            sb.append("      \"graceTicks\": ").append(c.getGraceTicks()).append(",\n");
            sb.append("      \"generation\": ").append(c.getGeneration()).append(",\n");
            sb.append("      \"parentage\": ").append(jsonStr(c.getParentage())).append(",\n");
            sb.append("      \"breedType\": ").append(jsonStr(c.getBreedType())).append(",\n");
            sb.append("      \"eliminatedAtGen\": ").append(c.getEliminatedAtGeneration()).append(",\n");
            sb.append("      \"color\": \"").append(String.format("#%02x%02x%02x", col.getRed(), col.getGreen(), col.getBlue())).append("\",\n");
            sb.append("      \"hasImage\": ").append(c.getBestImage() != null).append(",\n");

            // Config summary
            if (cfg != null) {
                sb.append("      \"config\": {\n");
                sb.append("        \"threads\": ").append(cfg.threads).append(",\n");
                sb.append("        \"population\": ").append(cfg.population).append(",\n");
                sb.append("        \"deltaEvolution\": ").append(cfg.useDeltaEvolution).append(",\n");
                sb.append("        \"crossover\": ").append(cfg.blockCrossoverEnabled).append(",\n");
                String initName = cfg.initializationMethod == 0 ? "Random"
                        : cfg.initializationMethod == 1 ? "Smart Greedy" : "LAP Optimal";
                sb.append("        \"initMethod\": ").append(jsonStr(initName)).append(",\n");
                sb.append("        \"name\": ").append(jsonStr(cfg.name)).append("\n");
                sb.append("      },\n");
            }

            // Uptime
            long uptimeMs = c.isRunning() ? (System.currentTimeMillis() - c.getStartTimeMs()) : 0;
            sb.append("      \"uptimeSeconds\": ").append(uptimeMs / 1000).append("\n");

            sb.append("    }");
            if (i < sorted.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // History logs (narrative)
        sb.append("  \"history\": [\n");
        List<String> narratives = new ArrayList<>();
        if (pm != null && pm.isActive()) {
            for (PrehistoricMode.EraRecord rec : pm.getHistory()) {
                narratives.add(rec.toNarrative());
            }
        }
        if (evo != null) {
            for (EvolutionaryTournament.GenerationRecord rec : evo.getHistory()) {
                narratives.add(rec.toNarrative());
            }
        }
        for (int i = 0; i < narratives.size(); i++) {
            sb.append("    ").append(jsonStr(narratives.get(i)));
            if (i < narratives.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}");

        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private TournamentContestant findContestant(String id) {
        for (TournamentContestant c : contestants) {
            if (c.getId().equals(id)) return c;
        }
        return null;
    }

    private static String jsonStr(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    private static void sendError(HttpExchange ex, int code) throws IOException {
        ex.sendResponseHeaders(code, -1);
        ex.close();
    }

    private String getFallbackHtml() {
        return "<html><body><h1>Dashboard loading error</h1>"
                + "<p>Could not find dashboard.html resource. "
                + "Check that it exists in src/main/resources/</p></body></html>";
    }
}
