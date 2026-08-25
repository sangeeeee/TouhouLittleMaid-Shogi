package com.github.sangeeeee.tlm_shogi.api.game.jchess;

import java.util.Locale;

/**
 * Describes whether the bundled shogi engine can run on the current machine.
 *
 * <p>The bundled engine is a Windows executable, so merely finding the engine
 * files is not enough to consider the platform supported.</p>
 */
public final class ShogiEnginePlatform {
    private static final boolean SUPPORTED = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT)
            .startsWith("windows");

    private ShogiEnginePlatform() {
    }

    public static boolean isSupported() {
        return SUPPORTED;
    }
}
