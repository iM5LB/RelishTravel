# Quick Start Guide

Get RelishTravel up and running in 5 minutes!

## Step 1: Install the Plugin

1. Download `RelishTravel-x.x.x.jar`
2. Place in `plugins/` folder
3. Start your server

## Step 2: Verify Installation

Check console for startup banner:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  RELISH TRAVEL  v1.0.6
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  ▶ Status: Enabled
  ▶ Load Time: 45ms
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Step 3: Test Basic Launch

1. Join your server
2. **Hold Sneak** (Shift)
3. **Jump** while sneaking
4. **Land** on the ground - charging starts
5. Watch action bar fill with charge
6. **Release Sneak** to launch
7. You should automatically start gliding!

## Step 4: Test Boost System

1. While gliding, **press Sneak**
2. You should get a speed boost
3. Action bar shows remaining boosts

## Step 5: Configure (Optional)

Edit `plugins/RelishTravel/config.yml`:

```yaml
# Quick tweaks
launch:
  cooldown-seconds: 60      # Reduce cooldown
  
boost:
  default-limit: 5          # More boosts for everyone
```

Reload: `/rt reload`

## Step 6: Set Up Permissions

Grant basic permission to all players:
```bash
/lp group default permission set relishtravel.use true
```

Grant VIP boosts:
```bash
/lp group vip permission set relishtravel.boost.vip true
```

## You're Done!

RelishTravel is now ready to use. Players can:
- Sneak + Jump, land to charge and launch
- Sneak while gliding to boost
- Enjoy safe, controlled Elytra flight

## Next Steps

- [Full Configuration Guide](Configuration.md)
- [All Commands](Commands.md)
- [Permission Setup](Permissions.md)
- [Troubleshooting](Troubleshooting.md)

## Common First-Time Issues

**Launch not working?**
- Check you have `relishtravel.use` permission
- Ensure you're not in water/lava
- Need 5 blocks of clear space above

**No Elytra?**
- Enable `allow-virtual: true` in config
- Or enable `auto-equip-from-inventory: true`

**Boosts not working?**
- Check `boost.enabled: true` in config
- Verify `default-limit` is greater than 0

## Getting Help

- **Discord**: [Join support server](https://discord.gg/jDr2KZcGXk)
- **Docs**: [Full documentation](/)
- **Issues**: [Report bugs](https://github.com/iM5LB/relishtravel/issues)
