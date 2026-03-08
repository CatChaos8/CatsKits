package github.catchaos8.catsKits.kits;

import github.catchaos8.catsKits.CatsKits;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CustomKitItemLoader {

    private final CatsKits plugin;
    private final Map<String, CustomKitItem> items = new LinkedHashMap<>();

    public CustomKitItemLoader(CatsKits plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        items.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("custom-kit-items");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) continue;

            String displayName = entry.getString("display-name", id);
            String permission = entry.getString("permission", "kits.item." + id);
            String base64 = entry.getString("item");

            if (base64 == null || base64.isBlank() || base64.startsWith("<")) {
                plugin.getLogger().warning("Custom kit item '" + id + "' has no valid base64 item data, skipping.");
                continue;
            }

            try {
                byte[] data = Base64.getDecoder().decode(base64);
                ItemStack item = ItemStack.deserializeBytes(data);
//                // Override display name from config
//                var meta = item.getItemMeta();
//                if (meta != null) {
//                    meta.setDisplayName(displayName);
//                    item.setItemMeta(meta);
//                }
                items.put(id, new CustomKitItem(id, displayName, permission, item));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load custom kit item '" + id + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + items.size() + " custom kit items.");
    }

    public List<CustomKitItem> getAll() { return new ArrayList<>(items.values()); }

    public CustomKitItem get(String id) { return items.get(id); }
}