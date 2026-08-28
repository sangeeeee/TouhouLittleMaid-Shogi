package com.github.sangeeeee.tlm_shogi.api.game.jchess;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import com.github.sangeeeee.tlm_shogi.engine.EngineException;
import com.github.sangeeeee.tlm_shogi.engine.SunfishResources;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Extracts the platform-independent Sunfish data consumed by the Java engine. */
@OnlyIn(Dist.CLIENT)
public final class SunfishDataFiles {
    private static final String RESOURCE_PREFIX = "assets/tlm_shogi/sunfish/";
    private static final Path TARGET_DIRECTORY = FMLPaths.CONFIGDIR.get()
            .resolve("touhou_little_maid")
            .resolve("shogi_engine")
            .resolve("sunfish4-java-" + SunfishResources.EXPECTED_EVAL_VERSION);

    private static final List<BundledFile> FILES = List.of(
            new BundledFile(
                    "eval.bin",
                    47_124_355L,
                    "3d547b73a0bee8a1b49dc76d1c278ee1fd7ac56ce33c233bfdc21f2d71ec1bea"
            ),
            new BundledFile(
                    "book.bin",
                    2_881_571L,
                    "301dff4b2e576e6977d85e85da9a77adf53842a37fdbe53d6dc623bfa679c59a"
            )
    );

    private SunfishDataFiles() {
    }

    /** Ensures both bundled files exist and returns their validated locations. */
    public static synchronized SunfishResources prepare() throws EngineException {
        try {
            Files.createDirectories(TARGET_DIRECTORY);
            boolean extracted = false;
            for (BundledFile file : FILES) {
                extracted |= extractIfMissingOrWrongSize(file);
            }

            SunfishResources resources = SunfishResources.fromDirectory(TARGET_DIRECTORY);
            resources.inspect();
            if (extracted) {
                TouhouLittleMaidShogi.LOGGER.info(
                        "Prepared Java Sunfish data in {}", TARGET_DIRECTORY.toAbsolutePath()
                );
            }
            return resources;
        } catch (IOException exception) {
            throw new EngineException(
                    "Failed to prepare bundled Java Sunfish data in " + TARGET_DIRECTORY.toAbsolutePath(),
                    exception
            );
        }
    }

    public static Path targetDirectory() {
        return TARGET_DIRECTORY;
    }

    private static boolean extractIfMissingOrWrongSize(BundledFile file) throws IOException {
        Path target = TARGET_DIRECTORY.resolve(file.name());
        if (Files.isRegularFile(target)
                && Files.size(target) == file.size()
                && file.sha256().equals(sha256(target))) {
            return false;
        }

        String resourcePath = RESOURCE_PREFIX + file.name();
        Path temporary = Files.createTempFile(TARGET_DIRECTORY, file.name() + '.', ".tmp");
        MessageDigest digest = newSha256Digest();
        try (InputStream resource = SunfishDataFiles.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (resource == null) {
                throw new IOException("Bundled resource is missing: " + resourcePath);
            }
            try (DigestInputStream input = new DigestInputStream(resource, digest)) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            long actualSize = Files.size(temporary);
            if (actualSize != file.size()) {
                throw new IOException(
                        "Bundled resource " + resourcePath + " has size " + actualSize
                                + "; expected " + file.size()
                );
            }
            String actualHash = HexFormat.of().formatHex(digest.digest());
            if (!file.sha256().equals(actualHash)) {
                throw new IOException(
                        "Bundled resource " + resourcePath + " has SHA-256 " + actualHash
                                + "; expected " + file.sha256()
                );
            }

            try {
                Files.move(
                        temporary,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String sha256(Path path) throws IOException {
        MessageDigest digest = newSha256Digest();
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0) {
                    digest.update(buffer, 0, count);
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest newSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("The Java runtime does not provide SHA-256", exception);
        }
    }

    private record BundledFile(String name, long size, String sha256) {
    }
}
