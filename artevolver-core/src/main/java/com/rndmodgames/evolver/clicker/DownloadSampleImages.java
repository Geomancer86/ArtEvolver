package com.rndmodgames.evolver.clicker;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Downloads real free images for the Sample Atlas from Picsum Photos (Unsplash)
 * and Wikimedia Commons. Replaces placeholder gradients with stunning landscapes,
 * wallpapers, and public-domain art.
 *
 * Run: mvn compile exec:java -Dexec.mainClass="com.rndmodgames.evolver.clicker.DownloadSampleImages"
 */
public final class DownloadSampleImages {

    private static final int W = SampleImageProvider.WIDTH;
    private static final int H = SampleImageProvider.HEIGHT;

    /** Picsum (Unsplash) image IDs - real photos, free to use */
    private static final Map<String, Integer> PICSUM_IDS = Map.ofEntries(
            Map.entry("landscape", 28),
            Map.entry("ocean", 17),
            Map.entry("forest", 26),
            Map.entry("sunset", 15),
            Map.entry("flowers", 30),
            Map.entry("mountain", 10),
            Map.entry("canyon", 18),
            Map.entry("lake", 32),
            Map.entry("meadow", 29),
            Map.entry("beach", 11),
            Map.entry("cityscape", 34),
            Map.entry("architecture", 35),
            Map.entry("portrait", 37),
            Map.entry("wildlife", 40),
            Map.entry("night", 21),
            Map.entry("abstract", 39),
            Map.entry("macro", 31),
            Map.entry("aerial", 27),
            Map.entry("street", 38),
            Map.entry("still_life", 33)
    );

    /** Wikimedia Commons - public domain art. Resized to 720x468 in code. */
    private static final Map<String, String> WIKIMEDIA_URLS = Map.ofEntries(
            Map.entry("mona_lisa", "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ec/Mona_Lisa%2C_by_Leonardo_da_Vinci%2C_from_C2RMF_retouched.jpg/720px-Mona_Lisa%2C_by_Leonardo_da_Vinci%2C_from_C2RMF_retouched.jpg"),
            Map.entry("starry_night", "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg/720px-Van_Gogh_-_Starry_Night_-_Google_Art_Project.jpg"),
            Map.entry("great_wave", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/The_Great_Wave_off_Kanagawa.jpg/720px-The_Great_Wave_off_Kanagawa.jpg"),
            Map.entry("girl_pearl_earring", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0f/1665_Girl_with_a_Pearl_Earring.jpg/720px-1665_Girl_with_a_Pearl_Earring.jpg"),
            Map.entry("birth_of_venus", "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0b/Sandro_Botticelli_-_La_nascita_di_Venere_-_Google_Art_Project_-_edited.jpg/720px-Sandro_Botticelli_-_La_nascita_di_Venere_-_Google_Art_Project_-_edited.jpg"),
            Map.entry("american_gothic", "https://upload.wikimedia.org/wikipedia/commons/thumb/4/42/Grant_Wood_-_American_Gothic_-_Google_Art_Project.jpg/720px-Grant_Wood_-_American_Gothic_-_Google_Art_Project.jpg"),

            // Legendary expansion (public domain photography, cinema, posters)
            Map.entry("lunch_atop_skyscraper", "https://commons.wikimedia.org/wiki/Special:FilePath/Lunch_atop_a_Skyscraper.jpg"),
            Map.entry("migrant_mother", "https://commons.wikimedia.org/wiki/Special:FilePath/Lange-MigrantMother02.jpg"),
            Map.entry("aldrin_moon", "https://commons.wikimedia.org/wiki/Special:FilePath/Aldrin_Apollo_11.jpg"),
            Map.entry("earthrise", "https://commons.wikimedia.org/wiki/Special:FilePath/AS08-14-2383.jpg"),
            Map.entry("blue_marble", "https://commons.wikimedia.org/wiki/Special:FilePath/The_Blue_Marble_(remastered).jpg"),
            Map.entry("nosferatu_orlok", "https://commons.wikimedia.org/wiki/Special:FilePath/Film_Nosferatu_(van_F,_SFA008003709.jpg"),
            Map.entry("metropolis_set", "https://commons.wikimedia.org/wiki/Special:FilePath/Horst_von_Harbou_-_Metropolis_set_photograph_10.jpg"),
            Map.entry("caligari_still", "https://commons.wikimedia.org/wiki/Special:FilePath/CABINET_DES_DR_CALIGARI_01.jpg"),
            Map.entry("safety_last_ad", "https://commons.wikimedia.org/wiki/Special:FilePath/Safety_Last_-_The_Film_Daily,_March_4,_1923_01.jpg"),
            Map.entry("uncle_sam_poster", "https://commons.wikimedia.org/wiki/Special:FilePath/I_Want_You_for_U.S._Army_by_James_Montgomery_Flagg.jpg"),
            Map.entry("weapons_for_liberty", "https://commons.wikimedia.org/wiki/Special:FilePath/Weapons_for_Liberty.jpg"),
            Map.entry("wpa_national_parks", "https://commons.wikimedia.org/wiki/Special:FilePath/The_national_parks_preserve_wild_life,_WPA_poster,_ca._1938.jpg"),
            Map.entry("wpa_work_pays", "https://commons.wikimedia.org/wiki/Special:FilePath/WPA-Work-Pays-America-Poster.jpg")
    );

    /** Secret samples - use Picsum for variety */
    private static final Map<String, Integer> SECRET_PICSUM = Map.of(
            "secret_suffer", 41,
            "secret_grind", 42,
            "secret_ascended", 43,
            "secret_gilded", 44,
            "secret_omega", 45
    );

    public static void main(String[] args) throws Exception {
        Path outDir = Paths.get("src/main/resources/samples");
        Files.createDirectories(outDir);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        System.out.println("Downloading real images to " + outDir.toAbsolutePath());

        // Picsum (nature, landscapes, etc.)
        for (Map.Entry<String, Integer> e : PICSUM_IDS.entrySet()) {
            String id = e.getKey();
            String url = "https://picsum.photos/id/" + e.getValue() + "/" + W + "/" + H;
            downloadAndSave(client, url, outDir.resolve(id + ".jpg"));
        }

        // Secret
        for (Map.Entry<String, Integer> e : SECRET_PICSUM.entrySet()) {
            String id = e.getKey();
            String url = "https://picsum.photos/id/" + e.getValue() + "/" + W + "/" + H;
            downloadAndSave(client, url, outDir.resolve(id + ".jpg"));
        }

        // Wikimedia (art) - persistence_of_memory may have copyright; use Picsum fallback
        for (Map.Entry<String, String> e : WIKIMEDIA_URLS.entrySet()) {
            downloadAndSave(client, e.getValue(), outDir.resolve(e.getKey() + ".jpg"));
        }
        downloadAndSave(client, "https://picsum.photos/id/46/" + W + "/" + H, outDir.resolve("persistence_of_memory.jpg"));

        System.out.println("Done. Images from Unsplash (Picsum) and Wikimedia Commons.");
    }

    private static void downloadAndSave(HttpClient client, String url, Path out) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "ArtEvolver-SampleDownloader/1.0")
                .GET()
                .build();

        HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() != 200) {
            System.err.println("  FAIL " + out.getFileName() + " (HTTP " + resp.statusCode() + ")");
            return;
        }

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(resp.body()));
        if (img == null) {
            System.err.println("  FAIL " + out.getFileName() + " (could not decode)");
            return;
        }

        if (img.getWidth() != W || img.getHeight() != H) {
            BufferedImage resized = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, W, H, null);
            g.dispose();
            img = resized;
        }

        ImageIO.write(img, "jpg", out.toFile());
        System.out.println("  " + out.getFileName());
    }
}
