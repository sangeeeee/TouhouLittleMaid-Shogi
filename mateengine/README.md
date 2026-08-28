# Defensive mate engine

`mateengine` is a standalone Java subproject for choosing an optimal defense
in a tsume-shogi position. It depends only on the existing `engine` project and
does not load Minecraft, NeoForge, `eval.bin`, or `book.bin`.

## Position contract

- Input is one four-field SFEN string.
- The side to move is the engine/defending side.
- The defending side must have exactly one king and must currently be in check.
- The attacking side may have either zero or one king.
- At attacker nodes only legal checking moves are considered, matching the
  continuous-check rule of tsume-shogi.
- The attacker chooses the shortest forced mate; the defender chooses an escape
  when one exists, otherwise the longest forced-mate line. Any tied defense may
  be returned.
- Mate distance is counted in plies from the supplied position, including the
  engine's root defense.
- A repeated search-path position is treated as an escape rather than a finite
  forced mate.

The result explicitly distinguishes `ESCAPE`, `FORCED_MATE`, `UNKNOWN`, and
`CANCELLED`. A move is only mathematically optimal when the outcome is proven;
`UNKNOWN` means a configured search limit was reached first.

## Run without Minecraft

```powershell
.\gradlew.bat :mateengine:mateEngineTest
```

```powershell
.\gradlew.bat :mateengine:mateSearch `
  -PmateSfen="4B2R1/3G3k1/7S1/6L2/9/9/9/9/9 w - 1" `
  -PmateMaxPly=63 `
  -PmateMaxNodes=2000000 `
  -PmateMillis=10000
```

The selected move is written to standard output in USI notation. Search status,
mate distance, node count, and principal variation are written to standard
error.
