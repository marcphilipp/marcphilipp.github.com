package de.marcphilipp.website;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS;

public class TalksPostProcessorTests {

    @Test
    void writesTargetYamlFile(@TempDir(cleanup = ON_SUCCESS) Path tempDir) throws IOException {
        var talksYml = tempDir.resolve("talks.yml");
        try (var in = getClass().getResourceAsStream("/talks.yml")) {
            Files.copy(requireNonNull(in), talksYml);
        }

        Path imageDir = Files.createDirectory(tempDir.resolve("img"));
        Path targetYamlFile = tempDir.resolve("talks_processed.yml");

        new TalksPostProcessor().process(talksYml, tempDir, imageDir, targetYamlFile);

        assertTrue(Files.exists(targetYamlFile));
    }

}
