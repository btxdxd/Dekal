package btxds.dekal;

import btxds.dekal.util.FileChecker;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadManager {

    private static final String BASE_URL = "https://" + Config.SERVER_DOWNLOAD_HOST;
    private static final Path JRE_DIR = Path.of("jre");
    private static final Path LAUNCHER_JAR = Path.of(Config.SERVER_DOWNLOAD_LAUNCHER);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static boolean ensureJre() {
        String hashUrl = BASE_URL + "/" + Config.SERVER_DOWNLOAD_JRE + "/hash";
        String fileUrl = BASE_URL + "/" + Config.SERVER_DOWNLOAD_JRE;

        if (FileChecker.isJreValid(JRE_DIR, hashUrl)) {
            return true;
        }

        Path tempZip = Path.of("jre.tmp");
        try {
            if (!download(fileUrl, tempZip)) return false;

            unpackZip(tempZip, JRE_DIR);
            Files.deleteIfExists(tempZip);

            String remoteHash = FileChecker.getRemoteHash(hashUrl);
            if (remoteHash.isEmpty()) return false;

            FileChecker.saveJreHash(JRE_DIR, remoteHash);
            return true;

        } catch (Exception e) {
            try { Files.deleteIfExists(tempZip); } catch (Exception ignored) {}
            return false;
        }
    }

    public static boolean ensureLauncher() {
        String hashUrl = BASE_URL + "/" + Config.SERVER_DOWNLOAD_LAUNCHER + "/hash";
        String fileUrl = BASE_URL + "/" + Config.SERVER_DOWNLOAD_LAUNCHER;

        if (FileChecker.isLauncherValid(LAUNCHER_JAR, hashUrl)) {
            return true;
        }

        return download(fileUrl, LAUNCHER_JAR);
    }

    public static void launch() {
        try {
            String javaw = System.getProperty("os.name").toLowerCase().contains("win") ? "javaw.exe" : "java";
            Path javaBin = JRE_DIR.resolve("bin").resolve(javaw);

            if (!Files.exists(javaBin) || !Files.exists(LAUNCHER_JAR)) {
                System.exit(0);
            }

            new ProcessBuilder(
                    javaBin.toAbsolutePath().toString(),
                    "-jar",
                    LAUNCHER_JAR.toAbsolutePath().toString()
            ).start();

        } catch (Exception ignored) {
            System.exit(0);
        }
    }

    private static boolean download(String url, Path destination) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(10))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                return false;
            }

            if (destination.getParent() != null) {
                Files.createDirectories(destination.getParent());
            }

            Path tempFile = destination.resolveSibling(destination.getFileName() + ".tmp");

            try (InputStream in = response.body();
                 OutputStream out = Files.newOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private static void unpackZip(Path zipFile, Path destDir) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path newPath = destDir.resolve(entry.getName()).normalize();
                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    if (newPath.getParent() != null) Files.createDirectories(newPath.getParent());
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

}