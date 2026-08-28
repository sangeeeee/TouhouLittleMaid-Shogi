package com.github.sangeeeee.tlm_shogi.datapack;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Data-pack representation of one tsume puzzle and its move limit. */
public record TsumeBoardStateRecord(
        @SerializedName("tags") List<String> tags,
        @SerializedName("display") Display display,
        @SerializedName("data") String data,
        @SerializedName("maximum_ply") int maximumPly,
        @SerializedName("weight") int weight) {

    public TsumeBoardStateRecord {
        tags = List.copyOf(tags);
        if (maximumPly < 1 || (maximumPly & 1) == 0) {
            throw new IllegalArgumentException("Tsume maximum_ply must be a positive odd number");
        }
    }

    public record Display(
            @SerializedName("description") String description,
            @SerializedName("author") String author) {
    }
}
