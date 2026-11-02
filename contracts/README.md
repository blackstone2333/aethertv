# AetherTV media launch contract v1

The source `MediaContract.kt` is compiled into both APKs without introducing a runtime dependency. Desktop package/namespace remain `com.dangbei.tvlauncher` / `app.orchard.tv`.

| Entry | Action | Extras |
| --- | --- | --- |
| Watch / resume | `app.aethertv.live.PLAY` | `contract_version` integer `1`; optional opaque `channel_id` string |
| Source settings | `app.aethertv.live.SETTINGS` | `contract_version` integer `1` |

Use an explicit target package `app.aethertv.live`. The player rejects unsupported versions of these actions, never interprets extras as stream URLs, and accepts channel IDs only from its own loaded catalog. Unknown channel IDs fall back to the last channel or the first station. A channel ID is 1–256 characters without control characters, e.g. `id:cctv1` or `name:news`. The regular Android MAIN launcher entry needs no version. `singleTask` PLAY requests update the running channel instead of creating a second playback activity. Settings runs in the same independent process and pauses/releases playback.

`MediaBridge.liveIntent(context, settings)` returns null for a missing companion so the desktop can show its localized install message. The desktop owns no live channel database, network refresh loop or IPTV player. A future incompatible contract must use another version and coordinate both implementations.

## Explicit legacy configuration import

Authority `com.dangbei.tvlauncher.media-config`, method `export_live_v1`, returns Bundle `{version: 1, configs: JSON string}`. The provider is read-only and requires the signature permission `com.dangbei.tvlauncher.permission.MEDIA_CONFIG`, checked by manifest and at call time. No arbitrary URI query/write/delete is supported. Only the player's user-selected Import action calls it; the export is not logged. Player import is bounded to 1 MiB and 128 sources, validates all entries before writing and inserts only absent IDs. It never deletes desktop preferences/cache or replaces an existing player entry.
