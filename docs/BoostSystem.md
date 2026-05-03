# Boost System

Mid-air speed boosts while gliding with permission-based limits.

## Usage

While gliding, press **Sneak** to activate a speed boost.

## Configuration

```yaml
boost:
  enabled: true
  speed: 2.0
  cooldown-seconds: 5
  default-limit: 3
  permission-limits:
    "relishtravel.boost.vip": 5
    "relishtravel.boost.premium": 10
    "relishtravel.boost.unlimited": -1
```

## Permissions

- `relishtravel.boost.vip` - 5 boosts per glide
- `relishtravel.boost.premium` - 10 boosts per glide  
- `relishtravel.boost.unlimited` - Unlimited boosts

Players get the highest limit from all permissions they have.
