# Changelog

All notable changes to RelishTravel.

## [1.0.4] - 2026-04-29

### Changed

**HUD:**
- Added more HUD modes `ACTION_BAR`, `BOSSBAR`, or `OFF`.

**Sounds:**
- Sounds are now configured per feature:
  - Charging: `effects.charge-sound.enabled`
  - Launch: `effects.launch-sound-enabled`
  - Forward boost firework: `launch.forward-boost-sound-enabled`
  - Auto-glide equip sound: `launch.auto-glide-equip-sound-enabled`
  - Boost use: `launch.boost.sound-enabled`

### Fixed

**Config Updates:**
- Improved config merge/migrations for nested keys and new sections.

## [1.0.3] - 2026-03-17

### Fixed

**Boost Permission Limits:**
- Fixed custom boost permission nodes not being recognized from config when keys contain dots (example: `relishtravel.boost.vip-plus`).
- Permission limits now take precedence when the player has any matching boost permission; `default-limit` is only used when none match.

**Normal Elytra Boosting:**
- Fixed an exploit where players gliding with a normal Elytra (no RelishTravel launch) could get effectively unlimited boosts.
- Action bar now correctly shows the boost counter while gliding with a normal Elytra.

**LuckPerms / Reload:**
- Dynamic boost permission nodes from `launch.boost.permission-limits` are now registered on startup and `/rt reload` so permission suggestions can appear.

## [1.0.2] - 2026-02-15

### Fixed

**Achievement System:**
- Fixed custom RelishTravel achievement being granted repeatedly on later launches.
- Added persistent per-player tracking so each player only receives the custom achievement once.

**Custom Achievement Message:**
- Updated announcement to a more vanilla-like style with white main text.
- Kept achievement title in green and changed hover text to green.
- Formatted announcement output into 2 lines.

## [1.0.1] - 2026-02-11

### Fixed

**Achievement System:**
- Fixed Elytra advancement detection for modern vanilla key `end/elytra` (kept legacy fallbacks).
- Added criterion-level cancellation using Paper's `PlayerAdvancementCriterionGrantEvent` to better block vanilla Elytra advancement when using virtual RelishTravel Elytra.
- Kept fallback advancement criteria revocation for compatibility.
- Added duplicate-message protection so custom achievement announcements are not broadcast twice.

**Custom Achievement Message:**
- Removed forced player name color to match vanilla style.
- Replaced plain chat description line with hover text on `[Sky Traveler]`.
- Switched custom announcement to Adventure components for proper hover support.

## [1.0.0] - 2026-02-09

### Initial Release

**Features:**
- ⚡ Charge-based launch system
- 🚀 Mid-air boost mechanics
- 🛡️ Safety features and damage prevention
- 🎨 Visual effects (particles, sounds, action bar)
- 🌍 Multi-language support (EN, AR)
- 🔧 Extensive configuration options
- 🔑 Permission-based boost limits
- 🛠️ Virtual Elytra support

**Technical:**
- Minecraft 1.20+
- Paper/Purpur support
- Java 17+
- Standalone (no dependencies)

---

## Upcoming Features

### Planned for 1.1.0
- PlaceholderAPI integration
- Statistics tracking
- More languages
---

## Support

- **Discord**: [Join server](https://discord.gg/jDr2KZcGXk)
- **GitHub**: [View source](https://github.com/iM5LB/relishtravel)
- **Issues**: [Report bugs](https://github.com/iM5LB/relishtravel/issues)
