package com.github.sangeeeee.tlm_shogi.engine.search;

import com.github.sangeeeee.tlm_shogi.engine.core.Move;

import java.util.Arrays;
import java.util.Optional;

/**
 * Sunfish-style three-way, depth-preferred transposition table.
 *
 * <p>Primitive parallel arrays keep the Java heap cost predictable.  Bucket
 * sizing follows Sunfish's 64-byte bucket accounting even though the Java
 * representation is slightly smaller.</p>
 */
public final class TranspositionTable {
    public static final int SLOTS_PER_BUCKET = 3;
    public static final int UPPER = 1;
    public static final int LOWER = 2;
    public static final int EXACT = UPPER | LOWER;

    private static final int MINIMUM_BUCKETS = 1 << 8;
    private static final int ACCOUNTED_BUCKET_BYTES = 64;

    private long[] keys;
    private int[] moves;
    private short[] scores;
    private byte[] depths;
    private byte[] scoreTypes;
    private int bucketCount;
    private int bucketMask;
    private int configuredMiB;

    public TranspositionTable(int mebibytes) {
        resizeMiB(mebibytes);
    }

    public int configuredMiB() {
        return configuredMiB;
    }

    public int bucketCount() {
        return bucketCount;
    }

    public int capacity() {
        return Math.multiplyExact(bucketCount, SLOTS_PER_BUCKET);
    }

    public void resizeMiB(int mebibytes) {
        if (mebibytes < 1) throw new IllegalArgumentException("TT size must be at least 1 MiB");
        long bytes = Math.multiplyExact((long) mebibytes, 1024L * 1024L);
        long maximumBuckets = bytes / ACCOUNTED_BUCKET_BYTES;
        int newBucketCount = MINIMUM_BUCKETS;
        while ((long) newBucketCount * 2 <= maximumBuckets
                && (long) newBucketCount * 2 * SLOTS_PER_BUCKET <= Integer.MAX_VALUE) {
            newBucketCount *= 2;
        }
        if (newBucketCount == bucketCount) {
            configuredMiB = mebibytes;
            return;
        }

        int entries = Math.multiplyExact(newBucketCount, SLOTS_PER_BUCKET);
        keys = new long[entries];
        moves = new int[entries];
        scores = new short[entries];
        depths = new byte[entries];
        scoreTypes = new byte[entries];
        bucketCount = newBucketCount;
        bucketMask = newBucketCount - 1;
        configuredMiB = mebibytes;
    }

    public void clear() {
        Arrays.fill(keys, 0L);
        Arrays.fill(moves, 0);
        Arrays.fill(scores, (short) 0);
        Arrays.fill(depths, (byte) 0);
        Arrays.fill(scoreTypes, (byte) 0);
    }

    public Optional<Entry> probe(long hash, int ply) {
        int slot = find(hash);
        if (slot < 0) return Optional.empty();
        return Optional.of(new Entry(
                hash,
                scoreAt(slot, ply),
                depthAt(slot),
                scoreTypeAt(slot),
                moveAt(slot)
        ));
    }

    public StoreStatus store(long hash, int alpha, int beta, int score,
                             int depth, int ply, Move move) {
        int scoreType = score >= beta ? LOWER : score <= alpha ? UPPER : EXACT;
        int normalizedDepth = Math.max(0, Math.min(255, depth));
        int slot = find(hash);
        boolean update = slot >= 0;

        if (update) {
            if (normalizedDepth < depthAt(slot)
                    && score < SunfishScore.MATE && score > -SunfishScore.MATE) {
                return StoreStatus.REJECT;
            }
        } else {
            int first = firstSlot(hash);
            slot = first;
            for (int candidate = first + 1; candidate < first + SLOTS_PER_BUCKET; candidate++) {
                if (scoreTypes[candidate] == 0) {
                    slot = candidate;
                    break;
                }
                if (depthAt(candidate) < depthAt(slot)) slot = candidate;
            }
        }

        int storedScore = normalizeScoreForStorage(score, ply);
        keys[slot] = hash;
        if (!update || move != null && !move.isNone()) {
            moves[slot] = move == null ? Move.none().serialize16() : move.serialize16();
        }
        scores[slot] = (short) storedScore;
        depths[slot] = (byte) normalizedDepth;
        scoreTypes[slot] = (byte) scoreType;
        return update ? StoreStatus.UPDATE : StoreStatus.REPLACE;
    }

    /** Approximate occupancy of the first 10,000 buckets, matching Sunfish's diagnostic. */
    public float usageRate() {
        int sampledBuckets = Math.min(bucketCount, 10_000);
        int occupied = 0;
        int sampledSlots = sampledBuckets * SLOTS_PER_BUCKET;
        for (int slot = 0; slot < sampledSlots; slot++) {
            if (scoreTypes[slot] != 0) occupied++;
        }
        return sampledSlots == 0 ? 0.0f : (float) occupied / sampledSlots;
    }

    int find(long hash) {
        int first = firstSlot(hash);
        for (int slot = first; slot < first + SLOTS_PER_BUCKET; slot++) {
            if (scoreTypes[slot] != 0 && keys[slot] == hash) return slot;
        }
        return -1;
    }

    int scoreAt(int slot, int ply) {
        int score = scores[slot];
        int type = scoreTypeAt(slot);
        if (score >= SunfishScore.MATE && type == LOWER) return score - ply;
        if (score <= -SunfishScore.MATE && type == UPPER) return score + ply;
        return score;
    }

    int depthAt(int slot) {
        return Byte.toUnsignedInt(depths[slot]);
    }

    int scoreTypeAt(int slot) {
        return Byte.toUnsignedInt(scoreTypes[slot]);
    }

    Move moveAt(int slot) {
        return Move.deserialize16(moves[slot]);
    }

    private int firstSlot(long hash) {
        return ((int) hash & bucketMask) * SLOTS_PER_BUCKET;
    }

    private static int normalizeScoreForStorage(int score, int ply) {
        if (score >= SunfishScore.MATE) {
            return score < SunfishScore.INFINITY - ply ? score + ply : SunfishScore.INFINITY;
        }
        if (score <= -SunfishScore.MATE) {
            return score > -SunfishScore.INFINITY + ply ? score - ply : -SunfishScore.INFINITY;
        }
        return score;
    }

    public enum StoreStatus {
        UPDATE,
        REPLACE,
        REJECT
    }

    public record Entry(long hash, int score, int depth, int scoreType, Move move) {
        public Entry {
            move = move.excludeExtData();
        }
    }
}
