# 東方リトルメイド：将棋（Touhou Little Maid Shogi）

[简体中文](README.md) | [English](README_en.md) | [日本語](README_ja.md)

**[Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid)** に、メイドと対局できる日本将棋盤を追加するアドオンMODです。

## 対応プラットフォーム

純Java版のSunfishエンジンを内蔵し、プレイヤーのクライアント上のバックグラウンドワーカーで動作します。Minecraft 1.21.1とJava 21を実行できるクライアントであれば、プラットフォームを問わず対局できます。専用サーバーは対局状態のみを管理し、探索処理は行いません。

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
tlm_shogi-1.0.0-beta-neoforge+mc1.21.1.jar
```

## ライセンスと第三者ソフトウェア表記

内蔵Java将棋エンジンには、[Sunfish4](https://github.com/sunfish-shogi/sunfish4) のソースコードを基に移植した実装が含まれています。Sunfish4はRyosuke Kubo氏により開発され、MIT Licenseの下で公開されています：Copyright (c) 2015 Ryosuke Kubo。元の著作権表示とライセンス全文は [SUNFISH.txt](src/main/resources/META-INF/licenses/tlm_shogi/SUNFISH.txt) に収録され、ビルドされたMODのJARにも同梱されます。

## クレジット

- TartaricAcidとTouhou Little Maidチーム——メイドフレームワークの提供
- Ryosuke Kubo氏およびSunfishのコントリビューター——将棋エンジン、評価データ、ルール実装の参考
- MODのテストやメイドとの対局に参加してくださった皆様
