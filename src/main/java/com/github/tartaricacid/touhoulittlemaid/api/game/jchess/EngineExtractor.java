package com.github.tartaricacid.touhoulittlemaid.api.game.jchess;


import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class EngineExtractor {
    public static final Path TARGET_DIR = Path.of(Minecraft.getInstance().gameDirectory.getAbsolutePath())
            .resolve("config")
            .resolve("touhou_little_maid")
            .resolve("shogi_engine");

    private static final String RESOURCE_PREFIX = "assets/touhou_little_maid/shogi_engine/";

    private static final List<String> FILES_TO_EXTRACT = List.of(
            "YaneuraOu_NNUE_halfkp_256x2_32_32-V900Git_AVX2.exe",
            "eval/nn.bin",
            "book/standard_book.db"
    );

    public static Path getEnginePath() {
        return TARGET_DIR.resolve("YaneuraOu_NNUE_halfkp_256x2_32_32-V900Git_AVX2.exe");
    }

    public static Path getEvalDir() {
        return TARGET_DIR.resolve("eval");
    }

    public static Path getBookPath() {
        return TARGET_DIR.resolve("book").resolve("standard_book.db");
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(EngineExtractor::extractIfNeeded);
    }

    public static void extractIfNeeded() {
        boolean allExist = FILES_TO_EXTRACT.stream()
                .map(file -> TARGET_DIR.resolve(file))
                .allMatch(Files::exists);

        if (allExist) {
            System.out.println("[TouhouLittleMaid] Shogi engine files already exist. Skipping extraction.");
            return;
        }

        try {
            Files.createDirectories(TARGET_DIR);
            for (String file : FILES_TO_EXTRACT) {
                extractSingleFile(file);
            }
            System.out.println("[TouhouLittleMaid] Shogi engine and evaluation files successfully extracted to:");
            System.out.println("[TouhouLittleMaid]   " + TARGET_DIR.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("[TouhouLittleMaid] Failed to extract shogi engine! AI features will be disabled.");
            System.err.println("[TouhouLittleMaid] Please report this issue with the log.");
            e.printStackTrace();
        }
    }

    private static void extractSingleFile(String relativePath) throws IOException {
        Path target = TARGET_DIR.resolve(relativePath);
        if (Files.exists(target)) {
            System.out.println("[TouhouLittleMaid] File already exists, skipping: " + relativePath);
            return;
        }

        String resourcePath = RESOURCE_PREFIX + relativePath;
        InputStream is = EngineExtractor.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IOException("Resource not found in JAR: " + resourcePath + " (Check case sensitivity and path)");
        }

        Files.createDirectories(target.getParent());
        Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        is.close();

        System.out.println("[TouhouLittleMaid] Extracted: " + relativePath);
    }
}