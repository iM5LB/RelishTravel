# Configuration Guide

Complete configuration reference.

## Main Settings

```yaml
language: "en"  # en or ar
debug: false
check-for-updates: true
```

## Charge Mechanics

```yaml
charge:
  max-time: 2.5           # Seconds to full charge
  cancel-on-move: true    # Cancel if player moves
```

## Launch Settings

```yaml
launch:
  min-power: 0.6          # 60% power at 0% charge
  max-power: 1.4          # 140% power at 100% charge
  forward-momentum: 0.3
  vertical-boost: 1.5
  cooldown-seconds: 120
  auto-glide: true
  forward-boost-speed: 1.0
```

## Boost System

```yaml
launch:
  boost:
    enabled: true
    speed: 2.0
    cooldown-seconds: 5
    allow-for-normal-elytra: true
    
    permission-limits:
      "relishtravel.boost.vip": 5
      "relishtravel.boost.premium": 10
      "relishtravel.boost.unlimited": -1
    
    default-limit: 3  # For players without permissions
```

**Boost Limits:**
- Use `-1` for unlimited
- Set `default-limit: 0` to disable for non-VIPs
- Players get highest limit from all permissions

## Elytra Handling

```yaml
elytra:
  allow-virtual: true                # Virtual Elytra for players without one
  auto-equip-from-inventory: true    # Auto-equip from inventory
  prevent-fall-damage: true
  prevent-kinetic-damage: true
```

## World Restrictions

```yaml
worlds:
  disabled-worlds:
    - "world_nether"
    - "world_the_end"
```

## Visual Effects

```yaml
effects:
  speed-display: true
  boost-display: true
  action-bar-update-ticks: 4  # Lower = more frequent

  # HUD mode for charge + glide displays
  hud:
    mode: "ACTION_BAR"  # ACTION_BAR, BOSSBAR, OFF
    bossbar:
      color: "BLUE"
      overlay: "PROGRESS"

  # Charging sound (only affects charge)
  charge-sound:
    enabled: true
    type: "BLOCK_BEACON_ACTIVATE"
    volume: 0.5
    pitch-min: 0.5
    pitch-max: 2.0
  
  particles: true
  particle-type: "ELECTRIC_SPARK"
  particle-count: 5
  particle-radius-min: 0.5
  particle-radius-max: 1.0
  
  launch-sound-enabled: true
  launch-sound-volume: 1.0
  launch-sound-pitch: 1.5
```

## Sound Toggles (Per Feature)

```yaml
launch:
  forward-boost-sound-enabled: true
  auto-glide-equip-sound-enabled: true
  boost:
    sound-enabled: true
```

## Safety Settings

```yaml
velocity:
  max-horizontal: 3.0
  max-vertical: 2.0

safety:
  glide-height-threshold: 5  # Blocks of clear space needed
```

## Example Configs

**Survival (Balanced):**
```yaml
launch:
  cooldown-seconds: 120
  boost:
    default-limit: 3
elytra:
  allow-virtual: false
```

**Creative (Powerful):**
```yaml
launch:
  cooldown-seconds: 10
  min-power: 1.0
  max-power: 2.0
  boost:
    default-limit: -1
```

## Reload Config

```bash
/rt reload
```
