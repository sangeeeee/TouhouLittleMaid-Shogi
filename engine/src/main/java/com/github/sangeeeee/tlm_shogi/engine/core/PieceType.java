package com.github.sangeeeee.tlm_shogi.engine.core;

import java.util.List;

/** Sunfish-compatible piece type value, including the original raw encoding. */
public record PieceType(int raw) {
    public static final int PROMOTION = 0x08;
    public static final int WHITE_FLAG = 0x10;
    public static final int EMPTY_FLAG = 0x20;
    public static final int HAND_MASK = 0x27;
    public static final int TYPE_MASK = 0x2f;

    public static final PieceType PAWN = new PieceType(0);
    public static final PieceType LANCE = new PieceType(1);
    public static final PieceType KNIGHT = new PieceType(2);
    public static final PieceType SILVER = new PieceType(3);
    public static final PieceType GOLD = new PieceType(4);
    public static final PieceType BISHOP = new PieceType(5);
    public static final PieceType ROOK = new PieceType(6);
    public static final PieceType KING = new PieceType(7);
    public static final PieceType TOKIN = new PieceType(8);
    public static final PieceType PRO_LANCE = new PieceType(9);
    public static final PieceType PRO_KNIGHT = new PieceType(10);
    public static final PieceType PRO_SILVER = new PieceType(11);
    public static final PieceType HORSE = new PieceType(13);
    public static final PieceType DRAGON = new PieceType(14);
    public static final PieceType EMPTY = new PieceType(32);

    public static final int TYPE_END = 15;
    public static final int HAND_END = 7;

    private static final List<PieceType> TYPES = List.of(
            PAWN, LANCE, KNIGHT, SILVER, GOLD, BISHOP, ROOK, KING,
            TOKIN, PRO_LANCE, PRO_KNIGHT, PRO_SILVER, HORSE, DRAGON
    );

    public PieceType {
        if (raw < 0 || raw > 0xff) {
            throw new IllegalArgumentException("piece type raw value must fit in an unsigned byte: " + raw);
        }
    }

    public boolean isEmpty() {
        return raw == EMPTY.raw;
    }

    public PieceType hand() {
        return new PieceType(raw & HAND_MASK);
    }

    public PieceType promote() {
        return new PieceType(raw | PROMOTION);
    }

    public PieceType unpromote() {
        return new PieceType(raw & ~PROMOTION);
    }

    public boolean isPromoted() {
        return (raw & PROMOTION) != 0;
    }

    public boolean isPromotable() {
        return raw <= ROOK.raw && raw != GOLD.raw;
    }

    public Piece black() {
        return new Piece(raw);
    }

    public Piece white() {
        return new Piece(raw | WHITE_FLAG);
    }

    public PieceType next() {
        return new PieceType(raw == PRO_SILVER.raw ? raw + 2 : raw + 1);
    }

    public PieceType nextUnsafe() {
        return new PieceType(raw + 1);
    }

    public String toCsa() {
        return switch (raw) {
            case 0 -> "FU";
            case 1 -> "KY";
            case 2 -> "KE";
            case 3 -> "GI";
            case 4 -> "KI";
            case 5 -> "KA";
            case 6 -> "HI";
            case 7 -> "OU";
            case 8 -> "TO";
            case 9 -> "NY";
            case 10 -> "NK";
            case 11 -> "NG";
            case 13 -> "UM";
            case 14 -> "RY";
            case 32 -> "  ";
            default -> Integer.toString(raw);
        };
    }

    public static PieceType parseCsa(String value) {
        if (value == null) {
            return EMPTY;
        }
        for (PieceType type : TYPES) {
            if (value.startsWith(type.toCsa())) {
                return type;
            }
        }
        return EMPTY;
    }

    @Override
    public String toString() {
        return toCsa();
    }
}
