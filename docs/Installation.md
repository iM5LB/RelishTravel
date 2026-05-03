# Installation Guide

Quick installation guide.

## Requirements

- **Minecraft**: 1.20+
- **Server**: Paper, Purpur, or Paper-based fork
- **Java**: 17+

## Installation Steps

1. Download `RelishTravel-x.x.x.jar`
2. Place in `plugins/` folder
3. Restart server
4. Configure in `plugins/RelishTravel/config.yml`

## Verify Installation

Check console for:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  RELISH TRAVEL  v1.0.0
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  ▶ Status: Enabled
  ▶ Load Time: 45ms
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Or run: `/plugins`

## Quick Setup

```bash
# Set language
language: "en"  # or "ar"

# Grant permission
/lp group default permission set relishtravel.use true

# Test
Sneak + Jump → Land → Release Sneak → Launch!
```

## Troubleshooting

**Plugin not loading?**
- Check Java version: `java -version` (must be 17+)
- Verify Paper-based server
- Check console for errors

**Launch not working?**
- Grant `relishtravel.use` permission
- Check not in disabled world
- Ensure 5 blocks clear above

## Next Steps

- [Configure the plugin](Configuration.md)
- [Set up permissions](Permissions.md)
- [Learn commands](Commands.md)
