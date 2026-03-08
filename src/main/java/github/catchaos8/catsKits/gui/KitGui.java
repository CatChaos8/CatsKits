package github.catchaos8.catsKits.gui;

import github.catchaos8.catsKits.CatsKits;
import github.catchaos8.catsKits.kits.DefaultKits;
import github.catchaos8.catsKits.kits.Kit;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class KitGui {
    public static final String KIT_SELECT_TITLE = "Kits: ";
    public static final String KIT_EDITOR_TITLE = "Edit Kit: ";

    private final CatsKits plugin;

    public KitGui(CatsKits plugin) {
        this.plugin = plugin;
    }

    public void openKitSelector(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(KIT_SELECT_TITLE));

        // 7 items centered in a 9-wide row = starts at slot 1
        // Layout: slot 1=basic, 2=basicplus, 3=vip, 4=custom1, 5=custom2, 6=custom3, 7=custom4
        int[] guiSlots = {10, 11, 12, 13, 14, 15, 16};

        // --- Default kits (slots 0-2 in guiSlots) ---
        List<DefaultKits> defaultKits = plugin.getDefaultKitLoader().getList();
        for (int i = 0; i < Math.min(defaultKits.size(), 3); i++) {
            DefaultKits kit = defaultKits.get(i);
            boolean hasAccess = player.hasPermission(kit.getPermission());
            Material mat = hasAccess ? kit.getGuiMaterial() : Material.BARRIER;
            List<String> lore = hasAccess
                    ? List.of("§7Left click to equip")
                    : List.of("§cNo permission");
            inv.setItem(guiSlots[i], makeGuiItem(mat, kit.getDisplayName(), lore));
        }

        // --- Custom kits (4 slots, guiSlots 3-6 = kit slots 3-6) ---
        Kit[] playerKits = plugin.getKitManager().getPlayerKits(player.getUniqueId());
        for (int i = 0; i < 4; i++) {
            int kitSlot = i + 3;
            Kit kit = playerKits[kitSlot];
            String label = kit != null ? "§a" + kit.getName() : "§7Custom Slot " + (i + 1);
            List<String> lore = kit != null
                    ? List.of("§7Left click to equip", "§7Shift click to edit")
                    : List.of("§7Click to create");
            inv.setItem(guiSlots[i + 3], makeGuiItem(Material.CHEST, label, lore));
        }

        fillEmpty(inv);
        player.openInventory(inv);
    }

    public void openEditor(Player player, int kitSlot) {
        plugin.getKitEditorGui().openEditor(player, kitSlot);
    }

    private ItemStack makeGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void fillEmpty(Inventory inv) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }
    }
}