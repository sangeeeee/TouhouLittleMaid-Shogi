# Touhou Little Maid Shogi

[简体中文](README.md) | [English](README_en.md) | [日本語](README_ja.md)

An add-on for **[Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid)** that adds a Japanese shogi board for playing against maids.

## Platform Support

The mod now embeds a pure-Java Sunfish engine and no longer extracts or invokes a Windows `.exe`. Shogi matches are available on any client platform capable of running the Java 21 runtime required by Minecraft 1.21.1. Dedicated servers do not perform engine searches.

Both `eval.bin` and `book.bin` remain inside the mod JAR and are read directly into memory through resource streams during client initialization. The engine does not extract or write any files under `config`. Search runs on a client background worker and does not move its computational load to multiplayer servers.

## Build

```powershell
.\gradlew.bat build
```

The packaged resources and one engine search can be verified directly from the distributable JAR:

```powershell
.\gradlew.bat verifyDistributableJar
```

The built JAR is placed in `build/libs`:

```text
tlm_shogi-1.0.0-neoforge+mc1.21.1.jar
```

## Credits

- TartaricAcid and the Touhou Little Maid team—for the maid framework
- Ryosuke Kubo and the Sunfish contributors—for the shogi engine, evaluation data, and rules reference
- Everyone who tested the mod and played shogi with their maids
