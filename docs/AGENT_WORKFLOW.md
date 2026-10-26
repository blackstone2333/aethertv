# AetherTV Agent 工作和交接规则

这份规则把 Goldilocks 的对齐、最小充分验证和持续交接落到 AetherTV。它不能替代代码审查、构建或设备验证。

## 命名

沿用上游历史中的 `AERIAL` 前缀，避免断开已有交接链：

| 对象 | 格式 | 示例 |
|---|---|---|
| 工作单元 | `AERIAL-YYYYMMDD-<slug>` | `AERIAL-20261026-roadmap-phase2` |
| 工作包 | `docs/work/<unit-id>.md` | `docs/work/AERIAL-20261026-roadmap-phase2.md` |
| 交接包 | `docs/handoffs/<unit-id>-handoff.md` | `docs/handoffs/AERIAL-20261026-roadmap-phase2-handoff.md` |
| 分支 | `codex/aerial/<unit-id-lowercase>` | `codex/aerial/aerial-20261026-roadmap-phase2` |
| 提交标题 | `<unit-id>: <verb>` | `AERIAL-20261026-roadmap-phase2: add live contracts` |

状态只使用 `pending`、`active`、`review`、`blocked`、`complete`、`superseded`。

## 角色和所有权

- **Owner**：负责工作单元的 Spec、Plan、接口边界、合并和最终验收。
- **Worker**：只在 Owner 指定的文件范围内实施，返回变更和证据，不改写 Owner 的 Plan 或其他 Worker 的范围。
- **Reviewer**：按 Spec 检查 diff、构建、测试、清单和风险；命令结果优先于主观判断。
- 一个可变文件同一时间只能有一个 Owner。共享接口先由 Owner 定义，其他 Agent 通过提案或独立分支修改。
- Agent 接管前先读工作包、交接包和当前源码，再执行 `git status --short --branch`；记录与源码冲突时以源码为准，并在工作包中注明。

## 工作包要求

每个跨阶段或可交接单元必须包含 `Spec`、`Plan`、`Status`、`Evidence`、`Exact next action` 和 `Deferred`。Spec 发生目标、范围、约束、权限或验收变化时先更新；只改变方法、顺序或负责人时更新 Plan。

每个子包必须声明：

1. 目标和非目标；
2. 可观察验收结果；
3. 唯一 Owner 和允许修改的文件范围；
4. 依赖的接口和上游工作包；
5. Java 17、包名、Android 5/6、设备安全边界；
6. 构建、单测、APK 元数据和真机证据。

## 接管与交接

只有所有权、会话或执行环境真实转移时才创建 handoff。交接包必须写清当前状态、完成项、未完成项、非目标、改动文件、命令及退出状态、风险/权限、`Exact next action`、完成条件和恢复路径。接收 Agent 不重复已通过且未受影响的检查。

## 必过门槛

1. `applicationId` 保持 `com.dangbei.tvlauncher`，namespace 保持 `app.orchard.tv`。
2. 构建命令显式使用 Java 17；禁止 Java 21/25。
3. 共享 Android 代码变更默认构建 `standard` 与 `guard` release，并运行受影响单测。
4. 用 `aapt2` 或等价工具核验 APK 包名、版本和清单过滤器。
5. 真机证据单列设备、APK flavor、版本、命令和结果；没有证据就写“未验证”。
6. 不得停用/卸载 `com.dangbei.leard.leradlauncher`，不得覆盖无障碍服务，不得删除户主数据或提交 `local/`。

## 分层测试策略

每次改动不要求都上当贝 F3。先按改动表面选择最低充分环境，再在发布前补齐设备验收：

- **L0：静态/单元测试**。适用于解析器、布局、配置校验、包名和清单检查；不需要设备。
- **L1：Android 手机或模拟器**。适用于 Compose 页面、手机 Web 控制台、HTTP/M3U/EPG 解析、普通 ViewModel 和非电视专属 Media3 播放。手机可验证交互和大部分逻辑，但不能替代电视结论。
- **L2：Android TV 模拟器或通用电视设备**。适用于横屏焦点、遥控器导航、Surface 播放和性能趋势。
- **L3：当贝 F3 Release 真机**。以下内容必须在目标设备验收：白名单包名和 HOME 接管、原厂桌面心跳、防黑屏、HomeGuard/待机复活、MStar 硬解码、HDMI、音量键、多任务中心和启动耗时。

工作包的 Evidence 必须写明测试级别、设备/模拟器、APK flavor、版本和结果。手机先测通过后，只能标记为“手机验证”；没有 L3 证据的当贝专属行为仍为“未验证”。

## 证据纪律

- 代码注释和标识符用英文；界面文案同步 `values`、`values-zh`、`values-ru`、`values-uk`、`values-kk` 五套资源，手机页文案同步 `app/src/main/assets/web/index.html` 的 `KEYS`/`TEXTS`。
- 只把结论和链接写入文档，不写 IP、令牌、签名密钥或户主数据。
- 不把“编译通过”写成“真机完成”，不把推测写成设备事实。
- 采用最小充分验证：已有决定性证据不重复跑；产品修改后只重跑失败项和受影响项。
- 工作包完成时更新状态和证据；真实接管时更新 handoff；完成后关闭 Plan，避免把历史继续当作当前指令。
