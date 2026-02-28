package com.rndmodgames.evolver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;

import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import com.rndmodgames.evolver.clicker.ClickerState;
import com.rndmodgames.evolver.clicker.SampleImageProvider;
import java.net.URISyntaxException;

/**
 * Embedded HTTP server that serves a real-time leaderboard dashboard.
 * Uses Java's built-in com.sun.net.httpserver — zero external dependencies.
 *
 * Endpoints:
 *   GET /            → dashboard HTML page
 *   GET /api/state   → JSON with full tournament state
 *   GET /api/image/{id}        → current best PNG thumbnail (cached + ETag)
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
    private final boolean standaloneMode;

    // Standalone mode: image uploaded via browser
    // Defaults match the full app's QUALITY_MODE_FULL_THREADS preset (720x468 canvas)
    private volatile BufferedImage uploadedImage;
    private volatile Palette uploadedPalette;
    private static final int DEFAULT_WIDTH_TRI = 80;
    private static final int DEFAULT_HEIGHT_TRI = 53;
    private static final float DEFAULT_TRI_WIDTH = 9.0f;
    private static final float DEFAULT_TRI_HEIGHT = 9.0f;
    private static final float DEFAULT_TRI_SCALE = 3.0f;
    private static final int DEFAULT_PALETTES = 4;

    private static final int THUMBNAIL_CACHE_MAX = 100;
    private final java.util.concurrent.ConcurrentHashMap<String, CachedThumbnail> thumbnailCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static class CachedThumbnail {
        final byte[] pngBytes;
        final double scoreAtGeneration;
        final String etag;

        CachedThumbnail(byte[] pngBytes, double score) {
            this.pngBytes = pngBytes;
            this.scoreAtGeneration = score;
            this.etag = Long.toHexString(Double.doubleToLongBits(score));
        }
    }

    public DashboardServer(ArtEvolver artEvolver,
                           List<TournamentContestant> contestants,
                           TournamentManagerWindow managerWindow) {
        this.artEvolver = artEvolver;
        this.contestants = contestants != null ? contestants : Collections.emptyList();
        this.managerWindow = managerWindow;
        this.standaloneMode = (artEvolver == null);
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();

        server.createContext("/", this::handleDashboard);
        server.createContext("/clicker", this::handleClicker);
        server.createContext("/api/state", this::handleState);
        server.createContext("/api/clicker/init", this::handleClickerInit);
        server.createContext("/api/clicker/state", this::handleClickerState);
        server.createContext("/api/clicker/click", this::handleClickerClick);
        server.createContext("/api/clicker/buy", this::handleClickerBuy);
        server.createContext("/api/clicker/prestige", this::handleClickerPrestige);
        server.createContext("/api/clicker/complete", this::handleClickerComplete);
        server.createContext("/api/clicker/gallery", this::handleClickerGallery);
        server.createContext("/api/clicker/samples", this::handleClickerSamples);
        server.createContext("/api/clicker/sample/", this::handleClickerSampleImage);
        server.createContext("/api/clicker/upload", this::handleClickerUpload);
        server.createContext("/api/clicker/my-images", this::handleClickerMyImages);
        server.createContext("/api/clicker/my-image/", this::handleClickerMyImage);
        server.createContext("/api/clicker/image", this::handleClickerImage);
        server.createContext("/api/clicker/reference", this::handleClickerReference);
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

        double currentScore = tc.isFinished() ? tc.getFinalScore() : tc.getBestScore();

        CachedThumbnail cached = thumbnailCache.get(id);
        if (cached != null && cached.scoreAtGeneration == currentScore) {
            String clientEtag = ex.getRequestHeaders().getFirst("If-None-Match");
            if (clientEtag != null && clientEtag.equals(cached.etag)) {
                ex.getResponseHeaders().set("ETag", cached.etag);
                ex.sendResponseHeaders(304, -1);
                ex.close();
                return;
            }
            ex.getResponseHeaders().set("Content-Type", "image/png");
            ex.getResponseHeaders().set("ETag", cached.etag);
            ex.getResponseHeaders().set("Cache-Control", "no-cache");
            ex.sendResponseHeaders(200, cached.pngBytes.length);
            ex.getResponseBody().write(cached.pngBytes);
            ex.close();
            return;
        }

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

        CachedThumbnail newEntry = new CachedThumbnail(data, currentScore);
        if (thumbnailCache.size() >= THUMBNAIL_CACHE_MAX) {
            thumbnailCache.keySet().stream().findFirst().ifPresent(thumbnailCache::remove);
        }
        thumbnailCache.put(id, newEntry);

        ex.getResponseHeaders().set("Content-Type", "image/png");
        ex.getResponseHeaders().set("ETag", newEntry.etag);
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

    private BufferedImage resolveSourceImage(String sampleId, HttpExchange ex) throws IOException {
        if (sampleId != null) {
            if (sampleId.startsWith("custom:")) {
                String fp = sampleId.substring(7);
                Path file = getCustomImagesDir().resolve(fp + ".png");
                if (!Files.exists(file)) {
                    sendJsonResponse(ex, "{\"initialized\":false,\"error\":\"Custom image not found.\"}");
                    return null;
                }
                return ImageIO.read(file.toFile());
            }
            var def = SampleImageProvider.getDef(sampleId);
            if (def == null || !clickerState.isSampleUnlocked(def)) {
                String msg = def == null ? "Unknown sample." : "Sample not unlocked yet.";
                sendJsonResponse(ex, "{\"initialized\":false,\"error\":\"" + msg + "\"}");
                return null;
            }
            return SampleImageProvider.generate(sampleId);
        }
        if (standaloneMode) {
            if (uploadedImage == null) {
                sendJsonResponse(ex, "{\"initialized\":false,\"error\":\"Drop or pick an image above to begin.\"}");
                return null;
            }
            return uploadedImage;
        }
        BufferedImage img = artEvolver.getResizedOriginal();
        if (img == null) {
            sendJsonResponse(ex, "{\"initialized\":false,\"error\":\"No source image loaded. Load an image in ArtEvolver first.\"}");
            return null;
        }
        return img;
    }

    private void handleClickerInit(HttpExchange ex) throws IOException {
        String sampleId = null;
        String presetId = null;
        String query = ex.getRequestURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && kv[0].equals("sample") && !kv[1].isEmpty()) {
                    sampleId = kv[1];
                }
                if (kv.length == 2 && kv[0].equals("preset") && !kv[1].isEmpty()) {
                    presetId = kv[1];
                }
            }
        }

        // Retro pixel mode: preset parameter routes to pixel engine
        if (presetId != null) {
            var preset = com.rndmodgames.evolver.clicker.RetroPreset.fromId(presetId);
            if (preset == null) {
                sendJsonResponse(ex, "{\"initialized\":false,\"error\":\"Unknown preset: " + presetId + "\"}");
                return;
            }
            BufferedImage sourceImage = resolveSourceImage(sampleId, ex);
            if (sourceImage == null) return;

            BufferedImage resized = new BufferedImage(preset.getWidth(), preset.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = resized.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(sourceImage, 0, 0, preset.getWidth(), preset.getHeight(), null);
            g2.dispose();

            String error = clickerState.initPixelMode(resized, preset, sampleId);
            if (error != null) {
                sendJsonResponse(ex, "{\"initialized\":false,\"error\":\"" + error.replace("\"", "\\\"") + "\"}");
                return;
            }
            var engine = clickerState.getEngine();
            String json = "{\"initialized\":true,\"pixelMode\":true"
                    + ",\"preset\":\"" + preset.getPaletteResource() + "\""
                    + ",\"presetName\":\"" + preset.getDisplayName() + "\""
                    + ",\"width\":" + preset.getWidth()
                    + ",\"height\":" + preset.getHeight()
                    + ",\"colors\":" + preset.getColorCount()
                    + ",\"cellCount\":" + engine.getTriangleCount()
                    + ",\"initialFitness\":" + engine.getFitness() + "}";
            sendJsonResponse(ex, json);
            return;
        }

        BufferedImage sourceImage;
        Palette palette;
        int gridW, gridH;
        float triW, triH, triScale;

        if (sampleId != null) {
            if (sampleId.startsWith("custom:")) {
                String fp = sampleId.substring(7);
                Path file = getCustomImagesDir().resolve(fp + ".png");
                if (!Files.exists(file)) {
                    sendJsonResponse(ex, "{\"initialized\":false,\"error\":\"Custom image not found.\"}");
                    return;
                }
                sourceImage = ImageIO.read(file.toFile());
            } else {
                var def = SampleImageProvider.getDef(sampleId);
                if (def == null || !clickerState.isSampleUnlocked(def)) {
                    String msg = def == null ? "Unknown sample." : "Sample not unlocked yet.";
                    sendJsonResponse(ex, "{\"initialized\":false,\"error\":\"" + msg + "\"}");
                    return;
                }
                sourceImage = SampleImageProvider.generate(sampleId);
            }
            try {
                palette = new Palette("Sherwin-Williams", DEFAULT_PALETTES);
            } catch (Exception e) {
                sendJsonResponse(ex, "{\"initialized\":false,\"error\":\"Palette failed.\"}");
                return;
            }
            gridW = DEFAULT_WIDTH_TRI;
            gridH = DEFAULT_HEIGHT_TRI;
            triW = DEFAULT_TRI_WIDTH;
            triH = DEFAULT_TRI_HEIGHT;
            triScale = DEFAULT_TRI_SCALE;
        } else if (standaloneMode) {
            sourceImage = uploadedImage;
            palette = uploadedPalette;
            gridW = DEFAULT_WIDTH_TRI;
            gridH = DEFAULT_HEIGHT_TRI;
            triW = DEFAULT_TRI_WIDTH;
            triH = DEFAULT_TRI_HEIGHT;
            triScale = DEFAULT_TRI_SCALE;
        } else {
            sourceImage = artEvolver.getResizedOriginal();
            palette = artEvolver.getPallete();
            gridW = artEvolver.getWidthTriangles();
            gridH = artEvolver.getHeightTriangles();
            triW = artEvolver.getTriangleWidth();
            triH = artEvolver.getTriangleHeight();
            triScale = artEvolver.getTriangleScaleHeight();
        }

        if (sourceImage == null || palette == null) {
            String msg = standaloneMode
                    ? "Drop or pick an image above to begin."
                    : "No source image loaded. Load an image in ArtEvolver first.";
            String json = "{\"initialized\":false,\"error\":\"" + msg + "\"}";
            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.sendResponseHeaders(200, data.length);
            ex.getResponseBody().write(data);
            ex.close();
            return;
        }

        String error = clickerState.initEngine(sourceImage, palette, gridW, gridH, triW, triH, triScale, sampleId);

        if (error != null) {
            String json = "{\"initialized\":false,\"error\":\"" + error.replace("\"", "\\\"") + "\"}";
            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.sendResponseHeaders(200, data.length);
            ex.getResponseBody().write(data);
            ex.close();
            return;
        }

        var engine = clickerState.getEngine();
        String json = "{\"initialized\":true,\"triangleCount\":" + engine.getTriangleCount()
                + ",\"initialFitness\":" + engine.getFitness() + "}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private void handleClickerState(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { sendError(ex, 405); return; }

        clickerState.tick();

        String raw = clickerState.toJson();
        String json = raw.startsWith("{")
                ? "{\"standaloneMode\":" + standaloneMode + "," + raw.substring(1)
                : raw;
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
        ClickerState.ClickResponse resp = clickerState.click();
        var engine = clickerState.getEngine();
        String json = "{\"earned\":" + resp.earned()
                + ",\"ep\":" + clickerState.getEp()
                + ",\"fitness\":" + resp.newFitness()
                + ",\"fitnessGain\":" + resp.fitnessGain()
                + ",\"successCount\":" + resp.successCount()
                + ",\"attemptCount\":" + resp.attemptCount()
                + ",\"totalSwaps\":" + (engine != null ? engine.getTotalSwaps() : 0)
                + ",\"successSwaps\":" + (engine != null ? engine.getSuccessfulSwaps() : 0)
                + ",\"critical\":" + resp.critical()
                + ",\"missStreak\":" + resp.missStreak()
                + ",\"hitStreak\":" + resp.hitStreak() + "}";
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
            String reason = "ok";
            if (!success) {
                reason = clickerState.isMaxed(id) ? "maxed" : "poor";
            }
            json = "{\"success\":" + success + ",\"reason\":\"" + reason + "\",\"ep\":" + clickerState.getEp() + "}";
        }
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private void handleClickerPrestige(HttpExchange ex) throws IOException {
        consumeRequestBody(ex);
        boolean success = clickerState.ascend();
        if (success && standaloneMode) {
            uploadedImage = null;
            uploadedPalette = null;
        }
        String json = "{\"success\":" + success + ",\"gf\":" + clickerState.getGf()
                + ",\"ascensions\":" + clickerState.getAscensionCount()
                + ",\"needsNewImage\":" + success
                + ",\"galleryCount\":" + clickerState.getGallery().size()
                + ",\"standaloneMode\":" + standaloneMode + "}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private void handleClickerGallery(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { sendError(ex, 405); return; }
        String json = clickerState.getGalleryJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private void handleClickerSamples(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { sendError(ex, 405); return; }
        String json = clickerState.getSamplesJson();
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private void handleClickerSampleImage(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { sendError(ex, 405); return; }
        String path = ex.getRequestURI().getPath();
        String id = path.replace("/api/clicker/sample/", "").split("/")[0].trim();
        if (id.isEmpty()) { sendError(ex, 400); return; }
        var def = SampleImageProvider.getDef(id);
        if (def == null) { sendError(ex, 404); return; }
        BufferedImage img = SampleImageProvider.generate(id);
        String query = ex.getRequestURI().getQuery();
        if (query != null && query.contains("thumb=1")) {
            int tw = 120, th = 78;
            BufferedImage thumb = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = thumb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, tw, th, null);
            g.dispose();
            img = thumb;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
        ImageIO.write(img, "png", baos);
        byte[] data = baos.toByteArray();
        ex.getResponseHeaders().set("Content-Type", "image/png");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private void handleClickerComplete(HttpExchange ex) throws IOException {
        consumeRequestBody(ex);
        ClickerState.MasterpieceResult result = clickerState.completeMasterpiece();
        String json;
        if (result != null) {
            if (standaloneMode) {
                uploadedImage = null;
                uploadedPalette = null;
            }
            json = "{\"success\":true,\"gfReward\":" + result.gfReward()
                    + ",\"finalFitness\":" + result.finalFitness()
                    + ",\"completedImages\":" + result.totalCompleted()
                    + ",\"totalGf\":" + clickerState.getGf()
                    + ",\"standaloneMode\":" + standaloneMode + "}";
        } else {
            json = "{\"success\":false}";
        }
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private void handleClickerImage(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { sendError(ex, 405); return; }

        var engine = clickerState.getEngine();
        if (engine == null || !engine.isInitialized()) { sendError(ex, 404); return; }

        String clientEtag = ex.getRequestHeaders().getFirst("If-None-Match");
        String currentEtag = Long.toHexString(engine.getJpegVersion());
        if (clientEtag != null && clientEtag.equals(currentEtag)) {
            ex.getResponseHeaders().set("ETag", currentEtag);
            ex.sendResponseHeaders(304, -1);
            ex.close();
            return;
        }

        byte[] jpeg = engine.getRenderedImageAsJpeg();
        if (jpeg == null) { sendError(ex, 500); return; }

        ex.getResponseHeaders().set("Content-Type", "image/jpeg");
        ex.getResponseHeaders().set("ETag", Long.toHexString(engine.getJpegVersion()));
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, jpeg.length);
        ex.getResponseBody().write(jpeg);
        ex.close();
    }

    private void handleClickerReference(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { sendError(ex, 405); return; }

        var engine = clickerState.getEngine();
        if (engine == null || engine.getReferenceImage() == null) { sendError(ex, 404); return; }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(engine.getReferenceImage(), "jpg", baos);
        byte[] data = baos.toByteArray();

        ex.getResponseHeaders().set("Content-Type", "image/jpeg");
        ex.getResponseHeaders().set("Cache-Control", "max-age=3600");
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
        if (managerWindow == null) {
            return "{\"standaloneMode\":true,\"timestamp\":" + System.currentTimeMillis()
                    + ",\"contestants\":[]}";
        }
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
            sb.append("    \"bestEverScore\": ").append(DF4.format(evo.getBestEverScore() * 100)).append(",\n");
            sb.append("    \"maxLifespan\": ").append(evo.getMaxLifespanSeconds()).append(",\n");
            sb.append("    \"adaptiveLifetime\": ").append(evo.isAdaptiveLifetimeEnabled()).append(",\n");
            sb.append("    \"adaptiveLifetimeMode\": ").append(jsonStr(evo.getAdaptiveLifetimeMode().name())).append(",\n");
            sb.append("    \"initialLifespan\": ").append(evo.getInitialLifespanSeconds()).append(",\n");
            sb.append("    \"longestLifetime\": ").append(DF2.format(evo.getLongestRecordedLifetime())).append(",\n");
            sb.append("    \"avgLifetime\": ").append(DF2.format(evo.getAverageRecordedLifetime())).append(",\n");
            sb.append("    \"lifetimeRecords\": ").append(evo.getLifetimeRecordCount()).append(",\n");
            sb.append("    \"anomalies\": ").append(evo.getAnomalyCount()).append(",\n");
            sb.append("    \"adaptiveGrowths\": ").append(evo.getAdaptiveGrowthCount()).append(",\n");
            sb.append("    \"absoluteMaxLifespan\": ").append(evo.getAbsoluteMaxLifespanSeconds()).append("\n");
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
            sb.append("      \"multiStage\": ").append(c.isMultiStage()).append(",\n");
            sb.append("      \"currentStage\": ").append(c.getCurrentStageIndex() + 1).append(",\n");
            sb.append("      \"totalStages\": ").append(c.getTotalStages()).append(",\n");
            sb.append("      \"stageName\": ").append(jsonStr(c.getCurrentStageName())).append(",\n");
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
        List<EvolutionaryTournament.GenerationRecord> genRecords = null;
        if (evo != null) {
            genRecords = evo.getHistory();
            for (EvolutionaryTournament.GenerationRecord rec : genRecords) {
                narratives.add(rec.toNarrative());
            }
        }
        for (int i = 0; i < narratives.size(); i++) {
            sb.append("    ").append(jsonStr(narratives.get(i)));
            if (i < narratives.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // Per-generation stats for the evolution chart
        sb.append("  \"generationHistory\": [\n");
        if (genRecords != null) {
            boolean first = true;
            for (EvolutionaryTournament.GenerationRecord rec : genRecords) {
                if (rec.skipped) continue;
                if (!first) sb.append(",\n");
                first = false;
                sb.append("    {");
                sb.append("\"gen\":").append(rec.generation).append(",");
                sb.append("\"best\":").append(DF4.format(rec.bestScore * 100)).append(",");
                sb.append("\"avg\":").append(DF4.format(rec.avgScore * 100)).append(",");
                sb.append("\"worst\":").append(DF4.format(rec.worstScore * 100)).append(",");
                sb.append("\"bestEver\":").append(DF4.format(rec.bestEverScore * 100)).append(",");
                sb.append("\"alive\":").append(rec.aliveCount).append(",");
                sb.append("\"promoted\":").append(rec.promotedCount).append(",");
                sb.append("\"stalled\":").append(rec.stalledGens).append(",");
                sb.append("\"cutoff\":").append(rec.currentCutoff);
                sb.append("}");
            }
            if (!first) sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}");

        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════════
    //  IMAGE UPLOAD (standalone / clicker-only mode)
    // ═══════════════════════════════════════════════════════════════

    private static Path getCustomImagesDir() {
        Path dir = Path.of(System.getProperty("user.home", "."), ".artevolver", "clicker-uploads");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {}
        return dir;
    }

    private void saveCustomImageToGallery(BufferedImage img) {
        if (img == null) return;
        long fp = ClickerState.computeFingerprint(img);
        Path dir = getCustomImagesDir();
        Path file = dir.resolve(fp + ".png");
        if (Files.exists(file)) return;
        try {
            ImageIO.write(img, "png", file.toFile());
        } catch (IOException e) {
            System.err.println("[Clicker] Could not save custom image: " + e.getMessage());
        }
    }

    private void handleClickerMyImages(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { sendError(ex, 405); return; }
        Path dir = getCustomImagesDir();
        List<Map<String, Object>> list = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            for (Path p : stream.filter(f -> f.toString().endsWith(".png")).toList()) {
                String name = p.getFileName().toString();
                String fp = name.replace(".png", "");
                if (!fp.matches("\\d+")) continue;
                list.add(Map.<String, Object>of("fingerprint", fp));
            }
        } catch (IOException e) {
            // empty list
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\"images\":[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"fingerprint\":\"").append(list.get(i).get("fingerprint")).append("\"}");
        }
        sb.append("]}");
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private void handleClickerMyImage(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { sendError(ex, 405); return; }
        String path = ex.getRequestURI().getPath();
        String fp = path.replace("/api/clicker/my-image/", "").split("/")[0].trim();
        if (!fp.matches("\\d+")) { sendError(ex, 400); return; }
        Path file = getCustomImagesDir().resolve(fp + ".png");
        if (!Files.exists(file)) { sendError(ex, 404); return; }
        BufferedImage img = ImageIO.read(file.toFile());
        if (img == null) { sendError(ex, 500); return; }
        String query = ex.getRequestURI().getQuery();
        if (query != null && query.contains("thumb=1")) {
            BufferedImage thumb = new BufferedImage(120, 78, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = thumb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, 120, 78, null);
            g.dispose();
            img = thumb;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
        ImageIO.write(img, "png", baos);
        byte[] data = baos.toByteArray();
        ex.getResponseHeaders().set("Content-Type", "image/png");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private void handleClickerUpload(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { sendError(ex, 405); return; }

        byte[] body = ex.getRequestBody().readAllBytes();
        if (body.length == 0) {
            sendJsonResponse(ex, "{\"success\":false,\"error\":\"No image data received.\"}");
            return;
        }

        // Parse multipart boundary from Content-Type header
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        byte[] imageBytes;
        if (contentType != null && contentType.contains("multipart/form-data")) {
            imageBytes = extractMultipartFile(body, contentType);
            if (imageBytes == null) {
                sendJsonResponse(ex, "{\"success\":false,\"error\":\"Could not parse multipart upload.\"}");
                return;
            }
        } else {
            imageBytes = body;
        }

        BufferedImage original;
        try {
            original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (Exception e) {
            sendJsonResponse(ex, "{\"success\":false,\"error\":\"Invalid image format.\"}");
            return;
        }
        if (original == null) {
            sendJsonResponse(ex, "{\"success\":false,\"error\":\"Could not decode image. Supported: JPG, PNG, BMP, GIF.\"}");
            return;
        }

        int newWidth = (int) (DEFAULT_TRI_WIDTH * DEFAULT_WIDTH_TRI);
        int newHeight = (int) (DEFAULT_TRI_HEIGHT * DEFAULT_HEIGHT_TRI - DEFAULT_TRI_HEIGHT);
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newWidth, newHeight,
                0, 0, original.getWidth(), original.getHeight(), null);
        g.dispose();

        Palette palette;
        try {
            palette = new Palette("Sherwin-Williams", DEFAULT_PALETTES);
        } catch (URISyntaxException e) {
            sendJsonResponse(ex, "{\"success\":false,\"error\":\"Palette loading failed: " + e.getMessage() + "\"}");
            return;
        }

        this.uploadedImage = resized;
        this.uploadedPalette = palette;

        saveCustomImageToGallery(resized);

        System.out.println("[Clicker] Image uploaded: " + original.getWidth() + "x" + original.getHeight()
                + " -> resized to " + newWidth + "x" + newHeight);

        String json = "{\"success\":true,\"width\":" + newWidth + ",\"height\":" + newHeight
                + ",\"triangles\":" + (DEFAULT_WIDTH_TRI * DEFAULT_HEIGHT_TRI) + "}";
        sendJsonResponse(ex, json);
    }

    private byte[] extractMultipartFile(byte[] body, String contentType) {
        String boundary = null;
        for (String part : contentType.split(";")) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                boundary = part.substring("boundary=".length()).trim();
                if (boundary.startsWith("\"") && boundary.endsWith("\""))
                    boundary = boundary.substring(1, boundary.length() - 1);
                break;
            }
        }
        if (boundary == null) return null;

        byte[] sep = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        int start = indexOf(body, new byte[]{13, 10, 13, 10}, 0);
        if (start < 0) start = indexOf(body, new byte[]{10, 10}, 0);
        if (start < 0) return null;
        start += (body[start] == 13 ? 4 : 2);

        byte[] endSep = ("\r\n--" + boundary).getBytes(StandardCharsets.UTF_8);
        int end = indexOf(body, endSep, start);
        if (end < 0) {
            endSep = ("\n--" + boundary).getBytes(StandardCharsets.UTF_8);
            end = indexOf(body, endSep, start);
        }
        if (end < 0) end = body.length;

        return Arrays.copyOfRange(body, start, end);
    }

    private static int indexOf(byte[] data, byte[] pattern, int from) {
        outer:
        for (int i = from; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private void sendJsonResponse(HttpExchange ex, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private void consumeRequestBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            is.readAllBytes();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    public boolean isStandaloneMode() { return standaloneMode; }

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
