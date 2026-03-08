package github.catchaos8.catsKits.util;

import org.bukkit.inventory.ItemStack;

import java.util.Base64;

public class ItemSerializer {

    public static String serialize(ItemStack item) {
        if(item == null) return "null";
        byte[] data = item.serializeAsBytes();
        return Base64.getEncoder().encodeToString(data);
    }

    public static ItemStack deserialize(String base64) {
        if(base64 == null || base64.equals("null")) return null;
        byte[] data = Base64.getDecoder().decode(base64);
        return ItemStack.deserializeBytes(data);
    }
}
