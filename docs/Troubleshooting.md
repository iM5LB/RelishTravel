# Troubleshooting

Common issues and solutions.

## Launch Not Working

**Check:**
- Permission: `relishtravel.use`
- Not in disabled world
- Not in water/lava
- 5 blocks clear above
- Not already flying

**Solution:**
```bash
/lp user YourName permission set relishtravel.use true
```

## Charge Cancels Immediately

**Cause:** Moving while charging

**Solution:**
```yaml
charge:
  cancel-on-move: false
```

## No Elytra

**Solution:**
```yaml
elytra:
  allow-virtual: true
  auto-equip-from-inventory: true
```

## Boosts Not Working

**Check:**
- `boost.enabled: true`
- `default-limit` > 0 or boost permission
- Not on cooldown

**Solution:**
```bash
/lp user YourName permission set relishtravel.boost.vip true
```

## No Particles/Sounds

**Solution:**
```yaml
effects:
  particles: true
  charge-sound:
    enabled: true
  launch-sound-enabled: true

launch:
  forward-boost-sound-enabled: true
  auto-glide-equip-sound-enabled: true
  boost:
    sound-enabled: true
```

Check client settings too.

## Debug Mode

Enable for detailed logging:
```yaml
debug: true
```

## Getting Help

- **Discord**: [Support server](https://discord.gg/jDr2KZcGXk)
- **GitHub**: [Open issue](https://github.com/iM5LB/relishtravel/issues)
