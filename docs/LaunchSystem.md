# Launch System

Detailed guide to RelishTravel's charge-based launch mechanics.

## Overview

The launch system is the core feature of RelishTravel. Players charge up by sneaking, jumping, and landing, then release sneak to launch into the air with automatic Elytra gliding.

## How It Works

### 1. Charging

**Start Charging:**
- Hold **Sneak** (Shift key)
- **Jump** while sneaking
- **Land** on the ground
- Charging begins automatically
- Action bar shows charge progress
- Particles appear and grow
- Sound pitch increases

**Charge Progress:**
```
⚡ Charging: 0%  ░░░░░░░░░░
⚡ Charging: 25% ██░░░░░░░░
⚡ Charging: 50% █████░░░░░
⚡ Charging: 75% ███████░░░
⚡ Charging: 100% ██████████
```

**Charge Duration:**
- Default: 2.5 seconds to full charge
- Configurable in `config.yml`
- Can launch at any charge level

### 2. Launching

**Trigger Launch:**
- Release **Sneak** key
- Launch power based on charge level
- Automatic Elytra equip and glide

**Power Scaling:**
- 0% charge = 60% power (min-power)
- 50% charge = 100% power
- 100% charge = 140% power (max-power)

### 3. Gliding

**Auto-Glide:**
- Elytra automatically opens
- Start gliding immediately
- No manual activation needed

**During Flight:**
- Speed display in action bar
- Sneak to use boosts
- Protected from damage

## Configuration

```yaml
charge:
  max-time: 2.5
  cancel-on-move: true

launch:
  min-power: 0.6
  max-power: 1.4
  forward-momentum: 0.3
  vertical-boost: 1.5
  cooldown-seconds: 120
  auto-glide: true
```

## Power Mechanics

### Launch Power Formula

```
power = min_power + (charge_percent * (max_power - min_power))
```

**Examples:**
- 0% charge: 0.6 + (0.0 * 0.8) = 0.6 (60% power)
- 50% charge: 0.6 + (0.5 * 0.8) = 1.0 (100% power)
- 100% charge: 0.6 + (1.0 * 0.8) = 1.4 (140% power)

### Velocity Calculation

```
horizontal = forward_momentum * power
vertical = vertical_boost * power
```

## Safety Checks

Before allowing launch, RelishTravel checks:

1. **Permission** - Has `relishtravel.use`
2. **World** - Not in disabled world
3. **Environment** - Not in water/lava
4. **Effects** - No levitation effect
5. **Flight State** - Not already flying
6. **Obstructions** - Clear space above
7. **Chunks** - Area ahead loaded
8. **Equipment** - Chest slot valid

## Charge Cancellation

Charge can be cancelled by:

**Movement** (if enabled):
```yaml
charge:
  cancel-on-move: true
```
- Walking
- Looking around (if strict)
- Jumping

**Damage:**
- Taking any damage
- From mobs, players, environment

**Inventory:**
- Opening inventory
- Opening chest
- Any GUI interaction

**Equipment:**
- Changing chest slot item
- Swapping armor

## Quick Launch Command

Bypass charging with `/rtl`:

```bash
/rtl        # Launch at 100% power
/rtl 75     # Launch at 75% power
/rtl 50     # Launch at 50% power
```

**Requirements:**
- Permission: `relishtravel.fastlaunch`
- Same safety checks apply
- Instant launch, no charging

## Cooldown System

**Default Cooldown:**
```yaml
launch:
  cooldown-seconds: 120  # 2 minutes
```

**Bypass Cooldown:**
- Permission: `relishtravel.bypass.cooldown`
- No cooldown for admins/VIPs

**Cooldown Message:**
```
⌛ Please wait 1m 30s before launching again
```

## Virtual Elytra

**What is it?**
- Temporary Elytra for players without one
- Equipped during launch
- Removed after landing

**Configuration:**
```yaml
elytra:
  allow-virtual: true
  auto-equip-from-inventory: true
```

**Benefits:**
- Everyone can use RelishTravel
- No Elytra required
- Automatic management

## Advanced Configuration

### Powerful Launches
```yaml
launch:
  min-power: 1.0
  max-power: 2.0
  forward-momentum: 0.5
  vertical-boost: 2.0
```

### Weak Launches
```yaml
launch:
  min-power: 0.4
  max-power: 1.0
  forward-momentum: 0.2
  vertical-boost: 1.0
```

### Fast Charging
```yaml
charge:
  max-time: 1.0  # 1 second to full charge
```

### Slow Charging
```yaml
charge:
  max-time: 5.0  # 5 seconds to full charge
```

## Tips & Tricks

1. **Optimal Charge** - 75-100% for best distance
2. **Quick Launch** - Use `/rtl` for instant launch
3. **Angle Matters** - Look up for height, forward for distance
4. **Combine with Boosts** - Launch then boost for maximum speed
5. **Practice** - Test different charge levels to find your preference

## Next Steps

- [Boost System](BoostSystem.md)
- [Safety Features](SafetyFeatures.md)
- [Visual Effects](VisualEffects.md)
