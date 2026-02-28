package com.rndmodgames.evolver.clicker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Manages player profiles stored as JSON files in ~/.artevolver/profiles/.
 *
 * Miyamoto: picking your save file should feel like choosing a world.
 * Sakaguchi: hours of grinding should NEVER be lost.
 * Yokoi: plain JSON files — no database, no binary formats, easy to inspect.
 */
public class ProfileManager {

    private static final String DIR_NAME = "profiles";
    private static final String DEFAULT_PROFILE = "Player";
    private static final String FILE_EXT = ".json";
    private final Path profileDir;

    public ProfileManager() {
        this.profileDir = Path.of(System.getProperty("user.home"), ".artevolver", DIR_NAME);
        try {
            Files.createDirectories(profileDir);
        } catch (IOException e) {
            System.err.println("[ProfileManager] Failed to create profiles directory: " + e.getMessage());
        }
    }

    public Path getProfileDir() { return profileDir; }

    /** Returns sorted list of profile names (without .json extension). */
    public List<String> listProfiles() {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(profileDir)) return names;
        try (Stream<Path> stream = Files.list(profileDir)) {
            stream.filter(p -> p.toString().endsWith(FILE_EXT))
                  .map(p -> p.getFileName().toString().replace(FILE_EXT, ""))
                  .sorted()
                  .forEach(names::add);
        } catch (IOException e) {
            System.err.println("[ProfileManager] Failed to list profiles: " + e.getMessage());
        }
        return names;
    }

    /** Returns true if the named profile exists on disk. */
    public boolean exists(String name) {
        return Files.exists(profilePath(name));
    }

    /** Creates an empty profile file. Returns false if it already exists. */
    public boolean create(String name) {
        Path path = profilePath(sanitize(name));
        if (Files.exists(path)) return false;
        try {
            Files.writeString(path, "{}", StandardCharsets.UTF_8);
            System.out.println("[ProfileManager] Created profile: " + name);
            return true;
        } catch (IOException e) {
            System.err.println("[ProfileManager] Failed to create profile: " + e.getMessage());
            return false;
        }
    }

    /** Deletes a profile. Returns false if it doesn't exist. */
    public boolean delete(String name) {
        Path path = profilePath(sanitize(name));
        try {
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) System.out.println("[ProfileManager] Deleted profile: " + name);
            return deleted;
        } catch (IOException e) {
            System.err.println("[ProfileManager] Failed to delete profile: " + e.getMessage());
            return false;
        }
    }

    /** Reads profile JSON from disk. Returns null if not found. */
    public String load(String name) {
        Path path = profilePath(sanitize(name));
        if (!Files.exists(path)) return null;
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[ProfileManager] Failed to load profile '" + name + "': " + e.getMessage());
            return null;
        }
    }

    /** Writes profile JSON to disk. Creates file if needed. */
    public boolean save(String name, String json) {
        Path path = profilePath(sanitize(name));
        try {
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException e) {
            try {
                Files.writeString(path, json, StandardCharsets.UTF_8);
                return true;
            } catch (IOException e2) {
                System.err.println("[ProfileManager] Failed to save profile '" + name + "': " + e2.getMessage());
                return false;
            }
        }
    }

    /** Returns the default profile name. */
    public String getDefaultProfile() { return DEFAULT_PROFILE; }

    /** Ensures at least the default profile exists. */
    public void ensureDefault() {
        if (listProfiles().isEmpty()) {
            create(DEFAULT_PROFILE);
        }
    }

    /** Gets file metadata for a profile (size, last modified). */
    public ProfileMeta getMeta(String name) {
        Path path = profilePath(sanitize(name));
        if (!Files.exists(path)) return null;
        try {
            long size = Files.size(path);
            long lastModified = Files.getLastModifiedTime(path).toMillis();
            return new ProfileMeta(name, size, lastModified);
        } catch (IOException e) {
            return new ProfileMeta(name, 0, 0);
        }
    }

    private Path profilePath(String name) {
        return profileDir.resolve(sanitize(name) + FILE_EXT);
    }

    public static String sanitize(String name) {
        if (name == null || name.isBlank()) return DEFAULT_PROFILE;
        return name.replaceAll("[^a-zA-Z0-9_\\- ]", "").trim();
    }

    public record ProfileMeta(String name, long fileSize, long lastModifiedMs) {}
}
