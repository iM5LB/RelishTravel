# Visual Effects

Customizable particles, sounds, and displays.

## Particles

```yaml
effects:
  particles: true
  particle-type: "ELECTRIC_SPARK"
  particle-count: 5
  particle-radius-min: 0.5
  particle-radius-max: 1.0
```

Electric sparks that grow as you charge.

## Sounds

```yaml
effects:
  # Charging sound only
  charge-sound:
    enabled: true
    type: "BLOCK_BEACON_ACTIVATE"
    volume: 0.5
    pitch-min: 0.5
    pitch-max: 2.0

  # Launch sound (RelishTravel launch trigger)
  launch-sound-enabled: true
```

Dynamic pitch that increases with charge level.

Additional sound toggles live under `launch.*`:

```yaml
launch:
  forward-boost-sound-enabled: true
  auto-glide-equip-sound-enabled: true
  boost:
    sound-enabled: true
```

## Action Bar Displays

```yaml
effects:
  speed-display: true
  boost-display: true
  action-bar-update-ticks: 4
```

- **Charge Progress** - Shows percentage and bar
- **Speed Display** - Real-time speed during flight
- **Boost Counter** - Remaining boosts shown
