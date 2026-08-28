package com.github.sangeeeee.tlm_shogi.datapack;

import java.util.ArrayList;
import java.util.List;

/** Server-data-backed collection of tsume-shogi positions available to loot tables. */
public final class TsumeBoardStateData {
    private static final List<TsumeBoardStateRecord> RECORDS = new ArrayList<>();
    private static final List<TsumeBoardStateRecord> MASTERPIECES = new ArrayList<>();

    private TsumeBoardStateData() {
    }

    public static void clear() {
        RECORDS.clear();
        MASTERPIECES.clear();
    }

    public static void addAll(List<TsumeBoardStateRecord> records) {
        RECORDS.addAll(records);
    }

    public static List<TsumeBoardStateRecord> records() {
        return List.copyOf(RECORDS);
    }

    public static void addAllMasterpieces(List<TsumeBoardStateRecord> records) {
        MASTERPIECES.addAll(records);
    }

    public static List<TsumeBoardStateRecord> masterpieces() {
        return List.copyOf(MASTERPIECES);
    }
}
