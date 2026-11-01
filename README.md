# NCAA

A Fabric mod for Minecraft that forces players into survival mode and prevents switching to creative mode.

> **本分支（`yfapi`）改动**：配置内核、刻调度改为依赖 [YFCoreLib](https://github.com/19136644525lxy/YFCorelib)，
> 在线玩家遍历改用库的 `ServerAccess`（不再自己订阅服务端生命周期事件），
> 并清理了模板遗留的空壳 Mixin。行为与 0.1.x 一致，安装时**必须同时装 YFCoreLib**。
>
> **0.2.0 起的行为调整**：
> - **旁观模式放行**——只限制创造与冒险，旁观不再被拽回生存；
> - **先提示、后切回**：检测到违规先在动作栏提示，`graceSeconds` 秒后才切回，不再瞬间拽人；
> - **从源头拦截**：新加 Mixin 拦 `GameModeCommand`，受限玩家执行 `/gamemode creative|adventure` 直接报错，
>   根本进不去创造（不再"先给你创造，下一秒再抢回来"）；
> - **管理员白名单 / 黑名单**：白名单里的管理员可用所有模式；黑名单里的管理员保留其他权限，
>   只是不能切换游戏模式（详见下方"管理员名单"）。

## Features

- Forces players to survival mode upon entering the world
- Blocks creative/adventure mode switches **at the command level** (Mixin on `GameModeCommand`) 
- Spectator mode is explicitly allowed
- Operators (permission level ≥ 2) can be exempted
- Shows a warning first, then switches back after a configurable grace period
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
  "exemptOperators": true,
  "graceSeconds": 2,
  "adminWhitelist": "yifei, 3f0a9c1e-9b0d-4c1a-8f2b-6d5e7a8b9c0d",
  "adminBlacklist": "Helper1, Helper2",
  "__version": 1
}
```

- `modEnabled`：是否启用强制生存模式
- `message`：被拉回生存时发给玩家的提示，留空使用语言文件里的默认文案
- `exemptOperators`：**未列入任何名单**的管理员（权限等级 ≥ 2）是否不受限制，默认 `true`
- `graceSeconds`：检测到违规后先提示多少秒再切回，默认 `2`（界面里可调 1–10 秒）
- `adminWhitelist` / `adminBlacklist`：管理员白名单与黑名单，见下一节
- `__version`：由 YFCoreLib 维护的格式版本号，用于将来做配置迁移，不要手改

> 新增的键由 YFCoreLib 自动补默认值，老配置文件不需要手改。

## 管理员名单

用白名单/黑名单区别对待管理员：

| 情况 | 能否切换创造/冒险 | 其他 OP 权限 |
| --- | --- | --- |
| 在黑名单里 | ❌ 不能 | ✅ 全部保留 |
| 在白名单里 | ✅ 可以 | ✅ 全部保留 |
| 两个名单都没有（管理员） | 由 `exemptOperators` 决定（默认 ✅ 可以） | ✅ 全部保留 |
| 普通玩家 | ❌ 不能 | —（本来也没有） |

- **黑名单优先**：同时出现在两个名单里按黑名单处理（显式拒绝 > 显式允许）。
- 一条记录可以写**玩家名**（不区分大小写）或 **UUID**；多条用英文逗号、分号、空格或换行分隔，
  例如 `"yifei, Helper1, 3f0a9c1e-9b0d-4c1a-8f2b-6d5e7a8b9c0d"`。
- 用 UUID 更稳（改名不影响）；用名字更方便，改名后记得同步改名单。
- 被黑名单拦下时提示会说明"其他权限照旧，只是不能切模式"，避免管理员误以为被封了。
- 名单改动后需要重载配置（Mod Menu 里点保存，或重进世界）才生效。

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
