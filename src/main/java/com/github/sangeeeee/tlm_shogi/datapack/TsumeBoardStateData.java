package com.github.sangeeeee.tlm_shogi.datapack;

import com.github.tartaricacid.touhoulittlemaid.datapack.pojo.BoardStateRecord;

import java.util.ArrayList;
import java.util.List;

/** Server-data-backed collection of tsume-shogi positions available to loot tables. */
public final class TsumeBoardStateData {
    private static final List<BoardStateRecord> RECORDS = new ArrayList<>();

    private TsumeBoardStateData() {
    }

    public static void clear() {
        RECORDS.clear();
    }

    public static void addAll(List<BoardStateRecord> records) {
        RECORDS.addAll(records);
    }

    public static List<BoardStateRecord> records() {
        return List.copyOf(RECORDS);
    }
}
