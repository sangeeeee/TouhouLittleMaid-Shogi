# 车万女仆：将棋（Touhou Little Maid Shogi）

[简体中文](README.md) | [English](README_en.md) | [日本語](README_ja.md)

这是一个为 **[车万女仆（Touhou Little Maid）](https://github.com/TartaricAcid/TouhouLittleMaid)** 制作的附属模组，加入了可以和女仆对弈的日本将棋棋盘。

## 平台支持

模组内置纯 Java Sunfish 引擎。引擎在玩家客户端的后台线程中运行；任何能够运行 Minecraft 1.21.1 与 Java 21 的客户端都可以进行将棋对局，专用服务器只负责维护对局状态，不承担搜索计算。

`eval.bin` 和 `book.bin` 均保留在模组 JAR 内，并在客户端初始化时直接通过资源流读入内存；模组不会为引擎向 `config` 解压或写入任何文件。搜索在玩家客户端的后台线程中完成，不会把计算压力转移到多人服务器。

## 构建

```powershell
.\gradlew.bat build
```

可分发的 JAR 位于 `build/libs`。还可以直接从该 JAR 加载内置数据并执行一次搜索验证：

```powershell
.\gradlew.bat verifyDistributableJar
```

## 独立诘将棋守方引擎

[`mateengine`](mateengine/README.md) 是一个不依赖 Minecraft 或评估文件的独立 Java
子项目。它接收轮到守方应将的 SFEN，允许进攻方省略王将，并在存在逃脱手时
优先逃脱；如果所有应手都会被诘，则返回能够将被诘手数拖得最长的应手。它目前作为
独立引擎和测试入口，不替换模组中的普通对局引擎。

## 许可证与第三方声明

内置 Java 将棋引擎包含基于 [Sunfish4](https://github.com/sunfish-shogi/sunfish4) 源码移植的实现。Sunfish4 由 Ryosuke Kubo 开发，以 MIT License 发布：Copyright (c) 2015 Ryosuke Kubo。原始版权声明与完整许可文本见 [SUNFISH.txt](src/main/resources/META-INF/licenses/tlm_shogi/SUNFISH.txt)，并会随构建出的模组 JAR 一同分发。

## 致谢

- TartaricAcid 与车万女仆团队——提供了优秀的女仆框架
- Ryosuke Kubo 与 Sunfish 贡献者——提供了将棋引擎、评估数据和规则实现参考
- 所有参与测试以及和女仆下将棋的玩家
