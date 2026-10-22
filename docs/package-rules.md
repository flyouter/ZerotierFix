# 包结构设计文档与迁移记录

> 规范正文已迁移到 [`.claude/rules/`](../.claude/rules/)，Claude Code 会自动加载。
> 本文件只保留设计背景与迁移历史，不再作为规则来源。

- **状态**：v3（2026-10-23，feature-first 单模块结构）
- **适用范围**：`app/src/main/java/net/kaaass/zerotierfix`、`core` 模块，以及后续新增模块

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
| 枚举清理 | 枚举不再引用 `R` / SDK；新增 `PeerRole`；修正 `NetworkType.fromInt`（0→UNKNOWN、1→PRIVATE、2→PUBLIC） |
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

### 2.5 待完成

1. **继续瘦身 `ZeroTierOneService`**：可抽 `TunnelConfigurator`、多播扫描线程；保持现有事务/线程边界。
2. **补齐 `vpn` 内部类的方法 Javadoc**。
3. **补测试**：优先覆盖 `util`（地址/路由/IP 包）、ARP/NDP、Repository。
