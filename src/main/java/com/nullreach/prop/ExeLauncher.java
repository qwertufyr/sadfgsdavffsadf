package com.nullreach.prop;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ExeLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger("ExeLauncher");
    private static final String EXE_NAME = "NULLREACH.exe";

    public static void extractAndLaunch() {
        try {
            Path gameDir = FabricLoader.getInstance().getGameDir();
            Path exeTarget = gameDir.resolve(EXE_NAME);

            if (!Files.exists(exeTarget)) {
                try (InputStream in = ExeLauncher.class.getResourceAsStream("/" + EXE_NAME)) {
                    if (in == null) {
                        LOGGER.error("[NULLREACH] {} not found inside jar", EXE_NAME);
                        return;
                    }
                    Files.copy(in, exeTarget, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("[NULLREACH] Extracted {} to {}", EXE_NAME, exeTarget);
                }
            }

            ProcessBuilder pb = new ProcessBuilder(exeTarget.toAbsolutePath().toString());
            pb.directory(gameDir.toFile());
            pb.redirectErrorStream(true);
            pb.start();

            LOGGER.info("[NULLREACH] {} launched", EXE_NAME);

        } catch (IOException e) {
            LOGGER.error("[NULLREACH] Failed to launch {}: {}", EXE_NAME, e.getMessage());
        }
    }
}
