# AetherTV

A home media launcher for Android TV 5.0+, based on [pengchujin/aerial-tv](https://github.com/pengchujin/aerial-tv), with its MIT license and copyright retained. Dangbei F3 is the primary target.

[中文](README.md) · **English**

## v0.1 development build

- Aerial home grid, folders, wallpaper, screensavers and QR-based phone management.
- AetherTV branding and five UI languages.
- Auto / Full / Lite rendering, selectable in the phone console under More, with background image cache reclamation.
- M3U and XMLTV/XMLTV.gz source configuration, channel/programme counts and offline disk-cache fallback.
- Documented work packages for human and Agent collaboration, Java 17 builds and two release flavors.

IPTV currently covers configuration, parsing and caching. Full-screen playback, live previews, NAS posters and network mounts remain future [Roadmap](ROADMAP.md) work. See the [phone acceptance record](docs/work/AERIAL-20261026-phone-acceptance.md) for evidence and limits.

## Build and install

Read the [installation guide](docs/INSTALL.md). No GitHub Release has been published yet; build from source:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleStandardRelease assembleGuardRelease
```

APKs are under `app/build/outputs/apk/<flavor>/release/`. Future in-app updates use this fork’s [Releases](https://github.com/blackstone2333/aethertv/releases). Upstream Aerial APKs are not updates for this fork.

Keep `com.dangbei.tvlauncher` as the application ID and `app.orchard.tv` as the namespace. Never disable or remove the Dangbei stock launcher `com.dangbei.leard.leradlauncher`. Phone testing does not verify F3 HOME takeover, black screens, remote input, HDMI, standby or hardware decoding; those remain unverified.

## Contributing

Start with [AGENTS.md](AGENTS.md), the [project map](docs/PROJECT.md), [workflow](docs/AGENT_WORKFLOW.md) and the relevant [work package](docs/work/) / [handoff](docs/handoffs/). Work unit IDs use `AERIAL-YYYYMMDD-<slug>`.

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./tools/check-project-rules.sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew testStandardReleaseUnitTest testGuardReleaseUnitTest
```

Keep real feed addresses, device details, tokens, credentials and signing keys local. Never commit `local/`. Historical device notes describe upstream evidence, not validation of this fork.

[Issues](https://github.com/blackstone2333/aethertv/issues) · [MIT License](LICENSE)
