package com.github.sangeeeee.tlm_shogi.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Locations and lightweight validation for Sunfish evaluation and opening-book data. */
public final class SunfishResources {
    public static final String EXPECTED_EVAL_VERSION = "2018.05.29.0";
    public static final long EXPECTED_EVAL_BYTES = 47_124_355L;
    public static final long EXPECTED_BOOK_BYTES = 2_881_571L;

    private final Path evalFile;
    private final Path bookFile;
    private final ClassLoader resourceLoader;
    private final String evalResource;
    private final String bookResource;

    /** Creates a filesystem-backed resource pair for standalone engine use. */
    public SunfishResources(Path evalFile, Path bookFile) {
        this.evalFile = Objects.requireNonNull(evalFile, "evalFile").toAbsolutePath().normalize();
        this.bookFile = Objects.requireNonNull(bookFile, "bookFile").toAbsolutePath().normalize();
        resourceLoader = null;
        evalResource = null;
        bookResource = null;
    }

    private SunfishResources(ClassLoader resourceLoader, String evalResource, String bookResource) {
        evalFile = null;
        bookFile = null;
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader");
        this.evalResource = Objects.requireNonNull(evalResource, "evalResource");
        this.bookResource = Objects.requireNonNull(bookResource, "bookResource");
    }

    public static SunfishResources fromDirectory(Path directory) {
        Objects.requireNonNull(directory, "directory");
        return new SunfishResources(directory.resolve("eval.bin"), directory.resolve("book.bin"));
    }

    /**
     * Creates resources read directly from a class path or mod JAR.
     *
     * <p>The directory uses forward slashes and may optionally start or end
     * with one. No files are copied to the filesystem.</p>
     */
    public static SunfishResources fromClasspath(ClassLoader loader, String directory) {
        Objects.requireNonNull(loader, "loader");
        Objects.requireNonNull(directory, "directory");
        String normalized = directory.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        String prefix = normalized.isEmpty() ? "" : normalized + '/';
        return new SunfishResources(loader, prefix + "eval.bin", prefix + "book.bin");
    }

    /** Returns the evaluation file for filesystem-backed resources. */
    public Path evalFile() {
        if (evalFile == null) {
            throw new IllegalStateException("The evaluation data is backed by a classpath resource, not a file");
        }
        return evalFile;
    }

    /** Returns the opening-book file for filesystem-backed resources. */
    public Path bookFile() {
        if (bookFile == null) {
            throw new IllegalStateException("The opening book is backed by a classpath resource, not a file");
        }
        return bookFile;
    }

    /** Opens a fresh evaluation-data stream. The caller must close it. */
    public InputStream openEval() throws EngineException {
        return open(evalFile, evalResource, "evaluation data");
    }

    /** Opens a fresh opening-book stream. The caller must close it. */
    public InputStream openBook() throws EngineException {
        return open(bookFile, bookResource, "opening book");
    }

    public long evalBytes() throws EngineException {
        return byteSize(evalFile, EXPECTED_EVAL_BYTES, "evaluation data");
    }

    public long bookBytes() throws EngineException {
        return byteSize(bookFile, EXPECTED_BOOK_BYTES, "opening book");
    }

    public String evalDescription() {
        return describe(evalFile, evalResource);
    }

    public String bookDescription() {
        return describe(bookFile, bookResource);
    }

    public SunfishResourceInfo inspect() throws EngineException {
        if (evalFile != null) {
            requireReadableFile(evalFile, "evaluation data");
            requireReadableFile(bookFile, "opening book");
        }

        long inspectedEvalBytes = evalBytes();
        long inspectedBookBytes = bookBytes();
        try {
            String version;
            try (InputStream input = openEval()) {
                version = readEvalVersion(input);
            }
            if (!EXPECTED_EVAL_VERSION.equals(version)) {
                throw new EngineException(
                        "Unsupported eval.bin version: " + version + "; expected " + EXPECTED_EVAL_VERSION
                );
            }

            String firstBookLine;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(openBook(), StandardCharsets.UTF_8))) {
                firstBookLine = reader.readLine();
            }
            if (firstBookLine == null || !firstBookLine.startsWith("sfen ")) {
                throw new EngineException("book.bin does not start with a Sunfish SFEN entry");
            }

            return new SunfishResourceInfo(version, inspectedEvalBytes, inspectedBookBytes);
        } catch (IOException e) {
            throw new EngineException("Failed to inspect Sunfish resources", e);
        }
    }

    private InputStream open(Path file, String resource, String description) throws EngineException {
        if (file != null) {
            try {
                return Files.newInputStream(file);
            } catch (IOException exception) {
                throw new EngineException("Unable to open " + description + ": " + file, exception);
            }
        }

        InputStream input = resourceLoader.getResourceAsStream(resource);
        if (input == null) {
            throw new EngineException("Missing bundled " + description + ": classpath:/" + resource);
        }
        return input;
    }

    private static long byteSize(Path file, long bundledSize, String description) throws EngineException {
        if (file == null) {
            return bundledSize;
        }
        try {
            return Files.size(file);
        } catch (IOException exception) {
            throw new EngineException("Unable to read the size of " + description + ": " + file, exception);
        }
    }

    private static String describe(Path file, String resource) {
        return file != null ? file.toString() : "classpath:/" + resource;
    }

    private static String readEvalVersion(InputStream input) throws IOException, EngineException {
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

    private static void requireReadableFile(Path path, String description) throws EngineException {
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new EngineException("Missing or unreadable " + description + ": " + path);
        }
    }
}
