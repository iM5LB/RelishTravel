# Boost System

Mid-air speed boosts while gliding with permission-based limits.

## Usage

While gliding, activate a boost using the configured trigger:

- **SNEAK** (default) — press Sneak while gliding
- **LEFT_CLICK** — left-click while gliding
- **RIGHT_CLICK** — right-click while gliding

The trigger is set in `config.yml` under `launch.boost.trigger`.

## Configuration

```yaml
launch:
  boost:
    enabled: true
    speed: 2.0
    cooldown-seconds: 5
    allow-for-normal-elytra: true  # Works for non-RelishTravel gliders too

    # Boost trigger while gliding
    # Options: SNEAK, LEFT_CLICK, RIGHT_CLICK
    trigger: "SNEAK"

    # Permission-based limits (players get the highest they qualify for)
    permission-limits:
      "relishtravel.boost.vip": 5
      "relishtravel.boost.vip-plus": 7
      "relishtravel.boost.premium": 10
      "relishtravel.boost.unlimited": -1

    # Default for players without any boost permission (0 = no boosts)
    default-limit: 3
```

## Boost Limits

- Players receive the **highest** limit from all permissions they hold
- Set to `-1` for unlimited boosts
- Set `default-limit: 0` to disable boosts for players without a permission

## Permissions

| Permission | Boosts |
|------------|--------|
| `relishtravel.boost.vip` | 5 |
| `relishtravel.boost.vip-plus` | 7 |
| `relishtravel.boost.premium` | 10 |
| `relishtravel.boost.unlimited` | ∞ |

## Cooldown

Each boost has a cooldown (default 5s). Bypass with `relishtravel.bypass.boost-cooldown`.

## Normal Elytra Support

When `allow-for-normal-elytra: true`, players gliding with a regular Elytra (not from a RelishTravel launch) can also use boosts. Their boost count resets each time they start a new glide.
