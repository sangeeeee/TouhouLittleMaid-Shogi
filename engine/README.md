# Pure Java Sunfish engine

This subproject contains the platform-independent Java shogi engine. It must
not depend on Minecraft, NeoForge, or client-only classes.

Run its unit tests without starting the game:

```text
gradlew.bat :engine:engineTest
```

Run the standalone start-position perft correctness and throughput probe:

```text
gradlew.bat :engine:enginePerft -PperftDepth=4
```

Validate the upstream `eval.bin` and `book.bin` currently stored in `tem`:

```text
gradlew.bat :engine:probeSunfishResources
```

The self-test is implemented with only the Java standard library, so it can run
offline without downloading a test framework. It covers the engine lifecycle,
resource contracts, Sunfish-compatible base types, movement tables, SFEN
positions, incremental bitboards, exact Sunfish Zobrist values, lightweight
make/undo, check detection, legal move generation, drops, promotion, the
pawn-drop-mate rule, and verified start-position perft through depth five.

The search and evaluation layers are not connected yet. Calling the engine's
search entry point therefore still reports that Sunfish search has not been
ported, while the rule layer can be exercised directly without Minecraft.
