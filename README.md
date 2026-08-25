# 车万女仆：将棋（Touhou Little Maid Shogi）

[简体中文](README.md) | [English](README_en.md) | [日本語](README_ja.md)

这是一个为 **[车万女仆（Touhou Little Maid）](https://github.com/TartaricAcid/TouhouLittleMaid)** 制作的附属模组，加入了可以和女仆对弈的日本将棋棋盘。

## 平台支持

由于内置的 YaneuraOu 引擎是 Windows `.exe` 程序，目前只有 **Windows 客户端**可以进行将棋对局。

Linux、macOS 等其他平台仍可正常加载模组、放置棋盘并让女仆入座，但玩家无法下棋，棋盘仅作为装饰方块使用。这些平台不会解压引擎资源，玩家尝试下棋时会收到平台不支持提示。平台支持情况按照玩家客户端判断，因此专用服务器本身可以运行在任意平台。

Windows 客户端首次运行时会将引擎及评估资源释放到 `config/touhou_little_maid/shogi_engine`。由于模组会解压并运行 `.exe` 文件，部分杀毒软件可能误报，请根据自己的安全策略检查后再决定是否加入白名单。

## 构建

```powershell
.\gradlew.bat build
```

## 致谢

- TartaricAcid 与车万女仆团队——提供了优秀的女仆框架
- YaneuraOu 作者及贡献者——提供了优秀的开源将棋引擎
- 所有参与测试以及和女仆下将棋的玩家
