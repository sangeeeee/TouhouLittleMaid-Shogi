package com.github.sangeeeee.tlm_shogi.engine.tool;

import com.github.sangeeeee.tlm_shogi.engine.SunfishResourceInfo;
import com.github.sangeeeee.tlm_shogi.engine.SunfishResources;
import com.github.sangeeeee.tlm_shogi.engine.search.SunfishEvaluator;

import java.nio.file.Path;

/** Command-line resource check that does not load or start Minecraft. */
public final class SunfishResourceProbe {
    private SunfishResourceProbe() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: SunfishResourceProbe <resource-directory>");
        }

        SunfishResources resources = SunfishResources.fromDirectory(Path.of(args[0]));
        SunfishResourceInfo info = resources.inspect();
        SunfishEvaluator evaluator = SunfishEvaluator.load(resources);

        System.out.println("Sunfish resources are valid.");
        System.out.println("eval version: " + info.evalVersion());
        System.out.println("eval bytes: " + info.evalBytes());
        System.out.println("eval weights: " + evaluator.weightCount());
        System.out.println("book bytes: " + info.bookBytes());
    }
}
