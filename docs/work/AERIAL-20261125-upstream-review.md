# AERIAL-20261125-upstream-review

Status: complete
Owner: desktop worktree
Branch: `codex/aerial/aerial-20261102-desktop-independent`

## Scope

Review the ten upstream commits from `7af23a46` through `cf97c6f3` (Aerial v2.19) without
changing the desktop/player boundary, the local liquid-glass control surfaces, or the 0.3.1/106
product baseline.

## Decisions

| Upstream commit | Decision | Local scope and boundary |
| --- | --- | --- |
| `8ba6b597` | Selected, adapted | Returning from Input settings restores focus to the Input settings row. The local root-page navigation and independent control widgets remain intact. |
| `dfeba22` | Selected, adapted | Personalization chips and the screensaver action keep left/right focus within their row at its ends; the local five-mode screensaver row is preserved. |
| `6602229f` | Selected, adapted | Detect an enabled TCL Google TV Quick Panel service and leave the remote input key to firmware; package visibility is declared without starting or changing any vendor service. |
| `2c78240c` | Selected, adapted | Guard diagnostics now record whether the Home key was left to the system or taken by the guard; key filtering and HOME ownership are unchanged. |
| `f21095c4` | Selected, adapted | PIN keypad focus is kept inside the liquid-glass panel while entering arrow PINs; no PIN storage or unlock behavior changes. |
| `c910cfee` | Deferred | App-decoded film keyframes depend on upstream film-reader APIs absent from the local wallpaper stack; importing them would cross the current single-decoder/performance boundary. |
| `435bcc30` | Deferred | Amlogic decoder detection and app-only frame extraction need a separate wallpaper performance package and Android TV evidence; not assumed applicable to the Dangbei F3. |
| `bdb254eb` | Skipped | Upstream triage handoff only. |
| `9a8871a`, `cf97c6f3` | Skipped | Upstream version/release/site metadata; local version, channel and release policy remain unchanged. |

## Verification

- Worktree was clean and owned by the desktop branch before editing; other worktrees and shared
  devices were untouched.
- `git fetch upstream --prune` succeeded after the configured proxy failed; upstream advanced to
  `cf97c6f3` and tag `v2.19`.
- Java 17 rules, affected unit tests, both Release flavors and APK contract checks are run after
  this package is complete. Device behavior remains **未验证** because no target device was
  connected by this maintenance pass.
- No default HOME, accessibility service, system app, user data, signing key or formal Release was
  changed.
