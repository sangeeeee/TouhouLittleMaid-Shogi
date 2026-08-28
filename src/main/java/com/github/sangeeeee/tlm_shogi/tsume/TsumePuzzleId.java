package com.github.sangeeeee.tlm_shogi.tsume;

import com.github.sangeeeee.tlm_shogi.engine.core.Position;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

/** Stable identity for a board, side-to-move, and both hands; the SFEN move number is ignored. */
public final class TsumePuzzleId {
    private TsumePuzzleId() {
    }

    public static String fromSfen(String sfen) {
        // Canonicalizing also makes equivalent hand-order spellings share one reward record.
        String[] fields = Position.parse(sfen).toSfen().split("\\s+");
        if (fields.length < 3) {
            throw new IllegalArgumentException("SFEN must contain board, turn, and hand fields");
        }
        String position = String.join(" ", Arrays.copyOf(fields, 3));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(position.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Every Java runtime must provide SHA-256", exception);
        }
    }
}
