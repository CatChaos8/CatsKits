package github.catchaos8.catsKits.kits;

import org.bukkit.inventory.ItemStack;

public class CustomKitItem {
    private final String id;
    private final String displayName;
    private final String permission;
    private final ItemStack item;        // original — used for comparisons

    public CustomKitItem(String id, String displayName, String permission, ItemStack item) {
        this.id = id;
        this.displayName = displayName;
        this.permission = permission;
        this.item = item.clone();
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getPermission() { return permission; }
    public ItemStack getItem() { return item.clone(); }           // for comparisons
}