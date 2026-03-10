package github.catchaos8.catsKits.listeners;

import github.catchaos8.catsKits.CatsKits;
import github.catchaos8.catsKits.kits.DefaultKits;
import github.catchaos8.catsKits.kits.Kit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class KitListener implements Listener {
    public final CatsKits plugin;

    public KitListener(CatsKits plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        int lastSlot = plugin.getKitManager().getLastUsedSlot(player.getUniqueId());
        if (lastSlot == -1) return; // never used a kit

        // Check if it was a default kit (slots 0-2) or custom kit (slots 3-8)
        final ItemStack[] contents;
        final ItemStack[] armor;

        if (lastSlot <= 2) {
            // Default kit — get from DefaultKitLoader by index
            List<DefaultKits> defaults = plugin.getDefaultKitLoader().getList();
            if (lastSlot >= defaults.size()) return;
            DefaultKits defaultKit = defaults.get(lastSlot);
            contents = defaultKit.getContents();
            armor = defaultKit.getArmor();
        } else {
            Kit kit = plugin.getKitManager().getKit(player.getUniqueId(), lastSlot);
            if (kit == null) return;
            contents = kit.getContents();
            armor = new ItemStack[]{contents.length > 0 ? contents[0] : null,
                    contents.length > 1 ? contents[1] : null,
                    contents.length > 2 ? contents[2] : null,
                    contents.length > 3 ? contents[3] : null};
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Armor
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

            // Offhand
            if (contents.length > 4 && contents[4] != null)
                player.getInventory().setItemInOffHand(contents[4]);

            // Inventory rows (contents[5-31] -> player slots 9-35)
            for (int i = 5; i <= 31 && i < contents.length; i++) {
                int playerSlot = i + 4;
                if (contents[i] != null && (player.getInventory().getItem(playerSlot) == null
                        || player.getInventory().getItem(playerSlot).getType().isAir())) {
                    player.getInventory().setItem(playerSlot, contents[i]);
                }
            }

            // Hotbar (contents[32-40] -> player slots 0-8)
            for (int i = 32; i <= 40 && i < contents.length; i++) {
                int playerSlot = i - 32;
                if (contents[i] != null && (player.getInventory().getItem(playerSlot) == null
                        || player.getInventory().getItem(playerSlot).getType().isAir())) {
                    player.getInventory().setItem(playerSlot, contents[i]);
                }
            }
        }, 1L);

    }

//
//    @EventHandler
//    public void onDeath(PlayerDeathEvent e) {
//        Player player = e.getEntity();
//    }

}
