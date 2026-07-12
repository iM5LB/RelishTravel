<div align="center">

## Safe, controlled Elytra launch system with automatic gliding

<img width="1493" height="861" alt="RelishTravel-Banner" src="https://github.com/user-attachments/assets/1fce8291-bd0b-4137-ae8e-cba664f8dc5a" />

</div>

---

## 🌟 **Why Choose RelishTravel?**

RelishTravel transforms Elytra flight with charge-based launching, mid-air boosting, and comprehensive safety features.

⚡ **Charge-Based Launch** - Sneak + Jump, land to charge, release to launch  
🚀 **Mid-Air Boosting** - Speed boosts with permission-based limits  
🛡️ **Safety Features** - Damage prevention and obstruction detection  
🎨 **Visual Effects** - Particles, sounds, and action bar displays  
🌍 **Multi-Language** - English and Arabic support  
🔄 **Chestplate Auto-Swap** - Wear armor normally; Elytra deploys on launch and retracts on landing

---

## 📋 **Requirements**

| Component | Requirement |
|-----------|-------------|
| **Minecraft** | 1.20+ |
| **Server** | Paper, Purpur, or Paper-based forks |
| **Java** | 17+ |

---

## 🎮 **How It Works**

<div align="center">



*Sneak + Jump, land to charge, release to launch*

</div>

### Launch System
- **Sneak + Jump** then land to start charging (up to 2.5 seconds)
- Release sneak to launch with power based on charge level
- Automatically opens Elytra and starts gliding
- Virtual Elytra for players without one

<div align="center">

<video src="https://github.com/user-attachments/assets/593b7d78-3453-4b0b-b619-c18e341d1d53" controls="controls" style="max-width: 100%;">
</video>


*Sneak while gliding to boost*

</div>

### Boost System
- Sneak, **Left-click**, or **Right-click** while gliding for speed bursts (configurable via `launch.boost.trigger`)
- Permission-based limits (VIP: 5, Premium: 10, Unlimited)
- Configurable cooldown (default 5s)
- Works with normal Elytra too

---

## 🚀 **Features**

### ⚡ Launch Mechanics
- Power scaling from 60% to 140%
- Visual progress bar and particles
- Dynamic sound effects
- Configurable cooldown (default 120s)

### 🛡️ Safety
- Fall damage prevention
- Kinetic damage protection
- Obstruction detection
- Environment checks (water, lava, levitation)

### 🎨 Effects
- Electric spark particles
- Dynamic pitch sounds
- Real-time speed display
- Boost counter in action bar

<div align="center">



*Customizable particles and sounds*

</div>

---

## 📦 **Installation**

1. Download the plugin JAR file
2. Place in `plugins/` folder
3. Restart server
4. Configure in `plugins/RelishTravel/config.yml`

```bash
/rt help          # View commands
/rt reload        # Reload config
```

---

## 🎮 **Commands**

| Command | Description | Permission |
|---------|-------------|------------|
| `/rt` | Show help | `relishtravel.use` |
| `/rt toggle` | Toggle your charging on/off | `relishtravel.use` |
| `/rt toggle <player>` | Toggle another player's charging | `relishtravel.toggle.others` |
| `/rt status` | Check your charging state | `relishtravel.use` |
| `/rt status <player>` | Check another player's charging state | `relishtravel.toggle.others` |
| `/rt launch [percent]` | Launch at set power | `relishtravel.fastlaunch` |
| `/rt reload` | Reload config | `relishtravel.reload` |
| `/rt cleanup` | Force cleanup temp Elytra | `relishtravel.use` |
| `/rtl [percent]` | Quick launch shortcut | `relishtravel.fastlaunch` |

**Aliases:** `/rt`, `/rtravel`, `/relishtravel`

---

## 🔧 **Configuration**

### Basic Setup
```yaml
language: "en"  # or "ar" for Arabic

charge:
  trigger: "SNEAK_JUMP"  # SNEAK, SNEAK_JUMP, or JUMP_SNEAK

launch:
  cooldown-seconds: 120
  min-power: 0.6
  max-power: 1.4
  auto-glide: true

  boost:
    enabled: true
    trigger: "SNEAK"  # SNEAK, LEFT_CLICK, or RIGHT_CLICK
    default-limit: 3
    permission-limits:
      "relishtravel.boost.vip": 5
      "relishtravel.boost.premium": 10
      "relishtravel.boost.unlimited": -1

elytra:
  allow-virtual: true
  auto-equip-from-inventory: true
  auto-swap-chestplate: false  # true = swap chestplate ↔ Elytra on launch/land
  prevent-fall-damage: true
  prevent-kinetic-damage: true
```

---

## 🔑 **Permissions**

### Player Permissions
| Permission | Default | Description |
|------------|---------|-------------|
| `relishtravel.use` | `true` | Use RelishTravel |
| `relishtravel.fastlaunch` | `op` | Use `/rtl` command |

### Boost Permissions
| Permission | Boosts | Default |
|------------|--------|---------|
| `relishtravel.boost.vip` | 5 | `false` |
| `relishtravel.boost.premium` | 10 | `false` |
| `relishtravel.boost.unlimited` | ∞ | `op` |

### Admin Permissions
| Permission | Description | Default |
|------------|-------------|---------|
| `relishtravel.admin` | Admin features & update notifications | `op` |
| `relishtravel.reload` | Reload config | `op` |
| `relishtravel.toggle.others` | Toggle/check charging for other players | `op` |
| `relishtravel.bypass.cooldown` | Bypass launch cooldowns | `op` |
| `relishtravel.bypass.boost-cooldown` | Bypass boost cooldowns | `op` |
| `relishtravel.bypass.disabled-worlds` | Use in disabled worlds | `op` |

---

## 🌍 **Multi-Language Support**

Built-in languages:
- 🇺🇸 **English** (en)
- 🇸🇦 **Arabic** (ar)

Create custom languages in `lang/[code].yml`

---

## 📞 **Support & Links**

<div align="center">

[![Discord](https://img.shields.io/badge/Discord-Support-7289da?style=for-the-badge&logo=discord)](https://discord.gg/jDr2KZcGXk)
[![Documentation](https://img.shields.io/badge/Docs-Read-blue?style=for-the-badge&logo=gitbook)](https://im5lb.github.io/relishtravel)
[![Issues](https://img.shields.io/badge/🐛%20Issues-Report-orange?style=for-the-badge)](https://github.com/iM5LB/relishtravel/issues)
[![GitHub](https://img.shields.io/badge/GitHub-Source-black?style=for-the-badge&logo=github)](https://github.com/im5lb/RelishTravel)
[![Donate](https://img.shields.io/badge/💖%20Donate-Love-ff69b4?style=for-the-badge)](https://creators.sa/m5lb)

</div>

---

<div align="center">

**Made with ❤️ by M5LB**
</div>



