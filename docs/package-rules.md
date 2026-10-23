# 包结构设计文档与迁移记录

> 规范正文已迁移到 [`.claude/rules/`](../.claude/rules/)，Claude Code 会自动加载。
> 本文件只保留设计背景与迁移历史，不再作为规则来源。

- **状态**：v5（2026-10-23，UI 迁移到 Compose + Material 3 完成）
- **适用范围**：`app/src/main/kotlin/net/kaaass/zerotierfix`、`core` 模块，以及后续新增模块

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

### 2.6 UI 迁移：Java View → Compose + Material 3（2026-10-23）

一次性破坏性替换，不保留旧 UI、EventBus 或兼容层：

| 项 | 结果 |
|---|---|
| 架构 | 单 Activity（`MainActivity`）+ Navigation Compose；字符串路由 `network_list` / `join` / `detail/{networkId}` / `moon` / `peer` / `settings` |
| 页面 | 6 个 feature 全部改为 `XxxRoute` + `XxxScreen` + `XxxViewModel` + `XxxUiState`；`@Preview` 覆盖 Screen |
| 状态 | 单向数据流：不可变 UiState、`StateFlow`、`SharedFlow<Effect>`、`collectAsStateWithLifecycle`、`LifecycleResumeEffect` |
| 服务通信 | 删除 EventBus 与 `events/`；新增 `vpn/engine/VpnStateStore`（Service 写、UI 读）与 `VpnServiceCommands` 命令接口 |
| 命令执行 | `service/AndroidVpnServiceCommands` 串行调度、按需绑定、`Mutex` 保护；不使用 `BIND_AUTO_CREATE` |
| 绑定解耦 | `vpn/node`、`vpn/tun` 改依赖 `VpnRuntime`；`vpn/config/TunnelConfigurator` 通过 `TunnelHost` 获得平台能力 |
| DI | 删除 `di/AppContainer`；改用 Hilt（`DatabaseModule`、`RepositoryModule`、`AppModule`、`DispatcherModule`） |
| 设置 | 新增 `SettingsRepository` / Impl，封装 SharedPreferences 并暴露 `StateFlow<Settings>`；移除 `androidx.preference` |
| 主题 | `Theme.Material3.DayNight.NoActionBar`；Compose `ZerotierFixTheme` 沿用原紫色/青色/绿色；删除 `attrs.xml`、`values-night/themes.xml` |
| 资源 | 删除 `layout/`、`menu/`、`xml/preferences.xml`；`strings.xml` / `values-zh` / `colors.xml` 保留 |
| 依赖 | 删除 EventBus、Lombok、appcompat、constraintlayout、navigation-fragment/ui、preference、recyclerview、swiperefreshlayout |
| 源集 | `app/src/main/java` 清空；全部代码位于 `app/src/main/kotlin` |
| Service | 移除 EventBus 与 `AppContainer`；写入 `VpnStateStore`；补充失败路径的 `STOPPED`；`stopZeroTier` 改为非阻塞幂等；抽出 `TunnelConfigurator` 后约 901 行 |
| 行为修复 | 拒绝 VPN 授权后开关不置位；Moon/Peer 订阅随 ViewModel 结束；停止操作在后台线程执行；Join 表单状态在旋转后保留 |

明确保留的历史行为：`default_route_enabled_networks` 只写不读、`StartupReceiver` 只记录日志、列表“最后一个已连接网络生效”、禁用 IPv6 时详情页地址显示字面量 `"null"`。

### 2.7 待完成

1. **运行期验证**：VPN 启停、TUN 数据面、ARP/NDP、Flow 线程调度、通知、开机广播，以及旧版本覆盖安装的数据库兼容性，均待真机/模拟器验证。
2. **继续瘦身 `ZeroTierOneService`**：当前约 901 行；剩余拆分点与不拆理由见 `.claude/rules/03-service-and-methods.md`。
3. **`vpn` 内部类的方法 KDoc** 仍有少量缺口。
4. **测试**：已补 `VpnStateStoreTest`；后续优先覆盖 `util`（地址/路由/IP 包）、ARP/NDP、Repository、ViewModel。
5. **Compose UI 测试**：建议先覆盖 NetworkList 与 Join。
