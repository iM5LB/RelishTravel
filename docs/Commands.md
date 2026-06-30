# Commands

Complete command reference.

## Player Commands

### `/relishtravel`
**Aliases:** `/rt`, `/rtravel`  
**Permission:** `relishtravel.use`

Shows the help menu.

---

### `/rt toggle`
**Permission:** `relishtravel.use`

Toggle your own charging on or off. Useful if you want to sneak without triggering the charge system.

```bash
/rt toggle        # Toggle your own charging
```

---

### `/rt status`
**Permission:** `relishtravel.use`

Check whether your charging is currently enabled or disabled.

```bash
/rt status        # Check your own charging state
```

---

### `/rtl [percent]`
**Permission:** `relishtravel.fastlaunch`

Quick launch command — bypasses charging and launches instantly.

```bash
/rtl        # Launch at 100%
/rtl 75     # Launch at 75%
/rtl 50     # Launch at 50%
```

---

## Admin Commands

### `/rt toggle <player>`
**Permission:** `relishtravel.toggle.others`

Toggle charging on or off for another player. The target player is also notified.

```bash
/rt toggle Steve     # Toggle Steve's charging
```

---

### `/rt status <player>`
**Permission:** `relishtravel.toggle.others`

Check the charging state of another player.

```bash
/rt status Steve     # Check Steve's charging state
```

---

### `/rt reload`
**Permission:** `relishtravel.reload`

Reload configuration and language files without restarting.

---

### `/rt cleanup`
**Permission:** `relishtravel.use`

Force cleanup of temporary Elytra and flight states.

---

## Usage Examples

**Basic Launch:**
1. Sneak + Jump, then land to start charging
2. Release Sneak to launch
3. Automatically start gliding

**Using Boosts:**
1. While gliding, press **Sneak** (default), **Left-click**, or **Right-click** depending on `launch.boost.trigger`
2. Get a speed boost
3. Check action bar for remaining boosts

---

## Tab Completion

All commands support tab completion:
```
/rt <tab>              → reload, launch, cleanup, toggle, status
/rtl <tab>             → 25, 50, 75, 100
/rt toggle <tab>       → [online players] (admins only)
/rt status <tab>       → [online players] (admins only)
```

---

## Cooldowns

**Launch:** 120s (default)  
**Boost:** 5s (default)

Bypass with permissions:
- `relishtravel.bypass.cooldown`
- `relishtravel.bypass.boost-cooldown`
