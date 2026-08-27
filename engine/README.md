# Pure Java Sunfish engine

This subproject contains the platform-independent Java shogi engine. It must
not depend on Minecraft, NeoForge, or client-only classes.

Run its unit tests without starting the game:

```text
gradlew.bat :engine:engineTest
```

Validate the upstream `eval.bin` and `book.bin` currently stored in `tem`:

```text
gradlew.bat :engine:probeSunfishResources
```

The self-test is implemented with only the Java standard library, so it can run
offline without downloading a test framework. It covers the engine lifecycle,
resource contracts, and the Sunfish-compatible base types (`Turn`, pieces,
squares, moves, hands, and 81-bit/rotated bitboards). Rule generation and search
are intentionally reported as not yet implemented until their Java ports are
added and verified against Sunfish.
