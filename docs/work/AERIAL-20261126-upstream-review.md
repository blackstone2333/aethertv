# AERIAL-20261126-upstream-review

Status: complete
Owner: desktop worktree
Branch: `codex/aerial/aerial-20261102-desktop-independent`

## Scope

Review the seven upstream commits from `cf97c6f3` through `f30f942a` (Aerial v2.20) while keeping
the local desktop/player boundary, liquid-glass control layout, Java 17 build and 0.3.1/106
product baseline.

## Decisions

| Upstream commit | Decision | Local scope and boundary |
| --- | --- | --- |
| `7e02f602` | Selected, adapted | Restore remote focus to the Personalize control after returning from the sheet and to the Hidden apps action after returning from that sheet. The local control-card grid and multi-layer sheet remain unchanged. |
| `157ec3a6` | Selected, adapted | Read Xiaomi's `xiaomi_picture_backlight` when present and write it together with `picture_backlight`; diagnostics expose both keys. Existing capability and permission checks remain in force. |
| `59eabdee` | Deferred | Its broad per-option note callback needs a larger pass across the customized five-language settings sheet; no partial change was introduced. |
| `b101d8b3` | Skipped | XGIMI documentation and historical HOME-record guidance only. |
| `fbba6dd9` | Skipped | Triage and device documentation only. |
| `2bd6becb` | Skipped | Upstream version metadata; local remains 0.3.1/106. |
| `f30f942a` | Skipped | Release/site handoff metadata; no formal Release is published. |

## Verification

- Worktree was clean and owned by the desktop branch before editing; other worktrees and shared
  devices were untouched.
- `git fetch upstream --prune` succeeded after the configured proxy failed; upstream advanced to
  `f30f942a` and tag `v2.20`.
- Java 17 rules, both flavor unit tests, both Release builds and APK contract checks are run for
  this package. Device behavior remains **未验证** because no target device was connected.
- No default HOME, accessibility service, system app, user data, signing key or formal Release was
  changed.
