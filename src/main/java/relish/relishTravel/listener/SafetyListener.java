package relish.relishTravel.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.handler.ChargeManager;
import relish.relishTravel.model.ChargeState;

public class SafetyListener implements Listener {
    
    private final RelishTravel plugin;
    private final ChargeManager chargeManager;
    
    public SafetyListener(RelishTravel plugin, ChargeManager chargeManager) {
        this.plugin = plugin;
        this.chargeManager = chargeManager;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Player joined - maintaining state (gliding: " + player.isGliding() + ")");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (!chargeManager.isCharging(player)) {
            return;
        }
        
        if (!plugin.getConfigManager().isCancelOnMove()) {
            return;
        }
        
        ChargeState state = chargeManager.getChargeState(player);
        if (state == null) {
            return;
        }
        
        double tolerance = 0.1;
        if (new relish.relishTravel.validator.SafetyValidator(plugin.getConfigManager())
                .hasMovedBeyondTolerance(state.startLocation(), player.getLocation(), tolerance)) {
            chargeManager.cancelCharge(player);
            plugin.getMessageManager().sendMessage(player, "charge.cancelled-moved");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        if (!chargeManager.isCharging(player)) {
            return;
        }
        
        chargeManager.cancelCharge(player);
        plugin.getMessageManager().sendMessage(player, "charge.cancelled-damaged");
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        
        if (!chargeManager.isCharging(player)) {
            return;
        }
        
        chargeManager.cancelCharge(player);
        plugin.getMessageManager().sendMessage(player, "charge.cancelled-damaged");
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        event.getDrops().removeIf(item -> 
            item != null && 
            item.getType() == org.bukkit.Material.ELYTRA && 
            item.getItemMeta() != null && 
            item.getItemMeta().isUnbreakable()
        );
        
        cleanupPlayer(player);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        if (chargeManager.isCharging(player)) {
            chargeManager.cancelCharge(player);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        cleanupPlayer(player);
    }
    
    private void cleanupPlayer(Player player) {
        if (chargeManager.isCharging(player)) {
            chargeManager.cancelCharge(player);
        }
        
        if (plugin.getLaunchHandler() != null && plugin.getLaunchHandler().hasActiveLaunch(player)) {
            plugin.getLaunchHandler().handleGlideEnd(player);
        }
        
        if (plugin.getRightClickBoostHandler() != null) {
            plugin.getRightClickBoostHandler().clearCooldown(player);
        }
        
        if (plugin.getSpeedDisplayHandler() != null) {
            plugin.getSpeedDisplayHandler().stopDisplay(player);
        }
        
        if (plugin.getElytraHandler() != null) {
            plugin.getElytraHandler().restoreChestplate(player);
        }
        
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] Complete cleanup performed");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        cleanupPlayer(player);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            cleanupPlayer(player);
        }, 1L);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        if (!chargeManager.isCharging(player)) {
            return;
        }
        
        chargeManager.cancelCharge(player);
        plugin.getMessageManager().sendMessage(player, "charge.cancelled-inventory");
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        if (!chargeManager.isCharging(player)) {
            return;
        }
        
        if (event.getSlot() == 38) {
            chargeManager.cancelCharge(player);
            plugin.getMessageManager().sendMessage(player, "charge.cancelled-chest-slot");
        }
    }
}
