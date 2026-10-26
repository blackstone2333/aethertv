# AetherTV Project Handoff

## Scope

AetherTV is the commercial product identity and working project derived from the migrated Aerial-tv Android TV launcher. The source repository is preserved under this directory, including its Git history, Gradle wrapper, Android module, site, docs, and existing artifacts.

## Current repository state

- Upstream project: Aerial-tv.
- Android namespace: `app.orchard.tv`.
- Application ID in the current source: `com.dangbei.tvlauncher`.
- Android support floor: API 21.
- Product flavors: `standard` and `guard`.
- Existing local changes and `artifacts/` were copied as-is; they were not discarded or rewritten.

## Directory map

```text
AetherTV/
├── app/                 Android TV launcher source
├── docs/                Device and implementation notes
├── site/                Existing project website
├── artifacts/           Existing local build/evidence outputs
├── design/brand/        AetherTV source logo and showcase page
├── assets/branding/     Icons, banners, boot visual concepts
├── BRANDING.md          Visual identity rules
├── ROADMAP.md           Functional specs, integration points and milestones
└── PROJECT.md           This handoff
```

## Recommended next passes

1. Confirm product naming and package/application ID policy before changing Android manifests.
2. Add AetherTV icon exports into Android `mipmap-*` resources after approval of the SVG mark.
3. Introduce a dedicated product flavor or resource overlay for branding, keeping upstream Aerial behavior isolated.
4. Build `standardDebug` and `guardDebug` on a machine with the Android SDK, then validate on representative TV hardware.
5. Keep signing material in `local/` and never commit it.

## Working agreements

- Treat `design/brand/aethertv-logo.svg` as the vector master.
- Do not edit generated raster exports by hand.
- Keep visual changes documented in `BRANDING.md`.
- Preserve upstream history and review changes in small commits.
