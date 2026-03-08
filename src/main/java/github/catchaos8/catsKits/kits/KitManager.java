package github.catchaos8.catsKits.kits;

import github.catchaos8.catsKits.CatsKits;
import github.catchaos8.catsKits.util.ItemSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KitManager {
    private final CatsKits plugin;
    private final Map<UUID, Kit[]> playerKits = new HashMap<>(); // 9 slots per player
    private final Map<UUID, Integer> lastUsedSlot = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public KitManager(CatsKits plugin) {
        this.plugin = plugin;
        setupFile();
        loadAll();
    }

    // --- Public API ---

    public Kit[] getPlayerKits(UUID uuid) {
        return playerKits.computeIfAbsent(uuid, k -> new Kit[9]);
    }

    public Kit getKit(UUID uuid, int slot) {
        return getPlayerKits(uuid)[slot];
    }

    public void setKit(UUID uuid, int slot, Kit kit) {
        getPlayerKits(uuid)[slot] = kit;
        savePlayer(uuid);
    }

    public int getLastUsedSlot(UUID uuid) {
        return lastUsedSlot.getOrDefault(uuid, -1);
    }

    public void setLastUsedSlot(UUID uuid, int slot) {
        lastUsedSlot.put(uuid, slot);
        dataConfig.set("lastUsed." + uuid, slot);
        save();
    }

    public boolean playerCanUseItem(Player player, ItemStack item) {
        // Check custom kit items first — match by serialized bytes
        for (CustomKitItem custom : plugin.getCustomKitItemLoader().getAll()) {
            // Strip display lore before comparing since selector adds lore tags
            ItemStack cleanCustom = custom.getItem().clone();
            var meta = cleanCustom.getItemMeta();
            if (meta != null) { meta.setLore(null); cleanCustom.setItemMeta(meta); }

            ItemStack cleanItem = item.clone();
            var itemMeta = cleanItem.getItemMeta();
            if (itemMeta != null) { itemMeta.setLore(null); cleanItem.setItemMeta(itemMeta); }

            if (java.util.Arrays.equals(cleanCustom.serializeAsBytes(), cleanItem.serializeAsBytes())) {
                return player.hasPermission(custom.getPermission());
            }
        }

        // Regular material + enchant check
        if (!player.hasPermission("kits.item." + item.getType().name().toLowerCase())) return false;
        for (Enchantment ench : item.getEnchantments().keySet()) {
            if (!player.hasPermission("kits.enchant." + ench.getKey().getKey())) return false;
        }
        return true;
    }

    // --- Storage ---

    private void savePlayer(UUID uuid) {
        Kit[] kits = getPlayerKits(uuid);
        for (int i = 0; i < 9; i++) {
            String path = "players." + uuid + ".slot" + i;
            if (kits[i] == null) {
                // Don't wipe existing data — only set null if it was previously saved
                dataConfig.set(path, null);
                continue;
            }
            dataConfig.set(path + ".name", kits[i].getName());
            List<String> serialized = new ArrayList<>();
            for (ItemStack item : kits[i].getContents()) {
                serialized.add(ItemSerializer.serialize(item));
            }
            dataConfig.set(path + ".contents", serialized);
        }
        save();
    }

    private void loadAll() {
        if (dataConfig.contains("players")) {
            for (String uuidStr : Objects.requireNonNull(dataConfig.getConfigurationSection("players")).getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Kit[] kits = new Kit[9];

                for (int i = 0; i < 9; i++) {
                    String path = "players." + uuidStr + ".slot" + i;
                    if (!dataConfig.contains(path)) continue;

                    String name = dataConfig.getString(path + ".name", "Kit " + (i + 1));
                    List<String> serialized = dataConfig.getStringList(path + ".contents");

                    // Fix: must be 41 to match saveKit's contents array size
                    ItemStack[] contents = new ItemStack[41];
                    for (int j = 0; j < Math.min(serialized.size(), 41); j++) {
                        contents[j] = ItemSerializer.deserialize(serialized.get(j));
                    }
                    kits[i] = new Kit(uuid, name, i, contents);
                }
                playerKits.put(uuid, kits);
            }
        }

        if (dataConfig.contains("lastUsed")) {
            for (String uuidStr : Objects.requireNonNull(dataConfig.getConfigurationSection("lastUsed")).getKeys(false)) {
                lastUsedSlot.put(UUID.fromString(uuidStr), dataConfig.getInt("lastUsed." + uuidStr));
            }
        }
    }

    private void save() {
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void setupFile() {
        dataFile = new File(plugin.getDataFolder(), "kits.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
}