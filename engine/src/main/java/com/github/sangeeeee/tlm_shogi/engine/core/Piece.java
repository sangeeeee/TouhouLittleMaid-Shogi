package com.github.sangeeeee.tlm_shogi.engine.core;

import java.util.List;

/** Sunfish-compatible colored piece value. */
public record Piece(int raw) {
    public static final Piece BLACK_PAWN = new Piece(0);
    public static final Piece BLACK_LANCE = new Piece(1);
    public static final Piece BLACK_KNIGHT = new Piece(2);
    public static final Piece BLACK_SILVER = new Piece(3);
    public static final Piece BLACK_GOLD = new Piece(4);
    public static final Piece BLACK_BISHOP = new Piece(5);
    public static final Piece BLACK_ROOK = new Piece(6);
    public static final Piece BLACK_KING = new Piece(7);
    public static final Piece BLACK_TOKIN = new Piece(8);
    public static final Piece BLACK_PRO_LANCE = new Piece(9);
    public static final Piece BLACK_PRO_KNIGHT = new Piece(10);
    public static final Piece BLACK_PRO_SILVER = new Piece(11);
    public static final Piece BLACK_HORSE = new Piece(13);
    public static final Piece BLACK_DRAGON = new Piece(14);

    public static final Piece WHITE_PAWN = new Piece(16);
    public static final Piece WHITE_LANCE = new Piece(17);
    public static final Piece WHITE_KNIGHT = new Piece(18);
    public static final Piece WHITE_SILVER = new Piece(19);
    public static final Piece WHITE_GOLD = new Piece(20);
    public static final Piece WHITE_BISHOP = new Piece(21);
    public static final Piece WHITE_ROOK = new Piece(22);
    public static final Piece WHITE_KING = new Piece(23);
    public static final Piece WHITE_TOKIN = new Piece(24);
    public static final Piece WHITE_PRO_LANCE = new Piece(25);
    public static final Piece WHITE_PRO_KNIGHT = new Piece(26);
    public static final Piece WHITE_PRO_SILVER = new Piece(27);
    public static final Piece WHITE_HORSE = new Piece(29);
    public static final Piece WHITE_DRAGON = new Piece(30);
    public static final Piece EMPTY = new Piece(32);

    public static final int END = 31;

    private static final List<Piece> PIECES = List.of(
            BLACK_PAWN, BLACK_LANCE, BLACK_KNIGHT, BLACK_SILVER, BLACK_GOLD,
            BLACK_BISHOP, BLACK_ROOK, BLACK_KING, BLACK_TOKIN, BLACK_PRO_LANCE,
            BLACK_PRO_KNIGHT, BLACK_PRO_SILVER, BLACK_HORSE, BLACK_DRAGON,
            WHITE_PAWN, WHITE_LANCE, WHITE_KNIGHT, WHITE_SILVER, WHITE_GOLD,
            WHITE_BISHOP, WHITE_ROOK, WHITE_KING, WHITE_TOKIN, WHITE_PRO_LANCE,
            WHITE_PRO_KNIGHT, WHITE_PRO_SILVER, WHITE_HORSE, WHITE_DRAGON
    );

    public Piece {
        if (raw < 0 || raw > 0xff) {
            throw new IllegalArgumentException("piece raw value must fit in an unsigned byte: " + raw);
        }
    }

    public boolean isEmpty() {
        return raw == PieceType.EMPTY_FLAG;
    }

    public PieceType hand() {
        return new PieceType(raw & PieceType.HAND_MASK);
    }

    public PieceType type() {
        return new PieceType(raw & PieceType.TYPE_MASK);
    }

    public Piece promote() {
        return new Piece(raw | PieceType.PROMOTION);
    }

    public Piece unpromote() {
        return new Piece(raw & ~PieceType.PROMOTION);
    }

    public boolean isPromoted() {
        return (raw & PieceType.PROMOTION) != 0;
    }

    public boolean isPromotable() {
        return type().isPromotable();
    }

    public Piece black() {
        return new Piece(raw & ~PieceType.WHITE_FLAG);
    }

    public Piece white() {
        return new Piece(raw | PieceType.WHITE_FLAG);
    }

    public Piece enemy() {
        return new Piece(raw ^ PieceType.WHITE_FLAG);
    }

    public boolean isBlack() {
        return (raw & (PieceType.EMPTY_FLAG | PieceType.WHITE_FLAG)) == 0;
    }

    public boolean isWhite() {
        return (raw & PieceType.WHITE_FLAG) != 0;
    }

    public Turn turn() {
        if (isBlack()) {
            return Turn.BLACK;
        }
        if (isWhite()) {
            return Turn.WHITE;
        }
        throw new IllegalStateException("empty or invalid piece has no turn");
    }

    public Piece next() {
        if (raw == 11 || raw == 14 || raw == 27) {
            return new Piece(raw + 2);
        }
        return new Piece(raw + 1);
    }

    public String toCsa() {
        if (isEmpty()) {
            return "   ";
        }
        String typeName = type().toCsa();
        if (typeName.length() != 2) {
            return Integer.toString(raw);
        }
        return (isWhite() ? "-" : "+") + typeName;
    }

    public String toSfen() {
        if (isEmpty()) {
            return "";
        }
        String symbol = switch (type().unpromote().raw()) {
            case 0 -> "P";
            case 1 -> "L";
            case 2 -> "N";
            case 3 -> "S";
            case 4 -> "G";
            case 5 -> "B";
            case 6 -> "R";
            case 7 -> "K";
            default -> null;
        };
        if (symbol == null) {
            return Integer.toString(raw);
        }
        if (isWhite()) {
            symbol = symbol.toLowerCase(java.util.Locale.ROOT);
        }
        return isPromoted() ? "+" + symbol : symbol;
    }

    public static Piece parseCsa(String value) {
        if (value == null) {
            return EMPTY;
        }
        for (Piece piece : PIECES) {
            if (value.startsWith(piece.toCsa())) {
                return piece;
            }
        }
        return EMPTY;
    }

    public static Piece parseSfen(String value) {
        if (value == null) {
            return EMPTY;
        }
        for (Piece piece : PIECES) {
            String sfen = piece.toSfen();
            if (!sfen.isEmpty() && value.startsWith(sfen)) {
                return piece;
            }
        }
        return EMPTY;
    }

    @Override
    public String toString() {
        return toCsa();
    }
}
