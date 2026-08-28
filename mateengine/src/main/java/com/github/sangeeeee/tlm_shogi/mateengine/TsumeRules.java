package com.github.sangeeeee.tlm_shogi.mateengine;

import com.github.sangeeeee.tlm_shogi.engine.core.Move;
import com.github.sangeeeee.tlm_shogi.engine.core.Position;

/** Small, non-searching rule queries shared by tsume gameplay and the mate engine. */
public final class TsumeRules {
    private TsumeRules() {
    }

    /** Returns whether the side to move has at least one legal move that gives check. */
    public static boolean hasLegalCheckingMove(String sfen) {
        return hasLegalCheckingMove(Position.parse(sfen));
    }

    public static boolean hasLegalCheckingMove(Position position) {
        for (Move move : position.legalMoves()) {
            if (position.isCheck(move)) {
                return true;
            }
        }
        return false;
    }
}
