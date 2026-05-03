# Permissions

Complete permission reference.

## Player Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `relishtravel.use` | `true` | Use RelishTravel |
| `relishtravel.fastlaunch` | `op` | Use `/rtl` command |

## Boost Permissions

| Permission | Boosts | Default |
|------------|--------|---------|
| `relishtravel.boost.vip` | 5 | `false` |
| `relishtravel.boost.premium` | 10 | `false` |
| `relishtravel.boost.unlimited` | ∞ | `op` |

Players get the **highest limit** from all permissions.

## Admin Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `relishtravel.admin` | `op` | Admin features |
| `relishtravel.reload` | `op` | Reload config |
| `relishtravel.bypass.cooldown` | `op` | Bypass launch cooldown |
| `relishtravel.bypass.boost-cooldown` | `op` | Bypass boost cooldown |
| `relishtravel.bypass.disabled-worlds` | `op` | Use in disabled worlds |

## Quick Setup

**Basic players:**
```bash
/lp group default permission set relishtravel.use true
```

**VIP rank:**
```bash
/lp group vip permission set relishtravel.boost.vip true
/lp group vip permission set relishtravel.fastlaunch true
```

**Admin rank:**
```bash
/lp group admin permission set relishtravel.* true
```

## Wildcards

- `relishtravel.*` - All permissions
- `relishtravel.boost.*` - All boost permissions (unlimited)
- `relishtravel.bypass.*` - All bypass permissions
