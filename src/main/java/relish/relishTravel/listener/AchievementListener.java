package relish.relishTravel.listener;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.config.ConfigManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AchievementListener implements Listener {

    private static final Set<String> ELYTRA_ADVANCEMENT_KEYS = Set.of(
        "elytra",        // Legacy/fallback key
        "story/elytra",  // Legacy/fallback key
        "end/elytra"     // Vanilla 1.20+ key
    );

    private static final long CUSTOM_ANNOUNCE_DEDUPE_MS = 3000L;

    private final RelishTravel plugin;
    private final ConfigManager config;
    private final NamespacedKey customAchievementAwardedKey;
    private final ConcurrentHashMap<UUID, Long> lastCustomAnnounce = new ConcurrentHashMap<>();

    public AchievementListener(RelishTravel plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.customAchievementAwardedKey = new NamespacedKey(plugin, "custom_achievement_awarded");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCriterionGrant(PlayerAdvancementCriterionGrantEvent event) {
        if (!config.isBlockVanillaAchievements()) {
            return;
        }

        Player player = event.getPlayer();
        String advancementKey = event.getAdvancement().getKey().getKey();

        if (!ELYTRA_ADVANCEMENT_KEYS.contains(advancementKey) || !isUsingVirtualElytra(player)) {
            return;
        }

        // Cancel criterion grant so vanilla advancement is not completed/announced.
        event.setCancelled(true);

        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Cancelled vanilla elytra criterion for " + player.getName() + " (using virtual elytra)");
        }

        if (config.isGrantRelishAchievement()
            && !hasReceivedCustomAchievement(player)
            && shouldAnnounceCustom(player.getUniqueId())) {
            grantCustomAchievement(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        if (!config.isBlockVanillaAchievements()) {
            return;
        }

        Player player = event.getPlayer();
        String advancementKey = event.getAdvancement().getKey().getKey();

        if (!ELYTRA_ADVANCEMENT_KEYS.contains(advancementKey) || !isUsingVirtualElytra(player)) {
            return;
        }

        // Fallback: revoke criteria if advancement done still fires on this server build.
        AdvancementProgress progress = player.getAdvancementProgress(event.getAdvancement());
        Set<String> criteria = new HashSet<>(progress.getAwardedCriteria());
        criteria.forEach(progress::revokeCriteria);

        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Revoked vanilla elytra advancement for " + player.getName() + " (fallback path)");
        }

        if (config.isGrantRelishAchievement()
            && !hasReceivedCustomAchievement(player)
            && shouldAnnounceCustom(player.getUniqueId())) {
            grantCustomAchievement(player);
        }
    }

    private boolean shouldAnnounceCustom(UUID playerId) {
        long now = System.currentTimeMillis();
        Long last = lastCustomAnnounce.get(playerId);

        if (last != null && (now - last) < CUSTOM_ANNOUNCE_DEDUPE_MS) {
            return false;
        }

        lastCustomAnnounce.put(playerId, now);
        return true;
    }

    private boolean isUsingVirtualElytra(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        return isVirtualElytra(chestplate);
    }

    private boolean isVirtualElytra(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        // Virtual elytra is marked unbreakable with plugin marker in display name.
        return meta.isUnbreakable() && meta.hasDisplayName() && meta.getDisplayName().contains("RelishTravel");
    }

    private void grantCustomAchievement(Player player) {
        String title = config.getCustomAchievementTitle();
        String description = config.getCustomAchievementDescription();

        markCustomAchievementReceived(player);

        Component hoverComponent = Component.text(title, NamedTextColor.GREEN)
            .append(Component.newline())
            .append(Component.text(description, NamedTextColor.GREEN));

        Component advancementComponent = Component.text("[" + title + "]", NamedTextColor.GREEN)
            .hoverEvent(HoverEvent.showText(hoverComponent));

        Component message = Component.text(player.getName(), NamedTextColor.WHITE)
            .append(Component.text(" has made the advancement ", NamedTextColor.WHITE))
            .append(advancementComponent);

        plugin.getServer().broadcast(message);

        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] Granted custom achievement to " + player.getName() + ": " + title);
        }
    }

    private boolean hasReceivedCustomAchievement(Player player) {
        Byte awarded = player.getPersistentDataContainer().get(customAchievementAwardedKey, PersistentDataType.BYTE);
        return awarded != null && awarded == (byte) 1;
    }

    private void markCustomAchievementReceived(Player player) {
        player.getPersistentDataContainer().set(customAchievementAwardedKey, PersistentDataType.BYTE, (byte) 1);
    }
}
