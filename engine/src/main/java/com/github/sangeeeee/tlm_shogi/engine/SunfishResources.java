package com.github.sangeeeee.tlm_shogi.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Locations and lightweight validation for Sunfish evaluation and opening-book data. */
public record SunfishResources(Path evalFile, Path bookFile) {
    public static final String EXPECTED_EVAL_VERSION = "2018.05.29.0";

    public SunfishResources {
        Objects.requireNonNull(evalFile, "evalFile");
        Objects.requireNonNull(bookFile, "bookFile");
        evalFile = evalFile.toAbsolutePath().normalize();
        bookFile = bookFile.toAbsolutePath().normalize();
    }

    public static SunfishResources fromDirectory(Path directory) {
        Objects.requireNonNull(directory, "directory");
        return new SunfishResources(directory.resolve("eval.bin"), directory.resolve("book.bin"));
    }

    public SunfishResourceInfo inspect() throws EngineException {
        requireReadableFile(evalFile, "evaluation data");
        requireReadableFile(bookFile, "opening book");

        try {
            String version = readEvalVersion(evalFile);
            if (!EXPECTED_EVAL_VERSION.equals(version)) {
                throw new EngineException(
                        "Unsupported eval.bin version: " + version + "; expected " + EXPECTED_EVAL_VERSION
                );
            }

            String firstBookLine;
            try (BufferedReader reader = Files.newBufferedReader(bookFile, StandardCharsets.UTF_8)) {
                firstBookLine = reader.readLine();
            }
            if (firstBookLine == null || !firstBookLine.startsWith("sfen ")) {
                throw new EngineException("book.bin does not start with a Sunfish SFEN entry");
            }

            return new SunfishResourceInfo(version, Files.size(evalFile), Files.size(bookFile));
        } catch (IOException e) {
            throw new EngineException("Failed to inspect Sunfish resources", e);
        }
    }

    private static String readEvalVersion(Path path) throws IOException, EngineException {
        try (InputStream input = Files.newInputStream(path)) {
            int length = input.read();
            if (length < 1 || length > 31) {
                throw new EngineException("eval.bin contains an invalid version length: " + length);
            }
            byte[] versionBytes = input.readNBytes(length);
            if (versionBytes.length != length) {
                throw new EngineException("eval.bin ended before its version header was complete");
            }
            return new String(versionBytes, StandardCharsets.US_ASCII);
        }
    }

    private static void requireReadableFile(Path path, String description) throws EngineException {
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new EngineException("Missing or unreadable " + description + ": " + path);
        }
    }
}
