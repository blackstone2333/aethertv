# 工作包

工作包是一个可验收的变更单元。文件名使用 `AERIAL-YYYYMMDD-<slug>.md`，状态使用 `pending`、`active`、`review`、`blocked`、`complete`、`superseded`。

开始实施前，Agent 必须读取本文件、目标工作包、相关 handoff 和当前 Git 状态。一个工作包只保留一个 Owner；需要并行时拆成不同文件范围的子包，并由集成 Owner 维护父包的 Plan。

推荐拆分：

- `phase2-brand-baseline`：品牌资源、五语言、版本/关于页、Lite 配置开关。
- `iptv-epg`：M3U/XMLTV 解析、缓存、源优先级和 Media3 播放器边界。
- `nas-media-dock`：MoonTV/Selene/OrionTV 卡片接口、历史和离线降级。
- `lite-mode`：渲染策略、缓存回收和性能证据。
- `flavor-matrix`：当贝优先的多机型 flavor、清单和 APK 元数据检查。

IPTV、NAS、Lite 和打包矩阵不得同时修改首页核心状态流；先由父包定义只读接口，再由子包接入。
