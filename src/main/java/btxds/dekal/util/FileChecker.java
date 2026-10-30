package btxds.dekal.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

public class FileChecker {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static boolean isLauncherValid(Path jarPath, String hashUrl) {
        if (!Files.exists(jarPath)) return false;
        String remoteHash = getRemoteHash(hashUrl);
        if (remoteHash.isEmpty()) return false;
        return calculateSha256(jarPath).equalsIgnoreCase(remoteHash);
    }

    public static boolean isJreValid(Path runtimeDir, String hashUrl) {
        Path javaw = runtimeDir.resolve("bin").resolve(System.getProperty("os.name").toLowerCase().contains("win") ? "javaw.exe" : "java");
        Path hashFile = runtimeDir.resolve(".hash");

        if (!Files.exists(javaw) || !Files.exists(hashFile)) return false;

        try {
            String localHash = Files.readString(hashFile).trim();
            String remoteHash = getRemoteHash(hashUrl);
            return !remoteHash.isEmpty() && localHash.equalsIgnoreCase(remoteHash);
        } catch (IOException e) {
            return false;
        }
    }

    public static void saveJreHash(Path runtimeDir, String hash) throws IOException {
        Files.createDirectories(runtimeDir);
        Files.writeString(runtimeDir.resolve(".hash"), hash.trim());
    }

    public static String getRemoteHash(String hashUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(hashUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? response.body().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String calculateSha256(Path file) {
        if (!Files.exists(file)) return "";
        try (InputStream is = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            return "";
        }
    }

}