package github.catchaos8.catsKits.kits;

import github.catchaos8.catsKits.CatsKits;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class DefaultKitLoader {

    private final CatsKits plugin;
    private final Map<String, DefaultKits> loaded = new LinkedHashMap<>(); // keeps order

    public DefaultKitLoader(CatsKits plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        loaded.clear();
        ConfigurationSection kitsSection = plugin.getConfig().getConfigurationSection("default-kits");
        if (kitsSection == null) return;

        for (String id : kitsSection.getKeys(false)) {
            ConfigurationSection kitSection = kitsSection.getConfigurationSection(id);
            if (kitSection == null) continue;

            String displayName = kitSection.getString("display-name", id);
            String permission = kitSection.getString("permission", "kits.preset." + id);
            Material guiMat = parseMaterial(kitSection.getString("material", "CHEST"));

            // Use same 41-slot layout as Kit.contents:
            // [0-3]=armor, [4]=offhand, [5-31]=inventory, [32-40]=hotbar
            ItemStack[] contents = new ItemStack[41];
            ItemStack[] armor = new ItemStack[4];

            ConfigurationSection contentsSection = kitSection.getConfigurationSection("contents");
            if (contentsSection != null) {
                for (String key : contentsSection.getKeys(false)) {
                    String value = contentsSection.getString(key);
                    if (value == null) continue;

                    ItemStack item = parseItem(value);
                    if (item == null) continue;

                    switch (key) {
                        case "armor-helmet"     -> { armor[0] = item; contents[0] = item; }
                        case "armor-chestplate" -> { armor[1] = item; contents[1] = item; }
                        case "armor-leggings"   -> { armor[2] = item; contents[2] = item; }
                        case "armor-boots"      -> { armor[3] = item; contents[3] = item; }
                        case "offhand"          -> contents[4] = item;
                        default -> {
                            try {
                                int slot = Integer.parseInt(key);
                                if (slot >= 0 && slot <= 40) contents[slot] = item;
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }

            loaded.put(id, new DefaultKits(id, displayName, permission, guiMat, contents, armor));
        }

        plugin.getLogger().info("Loaded " + loaded.size() + " default kits.");
    }

    private ItemStack parseItem(String value) {
        if (value == null || value.isBlank()) return null;

        // Try base64 first if it doesn't look like a simple material name
        // (base64 strings contain +, /, = and are longer than any material name)
        if (value.length() > 20 || value.contains("+") || value.contains("/") || value.endsWith("=")) {
            try {
                byte[] data = Base64.getDecoder().decode(value.trim());
                return ItemStack.deserializeBytes(data);
            } catch (Exception ignored) {}
        }

        // Try MATERIAL or MATERIAL:amount
        String[] parts = value.split(":");
        Material mat = parseMaterial(parts[0]);
        if (mat != null) {
            int amount = parts.length > 1 ? parseInt(parts[1], 1) : 1;
            return new ItemStack(mat, amount);
        }

        // Last resort base64 attempt
        try {
            byte[] data = Base64.getDecoder().decode(value.trim());
            return ItemStack.deserializeBytes(data);
        } catch (Exception ignored) {}

        return null;
    }

    public Map<String, DefaultKits> getAll() { return loaded; }

    public DefaultKits get(String id) { return loaded.get(id.toLowerCase()); }

    public List<DefaultKits> getList() { return new ArrayList<>(loaded.values()); }

    // --- Parsers ---

    // Supports "MATERIAL" or "MATERIAL:amount" or base64 serialized
    private Material parseMaterial(String name) {
        if (name == null) return null;
        try { return Material.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown material: " + name);
            return null;
        }
    }

    private boolean isBase64(String s) {
        try { Base64.getDecoder().decode(s); return true; }
        catch (Exception e) { return false; }
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}