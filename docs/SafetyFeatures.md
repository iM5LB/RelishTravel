# Safety Features

RelishTravel includes comprehensive safety checks and damage prevention.

## Damage Prevention

```yaml
elytra:
  prevent-fall-damage: true
  prevent-kinetic-damage: true
```

- **Fall Damage** - No damage when landing after RelishTravel flight
- **Kinetic Damage** - No damage from hitting walls at high speed

## Launch Safety Checks

Before allowing launch:
- Not in water or lava
- No levitation effect
- Not already flying
- Clear space above (5 blocks default)
- Chunks loaded ahead
- Valid equipment

## Obstruction Detection

```yaml
safety:
  glide-height-threshold: 5
```

Checks for clear blocks above before launch to prevent ceiling collisions.
