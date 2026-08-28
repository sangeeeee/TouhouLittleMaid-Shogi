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

Run one real Java search using the bundled Sunfish evaluation data:

```text
gradlew.bat :engine:engineSearch
```

The standalone search accepts optional Gradle properties such as
`-PsearchDepth=6`, `-PsearchMillis=5000`, `-PsearchNodes=100000`, and
`-PsearchSfen="..."`.

The self-test is implemented with only the Java standard library, so it can run
offline without downloading a test framework. It covers the engine lifecycle,
resource contracts, Sunfish-compatible base types, movement tables, SFEN
positions, incremental bitboards, exact Sunfish Zobrist values, lightweight
make/undo, check detection, legal move generation, drops, promotion, the
pawn-drop-mate rule, and verified start-position perft through depth five.

The direct engine API now reads Sunfish's complete optimized `eval.bin` feature
vector, uses a three-slot depth-preferred transposition table, and performs a
single-threaded iterative-deepening Alpha-Beta search with quiescence, move
ordering, fourfold-repetition draws, principal variations, and hard time/node/
cancellation limits. It remains independent of Minecraft.
Resources may be supplied either as ordinary files for standalone tools or as
classpath streams. The mod uses the latter so its evaluation and book data stay
inside the distributable JAR and never need to be extracted to `config`.

This is the correctness-oriented search baseline. Sunfish's advanced pruning,
parallel search, perpetual-check adjudication, and opening-book selection are
left for later stages.

## License and attribution

This Java engine includes an implementation ported from the
[Sunfish4](https://github.com/sunfish-shogi/sunfish4) source code. Sunfish4 is
distributed under the MIT License, Copyright (c) 2015 Ryosuke Kubo. The
original copyright notice and full license text are stored in
[`SUNFISH.txt`](../src/main/resources/META-INF/licenses/tlm_shogi/SUNFISH.txt)
and included in the distributable mod JAR.
