# 東方リトルメイド：将棋（Touhou Little Maid Shogi）

[简体中文](README.md) | [English](README_en.md) | [日本語](README_ja.md)

**[Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid)** に、メイドと対局できる日本将棋盤を追加するアドオンMODです。

## 対応プラットフォーム

同梱のYaneuraOuエンジンはWindows用の `.exe` であるため、現在、将棋をプレイできるのは **Windowsクライアントのみ**です。

Linux、macOSなどでもMODの導入、将棋盤の設置、メイドの着席は可能ですが、対局はできず、将棋盤は装飾ブロックとしてのみ使用できます。非対応環境ではエンジンのリソースは展開されず、駒を動かそうとすると非対応プラットフォームであることが表示されます。対応状況は各プレイヤーのクライアント側で判定されるため、専用サーバー自体はどのプラットフォームでも動作できます。

Windowsクライアントで初めて起動した際、エンジンと評価リソースが `config/touhou_little_maid/shogi_engine` に展開されます。`.exe` を展開して実行するため、ウイルス対策ソフトが誤検知する場合があります。例外に追加する前に、ご自身のセキュリティーポリシーに従ってファイルを確認してください。

## ビルド

```powershell
.\gradlew.bat build
```

ビルドされたJARは `build/libs` に出力されます。

```text
tlm_shogi-1.0.0-neoforge+mc1.21.1.jar
```

## クレジット

- TartaricAcidとTouhou Little Maidチーム——メイドフレームワークの提供
- YaneuraOuの作者およびコントリビューター——オープンソース将棋エンジンの提供
- MODのテストやメイドとの対局に参加してくださった皆様
