package com.github.sangeeeee.tlm_shogi.datapack;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

/** Merges ordinary and masterpiece tsume catalogs while keeping their pools independent. */
public final class TsumeBoardStateReloadListener implements ResourceManagerReloadListener {
    private static final ResourceLocation ORDINARY_PATH = ResourceLocation.fromNamespaceAndPath(
            TouhouLittleMaidShogi.MOD_ID, "board_states/tsume.json");
    private static final ResourceLocation MASTERPIECE_PATH = ResourceLocation.fromNamespaceAndPath(
            TouhouLittleMaidShogi.MOD_ID, "board_states/tsume_masterpieces.json");
    private static final Gson GSON = new Gson();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        TsumeBoardStateData.clear();
        resourceManager.listPacks().forEach(pack -> {
            readPack(pack, ORDINARY_PATH, TsumeBoardStateData::addAll);
            readPack(pack, MASTERPIECE_PATH, TsumeBoardStateData::addAllMasterpieces);
        });
    }

    private void readPack(PackResources pack, ResourceLocation path,
                          Consumer<List<TsumeBoardStateRecord>> destination) {
        IoSupplier<InputStream> resource = pack.getResource(PackType.SERVER_DATA, path);
        if (resource == null) {
            return;
        }
        try (InputStream stream = resource.get();
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            List<TsumeBoardStateRecord> records = GSON.fromJson(reader, new TypeToken<List<TsumeBoardStateRecord>>() {
            }.getType());
            if (records != null) {
                destination.accept(records);
            }
        } catch (Exception exception) {
            TouhouLittleMaidShogi.LOGGER.error("Failed to load tsume-shogi board states from {}", path, exception);
        }
    }
}
