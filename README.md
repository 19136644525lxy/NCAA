# NCAA

A Fabric mod for Minecraft that forces players into survival mode and prevents switching to creative mode.

> **本分支（`yfapi`）改动**：配置内核、刻调度改为依赖 [YFCoreLib](https://github.com/19136644525lxy/YFCorelib)，
> 在线玩家遍历改用库的 `ServerAccess`（不再自己订阅服务端生命周期事件），
> 并清理了模板遗留的空壳 Mixin。行为与 0.1.x 一致，安装时**必须同时装 YFCoreLib**。

## Features

- Forces players to survival mode upon entering the world
- Blocks all creative mode switches via commands, F3+F4 shortcuts, etc.
- Shows customizable warning message when players attempt to switch modes
- Configuration file generated in `.minecraft/config/ncaa.json`
- Mod Menu integration with dedicated configuration interface

## Requirements

- Minecraft 1.20.1
- Fabric Loader >= 0.16.12
- Fabric API
- **YFCoreLib >= 0.2.0**（本分支起必需）
- Mod Menu (for configuration UI)
- Cloth Config API (for configuration UI)

## Configuration

配置文件是 `.minecraft/config/ncaa.json`，与旧版本**同一个文件、同样的键名**，所以升级不需要迁移：

```json
{
  "modEnabled": true,
  "message": "",
  "__version": 1
}
```

- `modEnabled`：是否启用强制生存模式
- `message`：被拉回生存时发给玩家的提示，留空使用语言文件里的默认文案
- `__version`：由 YFCoreLib 维护的格式版本号，用于将来做配置迁移，不要手改

## Building

需要 **JDK 21**（Fabric Loom 1.14 的要求），Gradle 由 wrapper 自动下载：

```bash
./gradlew build
```

产物在 `build/libs/`。

本仓库 `libs/` 下自带一份 YFCoreLib 的生产 jar，所以 clone 下来即可直接构建；
若你同时检出了 YFCoreLib 仓库并按 `../YFCoreLib/fabric/build/libs/` 的布局放在一起，
构建会优先使用那份最新产物，便于联调。

## Usage

1. Install all required dependencies（含 YFCoreLib）
2. Place the mod JAR in your `.minecraft/mods` folder
3. Launch Minecraft
4. (Optional) Configure the mod via Mod Menu

## License

MIT License
