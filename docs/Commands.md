# Commands

Complete command reference.

## Player Commands

### `/relishtravel`
**Aliases:** `/rt`, `/rtravel`  
**Permission:** `relishtravel.use`

Shows help menu.

### `/rtl [percent]`
**Permission:** `relishtravel.fastlaunch`

Quick launch command.

```bash
/rtl        # Launch at 100%
/rtl 75     # Launch at 75%
/rtl 50     # Launch at 50%
```

## Admin Commands

### `/rt reload`
**Permission:** `relishtravel.reload`

Reload configuration and language files.

### `/rt cleanup`
**Permission:** `relishtravel.admin`

Force cleanup of temporary Elytra and states.

## Normal Usage

**Basic Launch:**
1. Sneak + Jump, then land to start charging
2. Release Sneak to launch
3. Automatically start gliding

**Using Boosts:**
1. While gliding, press Sneak
2. Get speed boost
3. Check action bar for remaining boosts

## Tab Completion

All commands support tab completion:
```
/rt <tab>       → reload, launch, cleanup
/rtl <tab>      → 25, 50, 75, 100
```

## Cooldowns

**Launch:** 120s (default)  
**Boost:** 5s (default)

Bypass with permissions:
- `relishtravel.bypass.cooldown`
- `relishtravel.bypass.boost-cooldown`
