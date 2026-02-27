package com.rndmodgames.evolver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Persistent disk cache for resized source images.
 *
 * High-resolution camera images (Canon EOS R5 45MP, R1 50MP, etc.) take significant
 * time to read and resize. This cache stores the resized result as a PNG on disk so
 * subsequent loads of the same image with the same grid settings are near-instant.
 *
 * Cache key = SHA-256 of (absolute path + file size + last modified + target width + target height).
 * Cache location = ~/.artevolver/cache/
 */
public class ImageDiskCache {

    private static final String CACHE_DIR_NAME = ".artevolver";
    private static final String CACHE_SUBDIR = "cache";
    private static final long MAX_CACHE_SIZE_MB = 500;
    private static final int MAX_CACHE_FILES = 200;

    private final File cacheDir;

    public ImageDiskCache() {
        cacheDir = new File(System.getProperty("user.home"), CACHE_DIR_NAME + File.separator + CACHE_SUBDIR);
        if (!cacheDir.exists()) {
            boolean created = cacheDir.mkdirs();
            if (created) {
                System.out.println("[ImageCache] Created cache directory: " + cacheDir.getAbsolutePath());
            }
        }
    }

    /**
     * Builds a unique cache key from the source file identity and target dimensions.
     * Two loads of the same file with the same grid will hit cache; changing grid
     * or modifying the file invalidates.
     */
    public String buildKey(File sourceFile, int targetWidth, int targetHeight) {
        String raw = sourceFile.getAbsolutePath()
                + "|" + sourceFile.length()
                + "|" + sourceFile.lastModified()
                + "|" + targetWidth + "x" + targetHeight;
        return sha256(raw);
    }

    /**
     * Returns the cached resized image, or null if not cached.
     */
    public BufferedImage get(String key) {
        File cached = new File(cacheDir, key + ".png");
        if (!cached.exists()) return null;

        try {
            long start = System.currentTimeMillis();
            BufferedImage img = ImageIO.read(cached);
            long elapsed = System.currentTimeMillis() - start;
            if (img != null) {
                System.out.println("[ImageCache] HIT: loaded " + img.getWidth() + "x" + img.getHeight()
                        + " from cache in " + elapsed + "ms");
                cached.setLastModified(System.currentTimeMillis());
            }
            return img;
        } catch (IOException e) {
            System.err.println("[ImageCache] Failed to read cache file: " + e.getMessage());
            cached.delete();
            return null;
        }
    }

    /**
     * Stores a resized image in the disk cache.
     */
    public void put(String key, BufferedImage image) {
        evictIfNeeded();

        File cached = new File(cacheDir, key + ".png");
        try {
            long start = System.currentTimeMillis();
            ImageIO.write(image, "png", cached);
            long elapsed = System.currentTimeMillis() - start;
            long sizeKB = cached.length() / 1024;
            System.out.println("[ImageCache] STORE: " + image.getWidth() + "x" + image.getHeight()
                    + " (" + sizeKB + "KB) in " + elapsed + "ms");
        } catch (IOException e) {
            System.err.println("[ImageCache] Failed to write cache file: " + e.getMessage());
        }
    }

    /**
     * Evicts oldest files if the cache exceeds size or count limits.
     */
    private void evictIfNeeded() {
        File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (files == null) return;

        long totalSize = 0;
        for (File f : files) totalSize += f.length();

        if (files.length < MAX_CACHE_FILES && totalSize < MAX_CACHE_SIZE_MB * 1024 * 1024) return;

        java.util.Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));

        int deleted = 0;
        for (File f : files) {
            if (files.length - deleted <= MAX_CACHE_FILES / 2
                    && totalSize < MAX_CACHE_SIZE_MB * 1024 * 1024 / 2) break;
            totalSize -= f.length();
            f.delete();
            deleted++;
        }

        if (deleted > 0) {
            System.out.println("[ImageCache] Evicted " + deleted + " old cache entries");
        }
    }

    /**
     * Clears all cached images.
     */
    public void clearAll() {
        File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (files == null) return;
        int count = 0;
        for (File f : files) {
            if (f.delete()) count++;
        }
        System.out.println("[ImageCache] Cleared " + count + " cached images");
    }

    /**
     * Returns the number of cached entries and total size.
     */
    public String getStats() {
        File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (files == null || files.length == 0) return "0 entries, 0 MB";
        long totalSize = 0;
        for (File f : files) totalSize += f.length();
        return files.length + " entries, " + (totalSize / (1024 * 1024)) + " MB";
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
