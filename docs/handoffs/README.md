# 交接包

交接包只用于真实的所有权、会话或执行环境转移，不用于普通进度更新。文件名为 `docs/handoffs/<unit-id>-handoff.md`，模板见 [TEMPLATE.md](TEMPLATE.md)。

接收 Agent 的第一步：读取交接包，执行 `git status --short --branch`，检查工作包和相关 diff，再按 `Exact next action` 推进。若源码与交接记录冲突，以源码和新命令证据为准，并回写工作包状态。
