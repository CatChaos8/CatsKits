package github.catchaos8.catsKits.gui;

import github.catchaos8.catsKits.CatsKits;
import github.catchaos8.catsKits.kits.CustomKitItem;
import github.catchaos8.catsKits.kits.Kit;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class KitEditorGui {

    public static final String EDITOR_TITLE = "Kit Editor: ";
    public static final String SELECTOR_TITLE = "Select Item: ";
    public static final String QTY_TITLE = "Set Quantity: ";

    // Slot mappings in the editor GUI
    // Armor: slots 0-3 (helmet, chest, legs, boots)
    // Offhand: slot 8
    // Locked fillers: slots 4-7
    // Inventory: slots 9-35
    // Hotbar: slots 36-44
    // Bottom bar: 45-53 (clear=45, save=53, rest=filler)

    public static final int SLOT_HELMET     = 0;
    public static final int SLOT_CHESTPLATE = 1;
    public static final int SLOT_LEGGINGS   = 2;
    public static final int SLOT_BOOTS      = 3;
    public static final int SLOT_OFFHAND    = 8;
    public static final int SLOT_CLEAR      = 45;
    public static final int SLOT_SAVE       = 53;

    // Add this field at the top of the class
    public static List<Material> ALL_ITEMS = null;

    private List<Material> getAllowedMaterials(Player player) {
        // Build the full item list once and cache it
        if (ALL_ITEMS == null) {
            ALL_ITEMS = new ArrayList<>();
            for (Material mat : Material.values()) {
                try {
                    if (mat.isItem() && !mat.isAir() && !mat.isLegacy()) {
                        ALL_ITEMS.add(mat);
                    }
                } catch (Exception ignored) {
                    // Some materials throw on isItem() — skip them
                }
            }
        }

        // Filter by permission
        List<Material> allowed = new ArrayList<>();
        for (Material mat : ALL_ITEMS) {
            if (player.hasPermission("kits.item." + mat.name().toLowerCase())) {
                allowed.add(mat);
            }
        }
        return allowed;
    }

    // Which editor slots are locked (can't place items)
    public static final Set<Integer> LOCKED_SLOTS = Set.of(4, 5, 6, 7, 46, 47, 48, 49, 50, 51, 52);

    private final CatsKits plugin;

    public KitEditorGui(CatsKits plugin) {
        this.plugin = plugin;
    }

    public void openEditor(Player player, int kitSlot) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(EDITOR_TITLE + kitSlot));

        ItemStack[] draft = getDraft(player.getUniqueId());

        if (draft != null) {
            // Restore from draft
            for (int i = 0; i < 54; i++) {
                if (draft[i] != null) inv.setItem(i, draft[i]);
            }
        } else {
            // Load from saved kit
            Kit existing = plugin.getKitManager().getKit(player.getUniqueId(), kitSlot);
            if (existing != null) {
                ItemStack[] contents = existing.getContents();
                if (contents.length > 0 && contents[0] != null) inv.setItem(SLOT_HELMET,     contents[0]);
                if (contents.length > 1 && contents[1] != null) inv.setItem(SLOT_CHESTPLATE, contents[1]);
                if (contents.length > 2 && contents[2] != null) inv.setItem(SLOT_LEGGINGS,   contents[2]);
                if (contents.length > 3 && contents[3] != null) inv.setItem(SLOT_BOOTS,      contents[3]);
                if (contents.length > 4 && contents[4] != null) inv.setItem(SLOT_OFFHAND,    contents[4]);

                // Inventory rows: contents[5-31] -> editor slots 9-35
                for (int i = 5; i <= 31 && i < contents.length; i++) {
                    if (contents[i] != null) inv.setItem(i + 4, contents[i]);
                }

                // Hotbar: contents[32-40] -> editor slots 36-44
                for (int i = 32; i <= 40 && i < contents.length; i++) {
                    if (contents[i] != null) inv.setItem(i + 4, contents[i]);
                }
            }
        }

        // Locked filler slots
        for (int slot : LOCKED_SLOTS) {
            inv.setItem(slot, makeFiller(Material.RED_STAINED_GLASS_PANE, " "));
        }

        inv.setItem(SLOT_CLEAR, makeButton(Material.RED_WOOL, "§cClear Kit", List.of("§7Removes all items")));
        inv.setItem(SLOT_SAVE,  makeButton(Material.LIME_WOOL, "§aSave Kit", List.of("§7Saves your kit")));

        fillClickable(inv);
        player.openInventory(inv);
    }
    // Add this field to KitEditorGui
    private final Map<UUID, ItemStack[]> draftKits = new HashMap<>();

    public void saveDraft(Player player, Inventory inv) {
        ItemStack[] draft = new ItemStack[54];
        for (int i = 0; i < 54; i++) {
            draft[i] = inv.getItem(i);
        }
        draftKits.put(player.getUniqueId(), draft);
    }

    public ItemStack[] getDraft(UUID uuid) {
        return draftKits.get(uuid);
    }

    public void clearDraft(UUID uuid) {
        draftKits.remove(uuid);
    }

    // Update openItemSelector to include custom items
    public void openItemSelector(Player player, int kitSlot, int editorSlot, int page) {
        // Build combined list: custom items first, then regular materials
        List<ItemStack> selectableItems = new ArrayList<>();

        // Custom config items the player has permission for
        for (CustomKitItem custom : plugin.getCustomKitItemLoader().getAll()) {
            if (player.hasPermission(custom.getPermission())) {
                ItemStack display = custom.getItem();
                var meta = display.getItemMeta();
                if (meta != null) {
                    List<String> lore = new ArrayList<>();
                    if (meta.getLore() != null) lore.addAll(meta.getLore());
                    lore.add("§7Click to select");
                    lore.add("§8[Custom Item]");
                    meta.setLore(lore);
                    display.setItemMeta(meta);
                }
                selectableItems.add(display);
            }
        }

        // Regular materials
        if (ALL_ITEMS != null) {
            for (Material mat : ALL_ITEMS) {
                if (player.hasPermission("kits.item." + mat.name().toLowerCase())) {
                    ItemStack item = new ItemStack(mat);
                    var meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName("§f" + mat.name());
                        meta.setLore(List.of("§7Click to select"));
                        item.setItemMeta(meta);
                    }
                    selectableItems.add(item);
                }
            }
        }

        int perPage = 45;
        int start = page * perPage;
        int end = Math.min(start + perPage, selectableItems.size());

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(SELECTOR_TITLE + kitSlot + ":" + editorSlot + ":" + page));

        for (int i = start; i < end; i++) {
            inv.setItem(i - start, selectableItems.get(i));
        }

        // Navigation
        if (page > 0)
            inv.setItem(45, makeButton(Material.ARROW, "§ePrevious Page", List.of()));
        inv.setItem(49, makeButton(Material.BARRIER, "§cCancel", List.of("§7Go back to editor")));
        if (end < selectableItems.size())
            inv.setItem(53, makeButton(Material.ARROW, "§eNext Page", List.of()));

        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, makeFiller(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        player.openInventory(inv);
    }

    // Opens the quantity selector for a chosen material
    public void openQtySelector(Player player, int kitSlot, int editorSlot, Material material, int currentQty) {
        int maxQty = plugin.getGuiListener().getPermissionLimit(player, material.name().toLowerCase());
        if (maxQty == Integer.MAX_VALUE) maxQty = material.getMaxStackSize();
        maxQty = Math.min(maxQty, material.getMaxStackSize());

        Inventory inv = Bukkit.createInventory(null, 27,
                Component.text(QTY_TITLE + kitSlot + ":" + editorSlot + ":" + material.name() + ":" + maxQty));

        // Show the item in the center
        ItemStack display = new ItemStack(material, Math.max(1, currentQty));
        ItemMeta meta = display.getItemMeta();
        meta.setDisplayName("§f" + material.name());
        meta.setLore(List.of("§7Amount: §e" + Math.max(1, currentQty), "§7Max: §e" + maxQty));
        display.setItemMeta(meta);
        inv.setItem(13, display);

        // + and - buttons
        inv.setItem(12, makeButton(Material.RED_STAINED_GLASS_PANE,  "§c- 1",  List.of()));
        inv.setItem(10, makeButton(Material.RED_STAINED_GLASS_PANE,  "§c- 8",  List.of()));
        inv.setItem(14, makeButton(Material.LIME_STAINED_GLASS_PANE, "§a+ 1",  List.of()));
        inv.setItem(16, makeButton(Material.LIME_STAINED_GLASS_PANE, "§a+ 8",  List.of()));

        // Confirm/cancel
        inv.setItem(22, makeButton(Material.LIME_WOOL,    "§aConfirm", List.of("§7Add to kit")));
        inv.setItem(18, makeButton(Material.YELLOW_WOOL,  "§eBack",    List.of("§7Back to item selector")));
        inv.setItem(26, makeButton(Material.RED_WOOL,     "§cCancel",  List.of("§7Back to editor")));

        // Filler
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, makeFiller(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        player.openInventory(inv);
    }
    private void fillClickable(Inventory inv) {
        // Slots that should never get a gray pane filler
        Set<Integer> skipSlots = new HashSet<>(KitEditorGui.LOCKED_SLOTS);
        skipSlots.add(SLOT_HELMET);
        skipSlots.add(SLOT_CHESTPLATE);
        skipSlots.add(SLOT_LEGGINGS);
        skipSlots.add(SLOT_BOOTS);
        skipSlots.add(SLOT_OFFHAND);
        skipSlots.add(SLOT_SAVE);
        skipSlots.add(SLOT_CLEAR);

        for (int i = 0; i < 54; i++) {
            if (skipSlots.contains(i)) continue;
            if (inv.getItem(i) == null || inv.getItem(i).getType().isAir()) {
                inv.setItem(i, makeFiller(Material.GRAY_STAINED_GLASS_PANE, "§7Click to add item"));
            }
        }
    }

    private ItemStack makeButton(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack makeFiller(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}