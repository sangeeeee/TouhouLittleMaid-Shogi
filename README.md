# 车万女仆：将棋（Touhou Little Maid Shogi）

[简体中文](README.md) | [English](README_en.md) | [日本語](README_ja.md)

这是一个为 **[车万女仆（Touhou Little Maid）](https://github.com/TartaricAcid/TouhouLittleMaid)** 制作的附属模组，加入了可以和女仆对弈的日本将棋棋盘。

## 平台支持

模组现在使用直接内置的纯 Java Sunfish 引擎，不再解压或调用 Windows `.exe`。只要客户端能够运行 Minecraft 1.21.1 所需的 Java 21，就可以进行将棋对局；专用服务器不会执行搜索任务。

客户端首次运行时会把 `eval.bin` 和 `book.bin` 数据释放到 `config/touhou_little_maid/shogi_engine/sunfish4-java-2018.05.29.0`。搜索在玩家客户端的后台线程中完成，不会把计算压力转移到多人服务器。

## 构建

```powershell
.\gradlew.bat build
```

## 致谢

- TartaricAcid 与车万女仆团队——提供了优秀的女仆框架
- Ryosuke Kubo 与 Sunfish 贡献者——提供了将棋引擎、评估数据和规则实现参考
- 所有参与测试以及和女仆下将棋的玩家
