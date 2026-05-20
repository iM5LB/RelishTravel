# Permissions

Complete permission reference.

## Player Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `relishtravel.use` | `true` | Use RelishTravel launch system |
| `relishtravel.fastlaunch` | `op` | Use `/rtl` quick launch command |

## Boost Permissions

Players get the **highest limit** from all permissions they have.  
Use `-1` in config for unlimited boosts.

| Permission | Boosts | Default |
|------------|--------|---------|
| `relishtravel.boost.vip` | 5 | `false` |
| `relishtravel.boost.vip-plus` | 7 | `false` |
| `relishtravel.boost.premium` | 10 | `false` |
| `relishtravel.boost.unlimited` | ∞ | `op` |

> Boost limits and permission nodes are fully configurable in `config.yml` under `launch.boost.permission-limits`.

## Admin Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `relishtravel.admin` | `op` | Admin features & update notifications |
| `relishtravel.reload` | `op` | Reload config with `/rt reload` |
| `relishtravel.toggle.others` | `op` | Toggle or check charging for other players |
| `relishtravel.bypass.cooldown` | `op` | Bypass launch cooldown |
| `relishtravel.bypass.boost-cooldown` | `op` | Bypass boost cooldown |
| `relishtravel.bypass.disabled-worlds` | `op` | Use RelishTravel in disabled worlds |

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

**Moderator rank:**
```bash
/lp group mod permission set relishtravel.toggle.others true
/lp group mod permission set relishtravel.reload true
```

**Admin rank:**
```bash
/lp group admin permission set relishtravel.* true
```

## Wildcards

- `relishtravel.*` — All permissions
- `relishtravel.boost.*` — All boost permissions (grants unlimited)
- `relishtravel.bypass.*` — All bypass permissions
