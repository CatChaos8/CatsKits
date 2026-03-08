package github.catchaos8.catsKits.kits;

import org.bukkit.inventory.ItemStack;

public class DefaultKits {

    private final String id;
    private final String displayName;
    private final String permission;
    private final org.bukkit.Material guiMaterial;
    private final ItemStack[] contents; // 36 slots
    private final ItemStack[] armor;    // [helmet, chestplate, leggings, boots]

    public DefaultKits(String id, String displayName, String permission,
                      org.bukkit.Material guiMaterial, ItemStack[] contents, ItemStack[] armor) {
        this.id = id;
        this.displayName = displayName;
        this.permission = permission;
        this.guiMaterial = guiMaterial;
        this.contents = contents;
        this.armor = armor;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getPermission() { return permission; }
    public org.bukkit.Material getGuiMaterial() { return guiMaterial; }
    public ItemStack[] getContents() { return contents; }
    public ItemStack[] getArmor() { return armor; }
}