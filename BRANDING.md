# AetherTV Brand Foundations

> **Design language:** quiet power, orbital light, and a screen that gets out of the way.

## Core idea — The Aether Halo

The mark combines a geometric **A** with an oblique breathing orbit. The A is the access point; the halo is the atmosphere around the screen. The orbit must remain open, light, and slightly imperfect in motion rather than becoming a sealed badge.

## Palette

| Token | Hex | Usage |
|---|---|---|
| Void | `#070B16` | primary background, boot field |
| Night | `#0E1526` | panels, navigation surfaces |
| Halo Mint | `#6FE7D4` | active states, orbit, signal |
| Halo Ice | `#C9FFF8` | highlights, light edge |
| Aether Violet | `#B9A4FF` | secondary glow, depth |
| Starlight | `#F7FAFF` | primary type and icon fill |
| Quiet | `#8D9AB3` | supporting text |

Use Void/Night for 70–85% of a composition. Mint is the signal color, not a decoration. Violet should sit behind Mint as atmosphere or a restrained secondary accent.

## Typography

- **Latin display:** Avenir Next / Helvetica Neue, 700, tracking `0.08em` for the wordmark.
- **Latin UI:** Avenir Next / system sans, 500–600.
- **中文 UI:** PingFang SC / Noto Sans CJK SC, 500–600.
- **Numbers and telemetry:** SF Mono / ui-monospace, 500, tracking `0.12em`.

Keep copy short and calm. Preferred voice: “进入以太”, “正在呼吸”, “静候你的选择”. Avoid exclamation marks and promotional noise.

## Logo rules

- Use `design/brand/aethertv-logo.svg` for lockups.
- Use `assets/branding/icons/aethertv-icon-512.svg` for square app surfaces.
- Maintain clear space equal to the halo’s stroke width around the mark.
- Never flatten the orbit into a solid ring, recolor the mint to neon green, add a drop shadow, or place the logo on a busy photo without a Night plate.

## Motion principle

The halo breathes on a 6–8 second ease-in-out cycle: scale 1 → 1.035 → 1, opacity 0.42 → 0.72 → 0.42. Orbit rotation is subtle (one turn per 48–60 seconds). Motion must feel like ambient light, never a loading spinner.

## Asset map

- `design/brand/aethertv-logo.svg` — horizontal master lockup.
- `assets/branding/icons/aethertv-icon-512.svg` — 512×512 app icon source.
- `assets/branding/banners/aethertv-banner-1600x620.svg` — product/website banner.
- `assets/branding/boot/aethertv-boot-concept-1920x1080.svg` — boot visual concept.
- `design/brand/showcase.html` — browser-openable visual system and breathing halo demo.

## Export notes

SVG is the source of truth. Raster exports should be generated at 1×, 2×, and 3× from the SVG sources. Keep transparent icon exports separate from dark-field preview exports.
