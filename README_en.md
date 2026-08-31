# Touhou Little Maid Shogi

[简体中文](README.md) | [English](README_en.md) | [日本語](README_ja.md)

An add-on for **[Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid)** that adds a placeable Japanese shogi board. Players can play complete games against their maids or collect puzzle items and challenge tsume-shogi positions.

## Mod Content

### Japanese Shogi

The board is crafted at the Touhou Little Maid altar with:

- 1 log of any kind
- 2 planks of any kind
- 1 black dye
- 1 red dye
- 1 diamond
- 0.1 altar power

Placing the board requires a row of three free blocks. Once a maid is seated opposite the player, the player can begin as Black. The Touhou Little Maid board-game setting determines whether players may only challenge their own maids.

Play with an empty hand:

1. Right-click one of your pieces, or a piece stack on the komadai, to select it.
2. Right-click the destination square to move or drop it.
3. A choice screen appears when promotion is optional; mandatory promotion happens automatically.
4. After the player moves, the maid thinks and responds automatically.
5. Right-click either reset area beside the board stand with an empty hand to restore the initial position.

The player always moves first. The board can still be placed and displayed without a maid, but a game cannot continue until a maid is seated.

### Tsume-Shogi Puzzle Item

Each puzzle item stores an initial position, a move limit, and author information. Hold `Shift` while viewing its tooltip to preview the board and both players' pieces in hand. Right-clicking a shogi board with the item immediately replaces any current position with the puzzle. The item is reusable and is not consumed.

The player always attacks first. When a maid is seated, she controls the defending side and tries to escape or delay checkmate for as long as possible.

#### What is tsume-shogi?

Tsume-shogi is a composed Japanese shogi problem whose sole objective is a forced checkmate within the stated number of plies. It differs from ordinary chess, xiangqi, and shogi endgame studies:

- Every attacking move must give check, and the final move must checkmate.
- The solution must work against every legal defense, not merely win material or obtain a favorable position.
- Positions are composed as puzzles; the attacking king may even be omitted.
- Pieces in hand can be dropped back onto the board and are often essential to the solution.
- The stated move count includes moves by both sides. A 3-ply problem is normally “check, defense, checkmate.”

A solution is marked incorrect if the move limit is reached without mate, or if the player reaches a turn with no legal checking move. Ordinary puzzles, enchanted masterpiece variants, and special long-form puzzles all use the same loading and play controls.

## Finding Puzzle Items

Naturally generated tsume-shogi items can be found in:

- Village cartographer-house chests
- Stronghold library chests

The position, move limit, and author are stored on the item. Special variants can appear in the same locations and do not require visiting a different structure.

## Maid Favorability

- Defeating a maid you own in a normal game grants the standard Touhou Little Maid board-game win reward.
- The first correct solution of an ordinary tsume-shogi position grants the same base reward.
- Masterpiece puzzles grant three times the base reward, while *Microcosmos* grants five times the base reward.
- Puzzle completion is tracked per player and per initial position. Solving the same position again does not grant favorability, even with a different maid.
- Multiplayer servers keep separate completion records for every player. All rewards still follow Touhou Little Maid's normal board-game reward cooldown.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.215 or a newer compatible version
- Touhou Little Maid 1.4.1 or newer
- Java 21

The mod must be installed on both the client and server. Its pure-Java shogi engine works on Windows, Linux, macOS, and other platforms capable of running the required Minecraft version. In multiplayer, move searches run on the player's client.

## License

This mod is distributed under the **MIT License**.

The embedded Java shogi engine includes an implementation ported from [Sunfish4](https://github.com/sunfish-shogi/sunfish4), developed by Ryosuke Kubo and released under the MIT License: Copyright (c) 2015 Ryosuke Kubo. The original notice and complete license text are available in [SUNFISH.txt](src/main/resources/META-INF/licenses/tlm_shogi/SUNFISH.txt) and are included in the distributed mod JAR.
