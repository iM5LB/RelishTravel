# Changelog

All notable changes to RelishTravel.

## [1.0.8] - 2026-07-12

### Added
- `elytra.auto-swap-chestplate` (default: `false`) — swaps a worn chestplate with an Elytra from inventory (any slot, including offhand) on launch, then re-equips the chestplate on landing. Falls back to virtual Elytra if no real one is found.

## [1.0.7] - 2026-06-30

### Fixed
- Fixed item duplication during an active launch.

## [1.0.6] - 2026-05-20

### Added
- `launch.boost.trigger` — set boost activation to `SNEAK`, `LEFT_CLICK`, or `RIGHT_CLICK`.
- `/rt toggle [player]` and `/rt status [player]` commands.

### Fixed
- Restored broken emoji/unicode characters in `en.yml`.

## [1.0.5] - 2026-05-04

### Changed
- Added `charge.trigger` option (`SNEAK`, `SNEAK_JUMP`, `JUMP_SNEAK`).

## [1.0.4] - 2026-04-29

### Changed
- Added `ACTION_BAR`, `BOSSBAR`, and `OFF` HUD modes.
- Sounds now configured per feature.
- Charging trigger split into `charge.trigger.first` / `charge.trigger.second`.

### Fixed
- Improved config merge and migration.

## [1.0.3] - 2026-03-17

### Fixed
- Fixed dotted boost permission nodes (e.g. `relishtravel.boost.vip-plus`) not recognized from config.
- Fixed unlimited boosts exploit for normal Elytra gliders.
- Boost action bar counter now shows correctly for normal Elytra gliding.
- Boost permission nodes registered on startup and `/rt reload`.

## [1.0.2] - 2026-02-15

### Fixed
- Fixed custom achievement granted repeatedly.
- Updated achievement announcement to vanilla-like style.

## [1.0.1] - 2026-02-11

### Fixed
- Fixed Elytra advancement detection for vanilla key `end/elytra`.
- Fixed duplicate custom achievement announcements.
- Switched achievement announcement to Adventure components for hover support.

## [1.0.0] - 2026-02-09

### Initial Release
- Charge-based launch system
- Mid-air boost mechanics
- Safety features and damage prevention
- Visual effects (particles, sounds, action bar)
- Multi-language support (EN, AR)
- Permission-based boost limits
- Virtual Elytra support

---

## Support

- **Discord**: [Join server](https://discord.gg/jDr2KZcGXk)
- **GitHub**: [View source](https://github.com/iM5LB/relishtravel)
- **Issues**: [Report bugs](https://github.com/iM5LB/relishtravel/issues)
