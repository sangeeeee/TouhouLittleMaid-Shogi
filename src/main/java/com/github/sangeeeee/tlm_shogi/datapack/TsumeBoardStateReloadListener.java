package com.github.sangeeeee.tlm_shogi.datapack;

import com.github.sangeeeee.tlm_shogi.TouhouLittleMaidShogi;
import com.github.tartaricacid.touhoulittlemaid.datapack.pojo.BoardStateRecord;
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

/** Merges every data pack's {@code tlm_shogi:board_states/tsume.json}. */
public final class TsumeBoardStateReloadListener implements ResourceManagerReloadListener {
    private static final ResourceLocation PATH = ResourceLocation.fromNamespaceAndPath(
            TouhouLittleMaidShogi.MOD_ID, "board_states/tsume.json");
    private static final Gson GSON = new Gson();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        TsumeBoardStateData.clear();
        resourceManager.listPacks().forEach(this::readPack);
    }

    private void readPack(PackResources pack) {
        IoSupplier<InputStream> resource = pack.getResource(PackType.SERVER_DATA, PATH);
        if (resource == null) {
            return;
        }
        try (InputStream stream = resource.get();
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            List<BoardStateRecord> records = GSON.fromJson(reader, new TypeToken<List<BoardStateRecord>>() {
            }.getType());
            if (records != null) {
                TsumeBoardStateData.addAll(records);
            }
        } catch (Exception exception) {
            TouhouLittleMaidShogi.LOGGER.error("Failed to load tsume-shogi board states from {}", PATH, exception);
        }
    }
}
