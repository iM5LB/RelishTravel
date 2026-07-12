package relish.relishTravel.handler;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import relish.relishTravel.RelishTravel;
import relish.relishTravel.config.ConfigManager;
import relish.relishTravel.message.MessageManager;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ElytraHandler {

    /**
     * PDC key stamped on the chestplate when it is displaced by a swap.
     * Value = session UUID string (unique per launch). Used at restore time
     * to find the item wherever the player moved it in their inventory.
     * The tag is removed once the chestplate is re-equipped.
     */
    private final NamespacedKey SWAP_TAG;

    /**
     * Elytra-source sentinels stored in swapElytraSlot:
     *   >= 0    : storage inventory index (0–35) the elytra came from
     *   OFFHAND : elytra came from the offhand slot
     *   VIRTUAL : no real elytra — a virtual one was created
     *   NONE    : chest slot was already empty — no chestplate displaced
     */
    private static final int OFFHAND = -2;
    private static final int VIRTUAL = -1;
    private static final int NONE    = -3;

    private final RelishTravel   plugin;
    private final ConfigManager  config;
    private final MessageManager messages;

    /**
     * Active swap sessions per player.
     * Key   = player UUID
     * Value = elytra source slot (sentinel or index) so we know where to return the elytra.
     */
    private final Map<UUID, Integer> swapElytraSlot;

    /**
     * Session tag per player — written into the chestplate's PDC and stored
     * here so we can scan for it on restore.
     */
    private final Map<UUID, String> swapSessionTag;

    public ElytraHandler(RelishTravel plugin, ConfigManager config, MessageManager messages) {
        this.plugin         = plugin;
        this.config         = config;
        this.messages       = messages;
        this.SWAP_TAG       = new NamespacedKey(plugin, "swap_session");
        this.swapElytraSlot = new ConcurrentHashMap<>();
        this.swapSessionTag = new ConcurrentHashMap<>();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    public boolean canUseElytra(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        if (chest != null && chest.getType() == Material.ELYTRA) return true;

        boolean slotFree    = chest == null || chest.getType() == Material.AIR;
        boolean swapAllowed = config.isAutoSwapChestplate();

        if (config.isAutoEquipFromInventory() && findElytraSlot(player) != NONE) {
            if (slotFree || swapAllowed) return true;
        }
        if (config.isAllowVirtual() && (slotFree || swapAllowed)) return true;
        return false;
    }

    public synchronized boolean equipElytra(Player player) {
        UUID      id    = player.getUniqueId();
        ItemStack chest = player.getInventory().getChestplate();

        if (chest != null && chest.getType() == Material.ELYTRA) {
            debug(player, "Already has Elytra equipped");
            return true;
        }

        // Clear any stale session data.
        clearSession(id);

        boolean hasChestplate = chest != null && chest.getType() != Material.AIR;

        // ── Auto-swap path ─────────────────────────────────────────────────
        if (hasChestplate && config.isAutoSwapChestplate()) {

            if (config.isAutoEquipFromInventory()) {
                int src = findElytraSlot(player);
                if (src != NONE) {
                    doRealSwap(player, id, chest, src);
                    return true;
                }
            }

            if (config.isAllowVirtual()) {
                doVirtualSwap(player, id, chest);
                return true;
            }

            debug(player, "Auto-swap: no Elytra source and virtual not allowed");
            return false;
        }
        // ──────────────────────────────────────────────────────────────────

        // ── Standard path: chest slot already free ─────────────────────────
        if (hasChestplate) return false; // swap disabled, chestplate blocking

        if (config.isAutoEquipFromInventory()) {
            int src = findElytraSlot(player);
            if (src != NONE) {
                ItemStack elytra = takeFromSlot(player, src);
                player.getInventory().setChestplate(elytra);
                swapElytraSlot.put(id, src);
                // No chestplate displaced — no tag needed.
                debug(player, "Equipped Elytra from slot " + src + " (chest was empty)");
                return true;
            }
        }

        if (config.isAllowVirtual()) {
            player.getInventory().setChestplate(createVirtualElytra());
            swapElytraSlot.put(id, VIRTUAL);
            debug(player, "Equipped virtual Elytra (chest was empty)");
            return true;
        }
        // ──────────────────────────────────────────────────────────────────

        debug(player, "Failed to equip Elytra");
        return false;
    }

    public synchronized void removeElytra(Player player, boolean wasVirtual) {
        doRestore(player, true);
    }

    /** Emergency restore: quit / death / world-change / plugin disable. */
    public void restoreChestplate(Player player) {
        doRestore(player, false);
    }

    public synchronized void cleanupFailedLaunch(Player player) {
        doRestore(player, false);
        debug(player, "Cleaned up elytra after failed launch");
    }

    public void cleanup() {
        for (UUID id : new ArrayList<>(swapElytraSlot.keySet())) {
            Player player = plugin.getServer().getPlayer(id);
            if (player != null && player.isOnline()) {
                doRestore(player, false);
            } else {
                // Player offline — clean up the tag from any cached item (best-effort).
                clearSession(id);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Swap equip helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Real-elytra swap — pure slot exchange, no inventory scanning needed at equip time:
     *   1. Take elytra from its slot (src slot → AIR).
     *   2. Take chestplate from chest slot (chest slot → AIR).
     *   3. Stamp session tag on the chestplate so restore can find it by tag
     *      even if the player moves it to a different slot during flight.
     *   4. Place chestplate into the now-empty src slot.
     *   5. Place elytra into the now-empty chest slot.
     * Net result: items are swapped, no item is ever passed through addItem.
     */
    private void doRealSwap(Player player, UUID id, ItemStack chest, int src) {
        String tag = UUID.randomUUID().toString();

        // Step 1: remove elytra from its inventory slot.
        ItemStack elytra = takeFromSlot(player, src);   // src slot is now AIR

        // Step 2: remove chestplate from chest slot directly.
        // We do NOT use setChestplate(null) because on some server implementations
        // that can trigger events or implicit handling. We grab the reference first,
        // then clear the slot, so we control exactly one copy of the item.
        ItemStack chestplate = player.getInventory().getChestplate();
        player.getInventory().setChestplate(null);       // chest slot is now AIR

        // Step 3: stamp tag on the chestplate for tag-based restore scan.
        stampTag(chestplate, tag);

        // Step 4 & 5: place items into each other's slots — pure swap.
        putAtSlot(player, src, chestplate);              // chestplate → elytra's old slot
        player.getInventory().setChestplate(elytra);     // elytra → chest slot

        swapElytraSlot.put(id, src);
        swapSessionTag.put(id, tag);

        debug(player, "Swap-equipped Elytra from slot " + src + "; chestplate tagged at slot " + src);
        messages.sendMessage(player, "elytra.elytra-equipped");
    }

    /**
     * Virtual swap — no real elytra exists in inventory:
     *   1. Find a free storage slot to park the chestplate.
     *   2. Explicitly place the chestplate there (no addItem, no implicit returns).
     *   3. Clear the chest slot.
     *   4. Place virtual elytra on chest slot.
     *   5. Store the parking slot as the "src" so restore knows exactly where it is.
     *
     * We never use addItem or setChestplate(null) to "return" the chestplate —
     * both can trigger server-side implicit inventory moves that duplicate the item.
     */
    private void doVirtualSwap(Player player, UUID id, ItemStack chest) {
        String tag = UUID.randomUUID().toString();

        // Find a free storage slot to park the chestplate.
        int parkSlot = findFreeStorageSlot(player);

        if (parkSlot >= 0) {
            // Stamp the tag so restore can verify the item.
            stampTag(chest, tag);

            // Remove chestplate from chest slot — we own the reference now.
            player.getInventory().setChestplate(null);

            // Place it at the known free slot — one explicit placement, no addItem.
            player.getInventory().setItem(parkSlot, chest);

            // Place virtual elytra on the now-empty chest slot.
            player.getInventory().setChestplate(createVirtualElytra());

            // Store parkSlot as src so restore treats this like a real swap
            // (chestplate is at a known slot index).
            swapElytraSlot.put(id, parkSlot);
            swapSessionTag.put(id, tag);

            debug(player, "Virtual-swap: chestplate tagged at free slot " + parkSlot + ", virtual Elytra equipped");
        } else {
            // Inventory is completely full — drop the chestplate and use virtual.
            stampTag(chest, tag);
            player.getInventory().setChestplate(null);
            player.getInventory().setChestplate(createVirtualElytra());
            player.getWorld().dropItemNaturally(player.getLocation(), chest);

            swapElytraSlot.put(id, VIRTUAL);
            swapSessionTag.put(id, tag);

            debug(player, "Virtual-swap: inventory full, chestplate dropped, virtual Elytra equipped");
        }

        messages.sendMessage(player, "elytra.elytra-equipped");
    }

    /** Returns the index of the first empty storage slot (0-35), or -1 if none. */
    private int findFreeStorageSlot(Player player) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int i = 0; i < storage.length; i++) {
            if (storage[i] == null || storage[i].getType() == Material.AIR) return i;
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core restore
    // ─────────────────────────────────────────────────────────────────────────

    private void doRestore(Player player, boolean sendMessage) {
        UUID    id  = player.getUniqueId();
        Integer src = swapElytraSlot.remove(id);
        String  tag = swapSessionTag.remove(id);

        if (src == null) return; // no active session

        ItemStack currentChest = player.getInventory().getChestplate();
        boolean   wearingElytra = currentChest != null && currentChest.getType() == Material.ELYTRA;

        // ── Step 1: remove whatever elytra is on the chest slot ───────────────
        ItemStack elytraToReturn = null;
        if (wearingElytra) {
            player.getInventory().setChestplate(null); // chest slot → AIR, we own the reference
            if (!isVirtualElytra(currentChest)) {
                elytraToReturn = currentChest; // real elytra — needs to go somewhere
            }
            // virtual elytra is simply discarded (null elytraToReturn = discard)
        }

        // ── Step 2: restore the chestplate ────────────────────────────────────
        boolean chestplateRestored = false;

        if (src == VIRTUAL || src == NONE) {
            // VIRTUAL: chestplate was pushed to inventory with a tag — scan for it.
            // NONE: chest was empty before, no chestplate to restore.
            if (src == VIRTUAL && tag != null) {
                ItemStack tagged = findTaggedInStorage(player, tag);
                if (tagged != null) {
                    int foundSlot = findTaggedSlot(player, tag);
                    removeTag(tagged);
                    if (foundSlot >= 0) {
                        player.getInventory().setItem(foundSlot, new ItemStack(Material.AIR));
                    } else {
                        // offhand or armour edge case — use removeFromInventory
                        removeFromInventory(player, tagged);
                    }
                    player.getInventory().setChestplate(tagged);
                    chestplateRestored = true;
                    debug(player, "Virtual-swap restore: chestplate re-equipped from slot " + foundSlot);
                } else {
                    debug(player, "Virtual-swap restore: tagged chestplate not found (dropped/lost)");
                }
            }
        } else {
            // Real swap: the chestplate is sitting in `src` right now (unless the
            // player moved it). Check by tag first for safety.
            ItemStack atSrc = getAtSlot(player, src);
            if (hasTag(atSrc, tag)) {
                // Happy path — chestplate is exactly where we put it.
                removeTag(atSrc);
                player.getInventory().setItem(src == OFFHAND ? 40 : src, new ItemStack(Material.AIR));
                // Clear the src slot properly
                if (src == OFFHAND) {
                    player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                } else {
                    player.getInventory().setItem(src, new ItemStack(Material.AIR));
                }
                player.getInventory().setChestplate(atSrc);
                chestplateRestored = true;
                debug(player, "Swap restore: chestplate taken from src slot " + src + " and re-equipped");

                // Elytra goes back to src slot (its original home).
                if (elytraToReturn != null) {
                    putAtSlot(player, src, elytraToReturn);
                    elytraToReturn = null; // handled
                    debug(player, "Elytra returned to src slot " + src);
                }
            } else if (tag != null) {
                // Player moved the chestplate — scan for the tag.
                int foundSlot = findTaggedSlot(player, tag);
                if (foundSlot != Integer.MIN_VALUE) {
                    ItemStack tagged = foundSlot == OFFHAND
                            ? player.getInventory().getItemInOffHand()
                            : player.getInventory().getItem(foundSlot);
                    removeTag(tagged);
                    if (foundSlot == OFFHAND) {
                        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                    } else {
                        player.getInventory().setItem(foundSlot, new ItemStack(Material.AIR));
                    }
                    player.getInventory().setChestplate(tagged);
                    chestplateRestored = true;
                    debug(player, "Swap restore: chestplate found at moved slot " + foundSlot + " and re-equipped");
                } else {
                    debug(player, "Swap restore: tagged chestplate not found (dropped/lost)");
                }

                // Elytra goes back to src if free, otherwise inventory.
                if (elytraToReturn != null) {
                    ItemStack nowAtSrc = getAtSlot(player, src);
                    if (nowAtSrc == null || nowAtSrc.getType() == Material.AIR) {
                        putAtSlot(player, src, elytraToReturn);
                    } else {
                        returnToInventory(player, elytraToReturn);
                    }
                    elytraToReturn = null;
                }
            }
        }

        // ── Step 3: handle any leftover elytra that wasn't placed yet ─────────
        if (elytraToReturn != null) {
            returnToInventory(player, elytraToReturn);
        }

        if (chestplateRestored && sendMessage) {
            messages.sendMessage(player, "elytra.chestplate-restored");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PDC tag helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void stampTag(ItemStack item, String tag) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(SWAP_TAG, PersistentDataType.STRING, tag);
        item.setItemMeta(meta);
    }

    private void removeTag(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().remove(SWAP_TAG);
        item.setItemMeta(meta);
    }

    private boolean hasTag(ItemStack item, String tag) {
        if (item == null || tag == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return tag.equals(pdc.get(SWAP_TAG, PersistentDataType.STRING));
    }

    /**
     * Scans storage slots (0-35) only for the tagged item.
     * Returns the live ItemStack reference (array element), or null.
     */
    private ItemStack findTaggedInStorage(Player player, String tag) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (hasTag(item, tag)) return item;
        }
        if (hasTag(player.getInventory().getItemInOffHand(), tag))
            return player.getInventory().getItemInOffHand();
        return null;
    }

    /**
     * Returns the slot index of the tagged item, OFFHAND sentinel, or Integer.MIN_VALUE if not found.
     */
    private int findTaggedSlot(Player player, String tag) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int i = 0; i < storage.length; i++) {
            if (hasTag(storage[i], tag)) return i;
        }
        if (hasTag(player.getInventory().getItemInOffHand(), tag)) return OFFHAND;
        // Also scan armour slots as a last resort
        ItemStack[] armour = player.getInventory().getArmorContents();
        for (ItemStack item : armour) {
            if (hasTag(item, tag)) return Integer.MIN_VALUE; // found but can't index easily
        }
        return Integer.MIN_VALUE; // not found
    }

    /**
     * Removes the tagged item from whichever slot it occupies.
     * Used as a fallback when the slot index isn't directly known.
     */
    private void removeFromInventory(Player player, ItemStack tagged) {
        if (tagged == null) return;
        String tag = getTag(tagged);
        PlayerInventory inv = player.getInventory();

        ItemStack[] storage = inv.getStorageContents();
        for (int i = 0; i < storage.length; i++) {
            if (storage[i] == tagged || hasTag(storage[i], tag)) {
                inv.setItem(i, new ItemStack(Material.AIR));
                return;
            }
        }
        if (inv.getItemInOffHand() == tagged || hasTag(inv.getItemInOffHand(), tag)) {
            inv.setItemInOffHand(new ItemStack(Material.AIR));
            return;
        }
        ItemStack[] armour = inv.getArmorContents();
        for (int i = 0; i < armour.length; i++) {
            if (armour[i] == tagged || hasTag(armour[i], tag)) {
                armour[i] = new ItemStack(Material.AIR);
                inv.setArmorContents(armour);
                return;
            }
        }
    }

    /** Reads the swap tag from an item's PDC, or null if absent. */
    private String getTag(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                   .get(SWAP_TAG, PersistentDataType.STRING);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Slot helpers
    // ─────────────────────────────────────────────────────────────────────────

    private int findElytraSlot(Player player) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int i = 0; i < storage.length; i++) {
            if (isUsableElytra(storage[i])) return i;
        }
        if (isUsableElytra(player.getInventory().getItemInOffHand())) return OFFHAND;
        return NONE;
    }

    private ItemStack takeFromSlot(Player player, int slot) {
        if (slot == OFFHAND) {
            ItemStack item = player.getInventory().getItemInOffHand();
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            return item;
        }
        ItemStack item = player.getInventory().getItem(slot);
        player.getInventory().setItem(slot, new ItemStack(Material.AIR));
        return item != null ? item : new ItemStack(Material.AIR);
    }

    private ItemStack getAtSlot(Player player, int slot) {
        if (slot == OFFHAND) return player.getInventory().getItemInOffHand();
        return player.getInventory().getItem(slot);
    }

    private void putAtSlot(Player player, int slot, ItemStack item) {
        if (slot == OFFHAND) {
            player.getInventory().setItemInOffHand(item);
        } else {
            player.getInventory().setItem(slot, item);
        }
    }

    private void returnToInventory(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        // Do NOT clone here — we are placing the exact item object that was just
        // removed from the chest slot. Cloning would create a second copy.
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }

    private void clearSession(UUID id) {
        swapElytraSlot.remove(id);
        swapSessionTag.remove(id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Item helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ItemStack createVirtualElytra() {
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        ItemMeta  meta   = elytra.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.setDisplayName("§bRelishTravel Elytra");
            elytra.setItemMeta(meta);
        }
        return elytra;
    }

    private boolean isVirtualElytra(ItemStack item) {
        return item != null
                && item.getType() == Material.ELYTRA
                && item.getItemMeta() != null
                && item.getItemMeta().isUnbreakable();
    }

    private boolean isUsableElytra(ItemStack item) {
        return item != null && item.getType() == Material.ELYTRA && !isDamaged(item);
    }

    private boolean isDamaged(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA) return true;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable d) {
            return d.getDamage() >= item.getType().getMaxDurability();
        }
        return false;
    }

    private void debug(Player player, String msg) {
        if (config.isDebugMode()) {
            plugin.getLogger().info("[DEBUG] [" + player.getName() + "] " + msg);
        }
    }
}
