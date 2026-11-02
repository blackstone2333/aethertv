# Provider protocols and verification boundaries

## Jellyfin

`System/Info/Public` checks ProductName, then `Users/AuthenticateByName` with Username/Pw. MediaBrowser Authorization carries app/device identity and Token. Per-user Views/Items/Resume/detail use stable string IDs. PlaybackInfo POST carries conservative DeviceProfile, requested audio/subtitle and start ticks. DirectPlay uses server Videos/stream?Static=true, never a server filesystem path; remux/transcode use negotiated URLs on the configured origin. External text subtitle routes include item/source/index. Sessions/Playing, Progress and Stopped report100ns ticks with source/session IDs; stopping converted playback deletes only the current DeviceId/PlaySessionId encoding.

L0 API23 protocol fixtures cover direct/remux/transcode/error/auth/size/origin paths. Redmi K40S Release fixture covers native decoding and resume. No household Jellyfin server was configured; F3 and actual Jellyfin server versions remain unverified.

## Emby

Separate provider: `/emby` root (without duplicating an explicitly supplied suffix), X-Emby-Authorization identity and X-Emby-Token; Username/Pw plus legacy SHA1 Password field. The root/password behavior was checked against the official Java API client. Shared DTOs retain IDs as strings. Images authenticate by header; media URLs include the app-held token. Playback/session/report parsing has independent Emby fixture coverage.

A user-configured household Emby account was observed in the app; its views, posters and resume list loaded. No credentials, server URL, IDs or screenshots of that account are included here. This is browse evidence only; fixture native playback does not establish real household playback compatibility.

## MoonTV — deployed protocol, 2026-10-28

User-specified source: `https://github.com/mooncloudy/moontv` (public GitHub API and page both404 on check). Do not substitute another MoonTV fork. The running user-authorized deployment was inspected instead; server location and credentials are private local configuration.

Observed client fingerprints:

- Play client `page-2773c10621404add.js`: SHA256 `b9d4777d92e4cf23002d7f7c5b8465b74158f34ce3ed5ad3432092301decc6c6`.
- Record client `331-450a9f4a18cd8b39.js`: SHA256 `510e4e4f49ebf542878c258bf69811ad415318cc7124a6df0af2173819c7c4de`.
- Runtime storage type: Redis; username/password login is required. No immutable source commit or stable public API version was available from the supplied repository.

Independent native adapter:

- POST `/api/login` JSON `{username,password}`; only the returned Cookie session is retained, encrypted in Android Keystore storage. Cookie is never attached to CDN media/images on another origin.
- GET `/api/search?q=...` returns `{results:[...]}`. Results have source/id/title/poster/episodes/episodes_titles/source_name/class/year/desc/type_name/douban_id. UI pages48 results and480 maximum loaded; each result retains its source and opens an episode grid.
- GET `/api/detail?source=...&id=...` returns the same media shape. Episode URLs are resolved inside NAS only. HTTP/HTTPS streams use the native Media3 player; no WebView, injected scripts, remote JavaScript, DRM bypass or admin API.
- GET `/api/favorites` is the home catalog; GET `/api/playrecords` supplies bounded continue-watching entries. Keys are `source+id`; episode index is1-based, play_time/total_time use seconds, save_time uses Unix milliseconds. POST writes only the selected item's record with the observed fields. There is no clear-all or source-management call in the app.
- App IDs encode source/id/episode into opaque local identifiers; Desktop never receives CDN URLs or Cookies. Detail cache is16 entries/5minutes; image and UI budgets follow the core app. Media duration is learned from playback and included in resume reports.
- Server conversion is not claimed for MoonTV. Audio/subtitle choices depend on tracks actually present in its supplied stream. Source health and unavailable upstream streams remain visible errors; changing source uses search results.

Real deployment L0 verifies authentication, search, detail, episode resolution and resume read. Redmi K40S Release L1 also verified a selected source's actual HLS first frame,26s record write, cold-start continue card and playback from that position. Only the recorded own test item was deleted afterward; its baseline was absent. Details and source limitations are in ACCEPTANCE.md. A deployment upgrade may change this private protocol; fingerprints must be rechecked.

## fnOS Video

User screenshot: client0.9.8 / service0.8.41. User explicitly deferred this provider on2026-10-28 and will supply a service later. No fnOS account is requested/used, no MoonTV credentials are reused, and no entry is presented as a working connection.

Future work must verify that service's login/session/signature and play APIs rather than assuming Jellyfin compatibility. Public third-party code is research only; it is not evidence that0.8.41 works, and no third-party proxy is deployed or required by this app.
