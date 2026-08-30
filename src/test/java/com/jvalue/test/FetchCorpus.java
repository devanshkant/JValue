package com.jvalue.test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads the JSONTestSuite conformance corpus using only JDK APIs.
 *
 * <p>This replaces the need for {@code git clone}, {@code curl}, or
 * any external download tool. Uses {@link HttpClient} (Java 11+) to
 * fetch a ZIP archive from GitHub, and {@link ZipInputStream} to
 * extract test files.</p>
 *
 * <p>If the corpus already exists locally, this tool exits immediately.
 * If the download fails (e.g., offline), a clear warning is printed
 * and the tool exits with code 0 so the build continues — the
 * conformance tests will gracefully skip when the corpus is absent.</p>
 *
 * <p>STDLIB substitution: replaces git submodule / curl+tar / wget
 * with {@code java.net.http.HttpClient} + {@code java.util.zip.ZipInputStream}
 * + {@code java.nio.file.Files}.</p>
 */
public final class FetchCorpus {

    private static final String ARCHIVE_URL =
        "https://github.com/nst/JSONTestSuite/archive/refs/heads/master.zip";

    private static final Path TARGET_DIR = Path.of("test-data", "JSONTestSuite");

    /**
     * The ZIP archive contains files under a root directory named
     * "JSONTestSuite-master/". We strip this prefix during extraction
     * so files land directly under test-data/JSONTestSuite/.
     */
    private static final String ZIP_ROOT_PREFIX = "JSONTestSuite-master/";

    public static void main(String[] args) {
        if (Files.exists(TARGET_DIR.resolve("test_parsing"))) {
            System.out.println("[FetchCorpus] Corpus already present at " + TARGET_DIR);
            return;
        }

        System.out.println("[FetchCorpus] Downloading JSONTestSuite corpus...");
        System.out.println("  URL: " + ARCHIVE_URL);

        try {
            byte[] zipData = download(ARCHIVE_URL);
            System.out.printf("[FetchCorpus] Downloaded %,d bytes%n", zipData.length);

            int extracted = extractZip(zipData, TARGET_DIR);
            System.out.printf("[FetchCorpus] Extracted %d files to %s%n", extracted, TARGET_DIR);
            System.out.println("[FetchCorpus] Done.");
        } catch (IOException | InterruptedException e) {
            System.out.println("[FetchCorpus] WARNING: Could not download corpus: " + e.getMessage());
            System.out.println("[FetchCorpus] Conformance tests will be skipped.");
            System.out.println("[FetchCorpus] To obtain manually:");
            System.out.println("  git clone --depth=1 https://github.com/nst/JSONTestSuite test-data/JSONTestSuite");
            // Exit 0: the build should continue even without the corpus
        }
    }

    private static byte[] download(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " from " + url);
        }

        return response.body();
    }

    private static int extractZip(byte[] zipData, Path targetDir) throws IOException {
        int count = 0;

        try (ZipInputStream zis = new ZipInputStream(
                new java.io.ByteArrayInputStream(zipData))) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                // Strip the GitHub ZIP root prefix
                if (!name.startsWith(ZIP_ROOT_PREFIX)) {
                    continue;
                }
                String relative = name.substring(ZIP_ROOT_PREFIX.length());

                if (relative.isEmpty()) {
                    continue;
                }

                Path outPath = targetDir.resolve(relative);

                // Security: prevent zip-slip attacks
                if (!outPath.normalize().startsWith(targetDir.normalize())) {
                    throw new IOException("Zip entry outside target directory: " + name);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    Files.write(outPath, zis.readAllBytes());
                    count++;
                }

                zis.closeEntry();
            }
        }

        return count;
    }
}
