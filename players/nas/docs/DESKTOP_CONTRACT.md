# AetherCinema Desktop Contract — proposal v1

Owner: NAS application. Status: app-side implementation; Desktop integration awaits parent Owner. No shared contracts or Desktop files are changed.

## Identity and permission

- Package: `app.aethertv.cinema`, namespace `app.orchard.tv`, Android6/minSdk23.
- Signature permission: `app.aethertv.cinema.permission.READ_SNAPSHOT` (protectionLevel=`signature`). Desktop must request it and use the same signing identity. Foreign signers are denied; do not make the library public to solve a signing mismatch.
- No HOME, boot receiver, accessibility service, foreground/background playback service or Desktop dependency.

## Snapshot

`ContentResolver.query(content://app.aethertv.cinema.snapshot/v1/cards, null, null, null, null)` returns a fixed schema, at most24 rows, for the currently selected local account:

| Column | Type | Meaning |
| --- | --- | --- |
| `version` | integer | 1 |
| `account_id` | string | Opaque local UUID; required with item ID |
| `item_id` | string | Opaque backend ID, maximum4096 chars; never parse in Desktop |
| `title` | string | Display text |
| `art_uri` | nullable string | Controlled `content://app.aethertv.cinema.snapshot/art/<sha256>` URI |
| `position_ticks` | long | 100ns ticks, >=0 |
| `duration_ticks` | long | 100ns ticks; 0 means unknown |
| `updated_at` | long | Unix milliseconds |

Provider callbacks read private files only, never scan a library or call a network API. Encoded snapshot JPEGs are192×288; only the selected24 are retained. Images are read-only via `openFile("r")`; paths are opaque64-character hex keys. No file write, delete, arbitrary path or remote URL access. Snapshot changes call `notifyChange` on the cards URI. Desktop registers only while foreground and queries on foreground/resume/change with its own byte-limited cache; no polling, second decoder or NAS SDK.

Account switch/removal invalidates the prior snapshot and art; cancelled/old-account work cannot republish it. Missing/offline/stale data must leave Desktop navigable. Snapshot content never includes server addresses, passwords, tokens or raw playback URLs.

## PLAY_ITEM v1

Explicit component: `app.aethertv.cinema/app.orchard.tv.cinema.PlayItemActivity` (protected by the same signature permission).

Action: `app.aethertv.cinema.PLAY_ITEM`.

Extras: `contract_version` int=1, `account_id` string, `item_id` string, optional `position_ticks` long. NAS selects the matching saved account, resolves the item under its own credentials, negotiates playback and clamps the supplied resume position. Invalid/missing account or folder IDs do not open a URL. No URL or credential extras are accepted. A normal launcher Activity intentionally ignores this action; this prevents bypassing the protected entry.

This entry is foreground-only and holds the sole NAS decoder. Back returns to details/source; app pause releases decoder before Desktop resumes. Parent Owner owns launcher-to-player and return-focus integration and its final L1/L3 acceptance. Android5 Desktop can use the contract to detect availability, but this app itself requires Android6.
