# Touhou Little Maid Shogi

[简体中文](README.md) | [English](README_en.md) | [日本語](README_ja.md)

An add-on for **[Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid)** that adds a Japanese shogi board for playing against maids.

## Platform Support

The bundled YaneuraOu engine is a Windows `.exe`, so shogi matches are currently available only on **Windows clients**.

The mod can still be loaded on Linux, macOS, and other platforms. Players can place the board and let a maid sit at it, but cannot play; the board works only as decoration. Engine resources are not extracted on unsupported platforms, and players receive an unsupported-platform message when attempting to move. Support is determined by each player's client, so a dedicated server may run on any platform.

On its first Windows launch, the mod extracts the engine and evaluation resources to `config/touhou_little_maid/shogi_engine`. Because the mod extracts and runs an `.exe`, some antivirus programs may report a false positive. Review the file according to your own security policy before adding an exception.

## Build

```powershell
.\gradlew.bat build
```

The built JAR is placed in `build/libs`:

```text
tlm_shogi-1.0.0-neoforge+mc1.21.1.jar
```

## Credits

- TartaricAcid and the Touhou Little Maid team—for the maid framework
- The YaneuraOu authors and contributors—for the open-source shogi engine
- Everyone who tested the mod and played shogi with their maids
