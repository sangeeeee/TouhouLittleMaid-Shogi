# 東方リトルメイド：将棋（Touhou Little Maid Shogi）

[简体中文](README.md) | [English](README_en.md) | [日本語](README_ja.md)

**[Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid)** に、メイドと対局できる日本将棋盤を追加するアドオンMODです。

## 対応プラットフォーム

現在は純Java版のSunfishエンジンを内蔵しており、Windows用の `.exe` の展開・実行は行いません。Minecraft 1.21.1に必要なJava 21を実行できるクライアントであれば、プラットフォームを問わず対局できます。専用サーバー側では探索処理を実行しません。

`eval.bin` と `book.bin` はMODのJAR内に保持され、クライアント初期化時にリソースストリームから直接メモリへ読み込まれます。エンジン用ファイルを `config` 以下へ展開・書き込みすることはありません。探索はクライアントのバックグラウンドワーカーで実行され、マルチプレイサーバーに計算負荷を移しません。

## ビルド

```powershell
.\gradlew.bat build
```

配布用JARから内蔵データを読み込み、探索を1回実行する検証も可能です。

```powershell
.\gradlew.bat verifyDistributableJar
```

ビルドされたJARは `build/libs` に出力されます。

```text
tlm_shogi-1.0.0-neoforge+mc1.21.1.jar
```

## クレジット

- TartaricAcidとTouhou Little Maidチーム——メイドフレームワークの提供
- Ryosuke Kubo氏およびSunfishのコントリビューター——将棋エンジン、評価データ、ルール実装の参考
- MODのテストやメイドとの対局に参加してくださった皆様
