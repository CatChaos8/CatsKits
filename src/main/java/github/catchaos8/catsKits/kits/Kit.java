package github.catchaos8.catsKits.kits;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Kit {
    private final UUID ownerUUID;
    private String name;
    private final int slot;
    private ItemStack[] contents;

    public Kit(UUID ownerUUID, String name, int slot, ItemStack[] contents) {
        this.ownerUUID = ownerUUID;
        this.name = name;
        this.slot = slot;
        this.contents = contents;
    }

    public UUID getOwnerUUID() { return ownerUUID; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getSlot() { return slot; }
    public ItemStack[] getContents() { return contents; }
    public void setContents(ItemStack[] contents) { this.contents = contents; }
}