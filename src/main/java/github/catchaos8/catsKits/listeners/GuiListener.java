package github.catchaos8.catsKits.listeners;

import github.catchaos8.catsKits.CatsKits;
import github.catchaos8.catsKits.gui.KitGui;
import github.catchaos8.catsKits.kits.DefaultKits;
import github.catchaos8.catsKits.kits.Kit;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiListener implements Listener {
    private final CatsKits plugin;

    public GuiListener(CatsKits plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.equals(KitGui.KIT_SELECT_TITLE)) {
            handleKitSelect(event, player);
        }
    }

    private void handleKitSelect(InventoryClickEvent e, Player player) {
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType().isAir()) return;

        int guiSlot = e.getRawSlot();

        // Default kits: GUI slots 10-12
        if (guiSlot >= 10 && guiSlot <= 12) {
            int index = guiSlot - 10;
            String[] presetPerms = {"kits.preset.basic", "kits.preset.basicplus", "kits.preset.vip"};
            String[] presetNames = {"basic", "basicplus", "vip"};

            if (!player.hasPermission(presetPerms[index])) {
                plugin.getMessages().send(player, "no-permission-kit");
                return;
            }
            equipDefaultKit(player, presetNames[index], index);
            player.closeInventory();
            return;
        }

        // Custom kits: GUI slots 13-16 = kit slots 3-6
        if (guiSlot >= 13 && guiSlot <= 16) {
            int kitSlot = (guiSlot - 13) + 3;

            if (e.isShiftClick()) {
                if (!player.hasPermission("kits.editor")) {
                    plugin.getMessages().send(player, "no-permission-editor");
                    return;
                }
                player.closeInventory();
                plugin.getKitGui().openEditor(player, kitSlot);
            } else {

                if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), "custom")) {
                    long remaining = plugin.getCooldownManager().getRemainingSeconds(player.getUniqueId(), "custom");
                    plugin.getMessages().send(player, "kit-cooldown", "{time}", plugin.getCooldownManager().formatTime(remaining));
                    return;
                }

                Kit kit = plugin.getKitManager().getKit(player.getUniqueId(), kitSlot);
                if (kit == null) {
                    if (!player.hasPermission("kits.editor")) {
                        plugin.getMessages().send(player, "no-permission-editor");
                        return;
                    }
                    plugin.getKitGui().openEditor(player, kitSlot);
                    return;
                }

                ItemStack[] contents = kit.getContents();

                // Armor
                if (contents.length > 0 && contents[0] != null && (player.getInventory().getHelmet() == null || player.getInventory().getHelmet().getType().isAir()))
                    player.getInventory().setHelmet(contents[0]);
                if (contents.length > 1 && contents[1] != null && (player.getInventory().getChestplate() == null || player.getInventory().getChestplate().getType().isAir()))
                    player.getInventory().setChestplate(contents[1]);
                if (contents.length > 2 && contents[2] != null && (player.getInventory().getLeggings() == null || player.getInventory().getLeggings().getType().isAir()))
                    player.getInventory().setLeggings(contents[2]);
                if (contents.length > 3 && contents[3] != null && (player.getInventory().getBoots() == null || player.getInventory().getBoots().getType().isAir()))
                    player.getInventory().setBoots(contents[3]);

                // Offhand
                if (contents.length > 4 && contents[4] != null && player.getInventory().getItemInOffHand().getType().isAir())
                    player.getInventory().setItemInOffHand(contents[4]);

                // Inventory rows: contents[5-31] -> player slots 9-35
                for (int i = 5; i <= 31 && i < contents.length; i++) {
                    int playerSlot = i + 4;
                    if (contents[i] != null && (player.getInventory().getItem(playerSlot) == null
                            || player.getInventory().getItem(playerSlot).getType().isAir())) {
                        player.getInventory().setItem(playerSlot, contents[i]);
                    }
                }

                // Hotbar: contents[32-40] -> player slots 0-8
                for (int i = 32; i <= 40 && i < contents.length; i++) {
                    int playerSlot = i - 32;
                    if (contents[i] != null && (player.getInventory().getItem(playerSlot) == null
                            || player.getInventory().getItem(playerSlot).getType().isAir())) {
                        player.getInventory().setItem(playerSlot, contents[i]);
                    }
                }

                plugin.getCooldownManager().setCooldown(player.getUniqueId(), "custom");
                plugin.getKitManager().setLastUsedSlot(player.getUniqueId(), kitSlot);
                plugin.getMessages().send(player, "kit-equipped", "{kit}", kit.getName());
                player.closeInventory();
            }
        }
    }

    private void equipDefaultKit(Player player, String kitName, int slot) {
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), kitName)) {
            long remaining = plugin.getCooldownManager().getRemainingSeconds(player.getUniqueId(), kitName);
            plugin.getMessages().send(player, "kit-cooldown", "{time}", plugin.getCooldownManager().formatTime(remaining));
            return;
        }

        DefaultKits defaultKit = plugin.getDefaultKitLoader().get(kitName);
        if (defaultKit == null) {
            plugin.getMessages().send(player, "kit-not-found");
            return;
        }

        ItemStack[] contents = defaultKit.getContents();

        // Offhand: contents[4]
        if (contents.length > 4 && contents[4] != null && player.getInventory().getItemInOffHand().getType().isAir())
            player.getInventory().setItemInOffHand(contents[4]);

        // Inventory rows: contents[5-31] -> player slots 9-35
        for (int i = 5; i <= 31 && i < contents.length; i++) {
            int playerSlot = i + 4;
            if (contents[i] != null && (player.getInventory().getItem(playerSlot) == null
                    || player.getInventory().getItem(playerSlot).getType().isAir())) {
                player.getInventory().setItem(playerSlot, contents[i]);
            }
        }

        // Hotbar: contents[32-40] -> player slots 0-8
        for (int i = 32; i <= 40 && i < contents.length; i++) {
            int playerSlot = i - 32;
            if (contents[i] != null && (player.getInventory().getItem(playerSlot) == null
                    || player.getInventory().getItem(playerSlot).getType().isAir())) {
                player.getInventory().setItem(playerSlot, contents[i]);
            }
        }

        // Armor
        ItemStack[] armor = defaultKit.getArmor();
        if (armor != null) {
            if (armor[0] != null && (player.getInventory().getHelmet() == null || player.getInventory().getHelmet().getType().isAir()))
                player.getInventory().setHelmet(armor[0]);
            if (armor[1] != null && (player.getInventory().getChestplate() == null || player.getInventory().getChestplate().getType().isAir()))
                player.getInventory().setChestplate(armor[1]);
            if (armor[2] != null && (player.getInventory().getLeggings() == null || player.getInventory().getLeggings().getType().isAir()))
                player.getInventory().setLeggings(armor[2]);
            if (armor[3] != null && (player.getInventory().getBoots() == null || player.getInventory().getBoots().getType().isAir()))
                player.getInventory().setBoots(armor[3]);
        }

        plugin.getCooldownManager().setCooldown(player.getUniqueId(), kitName);
        plugin.getKitManager().setLastUsedSlot(player.getUniqueId(), slot);
        plugin.getMessages().send(player, "kit-equipped", "{kit}", defaultKit.getDisplayName());
    }


    public int getPermissionLimit(Player player, String itemName) {
        if (!plugin.getConfig().contains("kit-item-limits." + itemName)) {
            return Integer.MAX_VALUE; // no limit defined = unlimited
        }

        int defaultLimit = plugin.getConfig().getInt("kit-item-limits." + itemName + ".default", 0);
        List<?> values = plugin.getConfig().getList("kit-item-limits." + itemName + ".values");

        if (values == null || values.isEmpty()) return defaultLimit;

        // Find the highest limit the player has permission for
        int highest = defaultLimit;
        for (Object val : values) {
            int amount = (int) val;
            if (player.hasPermission("kits.limit." + itemName + "." + amount)) {
                highest = Math.max(highest, amount);
            }
        }

        return highest;
    }
}