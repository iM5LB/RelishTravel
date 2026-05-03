# API Documentation

Developer API for integrating with RelishTravel.

## Getting Started

Add RelishTravel as a dependency in your `plugin.yml`:

```yaml
depend: [RelishTravel]
```

## Accessing the API

```java
RelishTravel plugin = (RelishTravel) Bukkit.getPluginManager().getPlugin("RelishTravel");
```

## Managers

### LaunchHandler
```java
LaunchHandler launchHandler = plugin.getLaunchHandler();
boolean isGliding = launchHandler.isGliding(player);
```

### ChargeManager
```java
ChargeManager chargeManager = plugin.getChargeManager();
boolean isCharging = chargeManager.isCharging(player);
```

### BoostHandler
```java
BoostHandler boostHandler = plugin.getRightClickBoostHandler();
int remaining = boostHandler.getRemainingBoosts(player);
```

## Events

RelishTravel uses standard Bukkit events. Listen for:
- `PlayerToggleSneakEvent` - Charge start/stop
- `EntityToggleGlideEvent` - Glide start/stop
- `PlayerMoveEvent` - Charge cancellation

## More Information

For advanced API usage, check the source code on [GitHub](https://github.com/iM5LB/relishtravel).
