package github.catchaos8.catsKits.listeners;

import github.catchaos8.catsKits.CatsKits;
import github.catchaos8.catsKits.gui.KitEditorGui;
import github.catchaos8.catsKits.gui.KitGui;
import github.catchaos8.catsKits.kits.Kit;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KitEditorListener implements Listener {

    private final CatsKits plugin;

    public KitEditorListener(CatsKits plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.startsWith(KitEditorGui.EDITOR_TITLE)) {
            handleEditor(event, player, title);
        } else if (title.startsWith(KitEditorGui.SELECTOR_TITLE)) {
            handleSelector(event, player, title);
        } else if (title.startsWith(KitEditorGui.QTY_TITLE)) {
            handleQty(event, player, title);
        }
    }

    // --- Editor handler ---

    private void handleEditor(InventoryClickEvent event, Player player, String title) {
        event.setCancelled(true);
        int kitSlot = parseFirst(title, KitEditorGui.EDITOR_TITLE);
        if (kitSlot == -1) return;

        int rawSlot = event.getRawSlot();

        // Locked slots — do nothing
        if (KitEditorGui.LOCKED_SLOTS.contains(rawSlot)) return;

        // Save
        if (rawSlot == KitEditorGui.SLOT_SAVE) {
            saveKit(player, kitSlot, event);
            return;
        }

        // Clear
        if (rawSlot == KitEditorGui.SLOT_CLEAR) {
            plugin.getKitEditorGui().clearDraft(player.getUniqueId());
            plugin.getKitEditorGui().openEditor(player, kitSlot);
            plugin.getMessages().send(player, "kit-cleared");
            return;
        }

        // Right click to clear slot
        if (event.isRightClick() && rawSlot >= 0 && rawSlot < 45 && !KitEditorGui.LOCKED_SLOTS.contains(rawSlot)) {
            if(rawSlot > 8) {
                event.getInventory().setItem(rawSlot, KitEditorGui.makeFiller(Material.GRAY_STAINED_GLASS_PANE, "§7Click to add item"));
            } else {
                event.getInventory().setItem(rawSlot, null);
            }
            // Update draft
            plugin.getKitEditorGui().saveDraft(player, event.getInventory());
            return;
        }

// Left click — open item selector
        if (event.isLeftClick() && rawSlot >= 0 && rawSlot < 54 && !KitEditorGui.LOCKED_SLOTS.contains(rawSlot)) {
            // Save current editor state as draft before leaving
            plugin.getKitEditorGui().saveDraft(player, event.getInventory());
            player.closeInventory();
            plugin.getKitEditorGui().openItemSelector(player, kitSlot, rawSlot, 0);
        }
    }

    private String getItemKey(ItemStack item) {
        // Check if it matches a custom kit item
        for (var custom : plugin.getCustomKitItemLoader().getAll()) {
            ItemStack clean = custom.getItem().clone();
            var meta = clean.getItemMeta();
            if (meta != null) { meta.setLore(null); clean.setItemMeta(meta); }

            ItemStack cleanItem = item.clone();
            var itemMeta = cleanItem.getItemMeta();
            if (itemMeta != null) {
                itemMeta.setLore(null);
                cleanItem.setItemMeta(itemMeta);
            }

            if (java.util.Arrays.equals(clean.serializeAsBytes(), cleanItem.serializeAsBytes())) {
                return custom.getId(); // returns "sharp_sword" instead of "diamond_sword"
            }
        }
        return item.getType().name().toLowerCase();
    }

    // --- Item selector handler ---

    private void handleSelector(InventoryClickEvent event, Player player, String title) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;

        // Title format: "Select Item: kitSlot:editorSlot:page"
        String[] parts = title.replace(KitEditorGui.SELECTOR_TITLE, "").split(":");
        if (parts.length < 3) return;
        int kitSlot    = parseInt(parts[0]);
        int editorSlot = parseInt(parts[1]);
        int page       = parseInt(parts[2]);

        int rawSlot = event.getRawSlot();

        // Previous page
        if (rawSlot == 45 && event.getCurrentItem().getType() == Material.ARROW) {
            player.closeInventory();
            plugin.getKitEditorGui().openItemSelector(player, kitSlot, editorSlot, page - 1);
            return;
        }

        // Cancel
        if (rawSlot == 49 && event.getCurrentItem().getType() == Material.BARRIER) {
            player.closeInventory();
            plugin.getKitEditorGui().openEditor(player, kitSlot);
            return;
        }

        // Next page
        if (rawSlot == 53 && event.getCurrentItem().getType() == Material.ARROW) {
            player.closeInventory();
            plugin.getKitEditorGui().openItemSelector(player, kitSlot, editorSlot, page + 1);
            return;
        }

        // Item selected (slots 0-44)
        if (rawSlot < 45) {
            ItemStack selected = event.getCurrentItem();
            if (selected == null || selected.getType().isAir()) return;

            boolean isCustom = selected.getItemMeta() != null
                    && selected.getItemMeta().getLore() != null
                    && selected.getItemMeta().getLore().contains("§8[Custom Item]");

            player.closeInventory();

            if (isCustom) {
                // Place directly into editor slot, no qty picker needed
                int finalKitSlot = kitSlot;
                int finalEditorSlot = editorSlot;
                ItemStack toPlace = selected.clone();
                // Remove the selector lore before placing
                var meta = toPlace.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.getLore();
                    if (lore != null) {
                        lore.remove("§7Click to select");
                        lore.remove("§8[Custom Item]");
                        meta.setLore(lore.isEmpty() ? null : lore);
                    }
                    toPlace.setItemMeta(meta);
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getKitEditorGui().openEditor(player, finalKitSlot);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.getOpenInventory() != null) {
                            player.getOpenInventory().getTopInventory().setItem(finalEditorSlot, toPlace);
                        }
                    }, 1L);
                }, 1L);
            } else {
                // Regular material — open qty selector
                plugin.getKitEditorGui().openQtySelector(player, kitSlot, editorSlot, selected.getType(), 1);
            }
        }
    }

    // --- Quantity selector handler ---

    private void handleQty(InventoryClickEvent event, Player player, String title) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;

        // Title format: "Set Quantity: kitSlot:editorSlot:MATERIAL:maxQty"
        String[] parts = title.replace(KitEditorGui.QTY_TITLE, "").split(":");
        if (parts.length < 4) return;
        int kitSlot    = parseInt(parts[0]);
        int editorSlot = parseInt(parts[1]);
        Material mat   = Material.valueOf(parts[2]);
        int maxQty     = parseInt(parts[3]);

        int rawSlot = event.getRawSlot();
        ItemStack display = event.getInventory().getItem(13);
        if (display == null) return;
        int current = display.getAmount();

        // + / - buttons
        if (rawSlot == 10) current = Math.max(1, current - 8);
        if (rawSlot == 12) current = Math.max(1, current - 1);
        if (rawSlot == 14) current = Math.min(maxQty, current + 1);
        if (rawSlot == 16) current = Math.min(maxQty, current + 8);

        // Update display after +/-
        if (rawSlot == 10 || rawSlot == 12 || rawSlot == 14 || rawSlot == 16) {
            display.setAmount(current);
            ItemMeta meta = display.getItemMeta();
            meta.setLore(java.util.List.of("§7Amount: §e" + current, "§7Max: §e" + maxQty));
            display.setItemMeta(meta);
            event.getInventory().setItem(13, display);
            return;
        }

        // Back to selector
        if (rawSlot == 18) {
            player.closeInventory();
            plugin.getKitEditorGui().openItemSelector(player, kitSlot, editorSlot, 0);
            return;
        }

        // Cancel — back to editor
        if (rawSlot == 26) {
            player.closeInventory();
            plugin.getKitEditorGui().openEditor(player, kitSlot);
            return;
        }

        // Confirm — place item in editor slot and reopen editor
        if (rawSlot == 22) {
            int finalQty = current;
            player.closeInventory();

            // Reopen editor and inject the item into the correct slot
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getKitEditorGui().openEditor(player, kitSlot);

                // After opening, inject item — needs 1 tick delay for inv to be ready
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.getOpenInventory() != null) {
                        ItemStack newItem = new ItemStack(mat, finalQty);
                        player.getOpenInventory().getTopInventory().setItem(editorSlot, newItem);
                    }
                }, 1L);
            }, 1L);
        }
    }

    // --- Save logic ---
    private void saveKit(Player player, int kitSlot, InventoryClickEvent event) {
        // Permission + limit check first
        Map<String, Integer> itemCounts = new HashMap<>();

        // Collect all items from editor slots 9-44 (inventory + hotbar)
        for (int i = 9; i <= 44; i++) {
            ItemStack item = getIfReal(event.getInventory().getItem(i));
            if (item == null) continue;
            String key = getItemKey(item);
            itemCounts.merge(key, item.getAmount(), Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            int allowed = plugin.getGuiListener().getPermissionLimit(player, entry.getKey());
            if (entry.getValue() > allowed) {
                if (allowed == 0) {
                    plugin.getMessages().send(player, "item-banned", "{item}", entry.getKey());
                } else {
                    plugin.getMessages().send(player, "item-limit", "{limit}", String.valueOf(allowed), "{item}", entry.getKey());
                }
                return;
            }
        }

        // contents layout:
        // [0-3]  = armor (helmet, chest, legs, boots)
        // [4]    = offhand
        // [5-31] = inventory rows (editor slots 9-35)
        // [32-40]= hotbar (editor slots 36-44)
        ItemStack[] contents = new ItemStack[41];

        contents[0] = getIfReal(event.getInventory().getItem(KitEditorGui.SLOT_HELMET));
        contents[1] = getIfReal(event.getInventory().getItem(KitEditorGui.SLOT_CHESTPLATE));
        contents[2] = getIfReal(event.getInventory().getItem(KitEditorGui.SLOT_LEGGINGS));
        contents[3] = getIfReal(event.getInventory().getItem(KitEditorGui.SLOT_BOOTS));
        contents[4] = getIfReal(event.getInventory().getItem(KitEditorGui.SLOT_OFFHAND));

        // Editor slots 9-35 -> contents[5-31]
        for (int i = 9; i <= 35; i++) {
            contents[i - 4] = getIfReal(event.getInventory().getItem(i));
        }

        // Editor slots 36-44 -> contents[32-40]
        for (int i = 36; i <= 44; i++) {
            contents[i - 4] = getIfReal(event.getInventory().getItem(i));
        }

        for (ItemStack item : contents) {
            if (item == null) continue;
            if (!plugin.getKitManager().playerCanUseItem(player, item)) {
                plugin.getMessages().send(player, "no-permission-item", "{item}", item.getType().name());
                return;
            }
        }

        plugin.getKitEditorGui().clearDraft(player.getUniqueId());
        Kit kit = new Kit(player.getUniqueId(), "Custom " + (kitSlot - 2), kitSlot, contents);
        plugin.getKitManager().setKit(player.getUniqueId(), kitSlot, kit);
        plugin.getMessages().send(player, "kit-saved");
        player.closeInventory();
    }

    private ItemStack getIfReal(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        // Filter out gray glass pane fillers from the editor
        if (item.getType() == Material.GRAY_STAINED_GLASS_PANE) return null;
        return item;
    }

    private int parseFirst(String title, String prefix) {
        try { return Integer.parseInt(title.replace(prefix, "").trim()); }
        catch (Exception e) { return -1; }
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return 0; }
    }
}