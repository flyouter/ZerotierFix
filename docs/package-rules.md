# 包结构设计文档与迁移记录

> 规范正文已迁移到 [`.claude/rules/`](../.claude/rules/)，Claude Code 会自动加载。
> 本文件只保留设计背景与迁移历史，不再作为规则来源。

- **状态**：v4（2026-10-23，非 UI Kotlin 化完成）
- **适用范围**：`app/src/main/kotlin/net/kaaass/zerotierfix`（非 UI）、`app/src/main/java/net/kaaass/zerotierfix`（UI）、`core` 模块，以及后续新增模块

---

## 1. 为什么不是全局 `data/domain/ui` 三层

大型 Android 开源项目的实际做法：

| 项目 | 组织方式 |
|---|---|
| Signal-Android | 单模块，功能/子系统优先：`conversation/`、`conversationlist/`、`groups/`、`media/`，旁边是 `database/`、`net/`、`service/`、`events/`、`util/` |
| Element Android | `features/<feature>/` + `core/<capability>/` |
| Wikipedia Android | 单模块，功能优先：`feed/`、`search/`、`page/`，旁边是 `database/`、`dataclient/`、`model/`、`util/` |
| AntennaPod | `ui/screen/<feature>/` + `storage/`、`network/`、`model/`、`event/`、`util/` |
| Now in Android | 多模块：`feature/*` + `core/{data,database,domain,model,network,ui}` |
| Bitwarden | 多模块：`app`、`ui`、`data`、`network`、`core`、`authenticator` |
| Thunderbird | 多模块：`feature/*` + `core` + `mail` + `backend` |

结论：单模块项目按功能纵切；多模块项目的“层”是 Gradle 模块边界，不是单个模块里的全局 `data/`、`domain/`、`ui/` 目录。本仓库采用前者。

---

## 2. 迁移历史

### 2.1 目录迁移（2026-10-23）

| 原位置 | 新位置 |
|---|---|
| `data/local/entity/*` | `data/database/entity/*` |
| `data/local/ZTOpenHelper`、`DatabaseUtils` | `data/database/` |
| `domain/model/enums/*` | `model/enums/*` |
| `domain/model/dto` | `model/dto` |
| `domain/service/node/*` | `vpn/node/*` |
| `domain/service/tun/*` | `vpn/tun/*` |
| `domain/service/route/*` | `vpn/route/*` |
| `domain/service/store/*` | `vpn/store/*` |
| `domain/service/vpn/ZeroTierOneService` | `service/ZeroTierOneService` |
| `domain/service/vpn/StartupReceiver` | `service/StartupReceiver` |
| `ui/networklist/*` | `features/networklist/*` |
| `ui/join/*` | `features/join/*` |
| `ui/detail/*` | `features/detail/*` |
| `ui/moon/*` | `features/moon/*` |
| `ui/peer/*` | `features/peer/*` |
| `ui/settings/*` | `features/settings/*` |
| `ui/common/*` | `ui/common/*` |
| `events/*` | `events/*` |
| `util/*` | `util/*` |

### 2.2 Repository 收口（2026-10-23）

| 项 | 结果 |
|---|---|
| `NetworkRepository` / Impl | 网络列表、详情、加入、lastActivated、名称、默认路由、删除 |
| `AppNodeRepository` / Impl | 本机节点 ID 读写 |
| `MoonOrbitRepository` / Impl | Moon 列表、添加、删除、缓存文件 |
| `model/dto` | `NetworkSummary`、`NetworkConfigDto`、`JoinNetworkRequest`、`MoonOrbitDto` |
| 调用点迁移 | `NetworkListFragment`、`JoinNetworkFragment`、`NetworkDetailModel/Fragment`、`MoonOrbitFragment`、`ZeroTierOneService` |
| 事件 | 删除 `DefaultRouteChangedEvent`；`NetworkListCheckedChangeEvent` 不再携带 `SwitchCompat`；事件不再携带实体 |
| 依赖入口 | 移除 `ZerotierFixApplication.getDaoSession()`，统一经 `AppContainer` 获取 Repository |

### 2.3 事件与枚举清理（2026-10-23）

| 项 | 结果 |
|---|---|
| 事件 DTO 化 | `NetworkListReplyEvent`、`NodeStatusEvent`、`PeerInfoReplyEvent`、`VirtualNetworkConfigChangedEvent`、`VirtualNetworkConfigReplyEvent`、`NetworkReconfigureEvent` 全部改用 DTO |
| 新 DTO | `VirtualNetworkConfigDto`、`VirtualNetworkDnsDto`、`NodeStatusDto`、`PeerInfoDto` |
| SDK 映射 | `vpn/config/VirtualNetworkConfigMapper`、`vpn/node/NodeInfoMapper` |
| 枚举清理 | 枚举不再引用 `R` / SDK；新增 `PeerRole`；`NetworkType` / `NetworkStatus` / `PeerRole` 不再持久化，移除无用的 `fromInt` / `toInt`，仅 `DNSMode` 保留 |
| 文案映射 | `ui/common/EnumTextFormatter` |
| 废弃事件 | 删除 `ErrorEvent`、`JoinNetworkEvent`、`LeaveNetworkEvent` |
| Service 拆分 | 抽出 `vpn/config/DnsConfigurator`、`service/VpnNotificationController`，`ZeroTierOneService` 降至约 980 行 |

### 2.4 合并 dev 与 Room 适配（2026-10-23）

`dev` 分支已通过 `chore/deps-upgrade` 完成 Gradle 9.8.2 / AGP 9.4.1 / JDK 17 升级，并用 Room 替换 GreenDAO。本次将 `dev` 合并进重构分支，并做如下适配：

| 项 | 结果 |
|---|---|
| 数据层 | Room 实体移到 `data/database/entity`，DAO 移到 `data/database/dao`，`AppDatabase` 放在 `data/database` |
| 迁移工具 | 删除 GreenDAO 的 `ZTOpenHelper`；旧库由 Room `fallbackToDestructiveMigration` 重建 |
| Repository | 三个 Repository 实现改为基于 `AppDatabase` + Room DAO |
| 依赖入口 | `AppContainer` 创建 `AppDatabase`，页面/Service 仍只依赖 Repository |
| 工具链 | 采用 dev 的 Gradle 9.8.2 / AGP 9.4.1 / compileSdk 37 / minSdk 29 |
| Schema | 导出 schema 移到 `app/schemas/net.kaaass.zerotierfix.data.database.AppDatabase/` |
| 业务代码 | UI、Service、事件、DTO 保持本次重构结果；dev 的 Room 适配改动由 Repository 替代 |

### 2.5 Kotlin 迁移（2026-10-23）

在保持业务逻辑、线程模型、锁/事务边界和 Room schema 不变的前提下，将全部非 UI 代码改写为 Kotlin。

| 项 | 结果 |
|---|---|
| 工具链 | 启用 AGP 9.4.1 内置 Kotlin（KGP 2.2.10），未引入 `kotlin-android` / `kapt`；Room 编译器由 `annotationProcessor` 迁至 KSP `2.2.10-2.0.2`；新增 `android.disallowKotlinSourceSets=false` 以兼容 KSP 的 source set 注入 |
| 源集 | 非 UI Kotlin 位于 `app/src/main/kotlin`；UI（`features/**`、`ui/**`）继续留在 `app/src/main/java` |
| 迁移范围 | `model`、`events`、`util`、`data`、`vpn`、`service`、`di`、`ZerotierFixApplication`，共 77 个有代码文件 |
| Java 互操作 | 工具类用 `object` + `@JvmStatic`；常量用 `const val`；DTO/事件布尔属性命名为 `isXxx`；`AppDatabase.getInstance`、`ZeroTierOneService.ZT1_NETWORK_ID`、`ZeroTierBinder` 等 Java 调用形态保持不变 |
| Room | 实体保留字段名、可空性与无参构造器；DAO 单条查询返回可空类型；`app/schemas/.../24.json` 零变更，`identityHash` 仍为 `5147b0c3923106a52f5d8395e56a2c8a` |
| 锁与事务 | Repository 的 `lock()` / `try` / `finally`、事务边界、`runInTransaction` 提前返回语义逐字保留 |
| Service | `ZeroTierOneService` 直接迁移后为 1099 行，超过 1000 行硬上限；将两个组播扫描线程等价抽到 `service/MulticastScanner.kt`，服务降至 969 行 |
| 提交划分 | 5 个阶段提交：model/events → util → data+KSP → vpn → service/di/Application，每个提交均可独立构建 |
| 遗留 Java | 仅 `features/**`、`ui/**` 共 19 个 UI 文件与 5 个 `package-info.java` |

### 2.6 待完成

1. **运行期验证**：VPN 启停、TUN 数据面、ARP/NDP、EventBus 线程调度、通知、开机广播，以及旧版本覆盖安装的数据库兼容性，均待真机/模拟器验证。
2. **继续瘦身 `ZeroTierOneService`**：当前 969 行；`TunnelConfigurator` 是候选拆分点，拆分保持现有事务/线程边界。
3. **`vpn` 内部类的方法 KDoc** 仍有缺口。
4. **测试**：优先覆盖 `util`（地址/路由/IP 包）、ARP/NDP、Repository。
5. **UI 迁移（可选）**：`features/**`、`ui/**` 仍为 Java；迁移保持现有 Java 互操作约束。
