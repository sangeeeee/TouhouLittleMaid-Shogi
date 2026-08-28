# Touhou Little Maid Shogi

[简体中文](README.md) | [English](README_en.md) | [日本語](README_ja.md)

An add-on for **[Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid)** that adds a Japanese shogi board for playing against maids.

## Platform Support

The mod now embeds a pure-Java Sunfish engine and no longer extracts or invokes a Windows `.exe`. Shogi matches are available on any client platform capable of running the Java 21 runtime required by Minecraft 1.21.1. Dedicated servers do not perform engine searches.

On first launch, the client extracts only `eval.bin` and `book.bin` data to `config/touhou_little_maid/shogi_engine/sunfish4-java-2018.05.29.0`. Search runs on a client background worker and does not move its computational load to multiplayer servers.

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
- Ryosuke Kubo and the Sunfish contributors—for the shogi engine, evaluation data, and rules reference
- Everyone who tested the mod and played shogi with their maids
