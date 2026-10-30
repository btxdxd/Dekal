package btxds.dekal.util;

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

public class HashChecker {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static String getRemoteHash(String hashUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(hashUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body().trim();
            }
        } catch (Exception e) {
            System.err.println("Failed to retrieve the hash from the server: " + e.getMessage());
        }
        return "";
    }

    public static String getLocalHash(Path filePath) {
        if (!Files.exists(filePath)) return "";

        try (InputStream is = Files.newInputStream(filePath)) {
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