package github.catchaos8.catsKits.commands;

import github.catchaos8.catsKits.CatsKits;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class KitsCommand implements BasicCommand {

    private final CatsKits plugin;

    public KitsCommand(CatsKits plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage("Players only.");
            return;
        }

        if (args.length == 0) {
            plugin.getKitGui().openKitSelector(player);
            return;
        }

        if (args[0].equalsIgnoreCase("setitem")) {
            if (!player.hasPermission("kits.admin")) {
                player.sendMessage("§cNo permission.");
                return;
            }
            if (args.length < 3) {
                player.sendMessage("§cUsage: /kits setitem <kitid> <slot>");
                return;
            }
            // setitem logic here
            String kitId = args[1].toLowerCase();
            String slotArg = args[2].toLowerCase();
            var item = player.getInventory().getItemInMainHand();

            if (item.getType().isAir()) {
                player.sendMessage("§cHold an item in your main hand.");
                return;
            }

            if (!plugin.getConfig().contains("default-kits." + kitId)) {
                player.sendMessage("§cKit '§e" + kitId + "§c' not found in config.yml.");
                return;
            }

            String base64 = java.util.Base64.getEncoder().encodeToString(item.serializeAsBytes());
            plugin.getConfig().set("default-kits." + kitId + ".contents." + slotArg, base64);
            plugin.saveConfig();
            plugin.getDefaultKitLoader().load();
            player.sendMessage("§aSaved §e" + item.getType().name() + " §ato kit §e" + kitId + " §aslot §e" + slotArg + "§a!");
            return;
        }

        if (args[0].equalsIgnoreCase("setkit")) {
            if (!player.hasPermission("kits.admin")) {
                player.sendMessage("§cNo permission.");
                return;
            }
            if (args.length < 2) {
                player.sendMessage("§cUsage: /kits setkit <kitid>");
                return;
            }

            String kitId = args[1].toLowerCase();
            StringBuilder sb = new StringBuilder();
            sb.append("\n§aContents for kit '§e").append(kitId).append("§a':\n");
            sb.append("§7  contents:\n");

            ItemStack[] armor = {
                    player.getInventory().getHelmet(),
                    player.getInventory().getChestplate(),
                    player.getInventory().getLeggings(),
                    player.getInventory().getBoots()
            };
            String[] armorKeys = {"armor-helmet", "armor-chestplate", "armor-leggings", "armor-boots"};

            // Hotbar (player slots 0-8 = config slots 32-40)
            for (int i = 0; i <= 8; i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item == null || item.getType().isAir()) continue;
                int configSlot = i + 32;
                String value = getItemConfigString(item);
                if (value != null) sb.append("§7    ").append(configSlot).append(": ").append(value).append("\n");
                else sb.append("§c    ").append(configSlot).append(": §4# CUSTOM ITEM - add base64 manually\n");
            }

            // Inventory (player slots 9-35 = config slots 5-31)
            for (int i = 9; i <= 35; i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item == null || item.getType().isAir()) continue;
                int configSlot = i - 4;
                String value = getItemConfigString(item);
                if (value != null) sb.append("§7    ").append(configSlot).append(": ").append(value).append("\n");
                else sb.append("§c    ").append(configSlot).append(": §4# CUSTOM ITEM - add base64 manually\n");
            }

            // Armor
            for (int i = 0; i < 4; i++) {
                if (armor[i] == null || armor[i].getType().isAir()) continue;
                String value = getItemConfigString(armor[i]);
                if (value != null) sb.append("§7    ").append(armorKeys[i]).append(": ").append(value).append("\n");
                else sb.append("§c    ").append(armorKeys[i]).append(": §4# CUSTOM ITEM - add base64 manually\n");
            }

            // Offhand
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (!offhand.getType().isAir()) {
                String value = getItemConfigString(offhand);
                if (value != null) sb.append("§7    offhand: ").append(value).append("\n");
                else sb.append("§c    offhand: §4# CUSTOM ITEM - add base64 manually\n");
            }

            // Clean version to console for easy copying
            StringBuilder console = new StringBuilder();
            console.append("Contents for kit '").append(kitId).append("':\n  contents:\n");
            for (int i = 0; i <= 8; i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item == null || item.getType().isAir()) continue;
                String value = getItemConfigString(item);
                console.append("    ").append(i + 32).append(": ").append(value != null ? value : "# CUSTOM ITEM").append("\n");
            }
            for (int i = 9; i <= 35; i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item == null || item.getType().isAir()) continue;
                String value = getItemConfigString(item);
                console.append("    ").append(i - 4).append(": ").append(value != null ? value : "# CUSTOM ITEM").append("\n");
            }
            for (int i = 0; i < 4; i++) {
                if (armor[i] == null || armor[i].getType().isAir()) continue;
                String value = getItemConfigString(armor[i]);
                console.append("    ").append(armorKeys[i]).append(": ").append(value != null ? value : "# CUSTOM ITEM").append("\n");
            }
            if (!offhand.getType().isAir()) {
                String value = getItemConfigString(offhand);
                console.append("    offhand: ").append(value != null ? value : "# CUSTOM ITEM").append("\n");
            }

            plugin.getLogger().info(console.toString());
            player.sendMessage(sb.toString());
            player.sendMessage("§aAlso printed to console for easy copying!");
            return;
        }


        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("kits.admin")) {
                player.sendMessage("§cNo permission.");
                return;
            }
            plugin.reloadConfig();
            plugin.getDefaultKitLoader().load();
            plugin.getCustomKitItemLoader().load();
            player.sendMessage("§aKits reloaded!");
        }
    }

    private String getItemConfigString(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType().isAir()) return null;


        // Always serialize potions as base64 — potion type is stored in meta not visible fields
        if (item.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta) {
            return java.util.Base64.getEncoder().encodeToString(item.serializeAsBytes());
        }

        // Has enchantments or custom meta — use base64
        if (item.hasItemMeta() && (
                item.getItemMeta().hasEnchants() ||
                        item.getItemMeta().hasDisplayName() ||
                        item.getItemMeta().hasLore() ||
                        item.getItemMeta().hasCustomModelData())) {
            return java.util.Base64.getEncoder().encodeToString(item.serializeAsBytes());
        }

        // Plain item
        return item.getAmount() > 1
                ? item.getType().name() + ":" + item.getAmount()
                : item.getType().name();
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (args.length == 1) return List.of("setitem", "setkit", "reload");
        if (args.length == 2 && args[0].equalsIgnoreCase("setitem")) {
            return plugin.getDefaultKitLoader().getAll().keySet().stream().toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setkit")) {
            return plugin.getDefaultKitLoader().getAll().keySet().stream().toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setitem")) {
            return List.of("0", "1", "2", "3", "4", "5", "6", "7", "8",
                    "armor-helmet", "armor-chestplate", "armor-leggings", "armor-boots");
        }
        return List.of();
    }
}