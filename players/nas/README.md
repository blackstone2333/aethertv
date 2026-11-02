# AetherCinema

Independent Android TV media application. Test version **0.1.0 / 1**, package **app.aethertv.cinema**, namespace **app.orchard.tv**, **Android6+ / minSdk23**. Built on JDK21 under the user's explicit authorization; Desktop remains JDK17.

Based on fixed Jellyfin Android TV v0.19.10 source, with a new remote-first Compose UI, separate Jellyfin and Emby providers, and a native adapter for the user's observed MoonTV deployment. fnOS Video is deferred at the user's request.

```sh
# Supply Android SDK36 via ANDROID_HOME or ignored local.properties.
# Supply JDK21 via JAVA_HOME, or install it in an ignored .runtime/ path.
tools/build.sh
```

Output: `app/build/outputs/apk/release/app-release.apk`. The release configuration is optimized and signed with a **test key**. It is not a production release. Credentials remain encrypted in Android Keystore storage; passwords are discarded. No HOME, accessibility or system-app takeover.

Use a full HTTP/HTTPS server root in Accounts. Emby accepts a root with or without `/emby`; reverse proxy prefixes are retained. MoonTV has native search, source/episode selection, favorites, continue watching and API record reporting. Search queries the configured provider; no remote JavaScript runs in the app.

Controls use directional keys and select, with interruptible spring focus and Back to the source page. Player has pause, seek, audio, subtitles and stop; remote media play/pause works. Controls hide during idle playback and reappear on direction/select. Only foreground playback holds a decoder; background releases player and artwork memory.

- [Source and license provenance](docs/UPSTREAM.md)
- [Exact runtime dependency attribution](THIRD_PARTY_NOTICES.md)
- [Provider protocols and known limits](docs/PROVIDERS.md)
- [Desktop contract proposal](docs/DESKTOP_CONTRACT.md)
- [Verification and open acceptance](docs/ACCEPTANCE.md)
- [Isolated development and handoff](docs/DEVELOPMENT.md)

## Reusable protocol fixture

`tools/make_fixture.sh` generates synthetic AVC/AAC media (requires ffmpeg); `python3 tools/fixture_server.py` binds loopback only. For an authorized Android test device, forward only this port with `adb reverse tcp:18967 tcp:18967`, then enter `http://127.0.0.1:18967`, `fixture / fixture`, selecting Jellyfin or Emby. This is a protocol fixture, not a real Jellyfin/Emby server. Remove only this own reverse after testing.

Unit protocol tests run at API23 with Robolectric. A live MoonTV read-only test is opt-in via `AETHER_MOON_SERVER_FILE` and `AETHER_MOON_AUTH_FILE` pointing to ignored local files. It does not clear or edit user records.

Preserve GPL v2, upstream references/contributors, all corresponding source and third-party notices with distributed test APKs. Do not check in `local/`, test server details, accounts, screenshots with private content, runtime JDKs, build output or signing keys.
