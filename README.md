# 车万女仆：将棋（Touhou Little Maid Shogi）

这是一个为 **[车万女仆（Touhou Little Maid）](https://github.com/TartaricAcid/TouhouLittleMaid)** 制作的附属模组，  
新增了**日本将棋（Shogi）**！

### 重要限制
由于使用的 YaneuraOu 引擎为原生编译的 Windows 可执行程序，  
**本模组目前仅支持以下平台**：

**仅支持 Windows 10/11 64位系统 + 支持 AVX2 的 CPU**  
（几乎所有 2015 年之后的 Intel/AMD CPU 都支持 AVX2，2013 年之前的旧机器大概率不支持）

不支持的情况：
- macOS
- Linux
- ARM 架构（如苹果 M1/M2、Android等）
- 过老的 Intel CPU（如 2012 年以前的 i3/i5/i7）

在不支持的平台上加载本模组不会导致崩溃，但无法与女仆对局，将棋盘会仅成为装饰性方块。

因为模组运行时会自动解压并运行一个 `.exe` 可执行文件（YaneuraOu 引擎本体），**部分杀毒软件可能会误报**。  
目前测试未发现报警，但不排除个别杀软报“未知发布者”或“行为监控”警告。  
该文件来自公开的 YaneuraOu 官方发布版，可放心加入白名单。

### 致谢
- TartaricAcid & 车万女仆团队 —— 提供了优秀的女仆框架
- YaneuraOu 作者 —— 世界最强的开源将棋引擎之一
- 所有测试与女仆下将棋的玩家（