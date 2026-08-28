package com.github.sangeeeee.tlm_shogi.mateengine;

/** What optimal defense can achieve from the supplied checked position. */
public enum MateSearchOutcome {
    /** The selected move escapes every forced continuous-check mate. */
    ESCAPE,
    /** Every legal defense is mated; the selected move maximizes the mate distance. */
    FORCED_MATE,
    /** The configured depth, node, or time limit prevented a proof. */
    UNKNOWN,
    /** An explicit cancellation request stopped the search. */
    CANCELLED
}
