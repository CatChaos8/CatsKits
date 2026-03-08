package github.catchaos8.catsKits.kits;

import github.catchaos8.catsKits.CatsKits;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KitCooldownManager {

    private final CatsKits plugin;
    // Map of UUID -> Map of kit identifier -> last used timestamp (ms)
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public KitCooldownManager(CatsKits plugin) {
        this.plugin = plugin;
    }

    // kitKey = "basic", "basicplus", "vip", or "custom"
    public boolean isOnCooldown(UUID uuid, String kitKey) {
        long cooldownSeconds = getCooldownSeconds(uuid, kitKey);
        if (cooldownSeconds <= 0) return false;

        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return false;

        Long lastUsed = playerCooldowns.get(kitKey);
        if (lastUsed == null) return false;

        return System.currentTimeMillis() - lastUsed < cooldownSeconds * 1000;
    }

    public long getRemainingSeconds(UUID uuid, String kitKey) {
        long cooldownSeconds = getCooldownSeconds(uuid, kitKey);
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return 0;
        Long lastUsed = playerCooldowns.get(kitKey);
        if (lastUsed == null) return 0;
        long elapsed = (System.currentTimeMillis() - lastUsed) / 1000;
        return Math.max(0, cooldownSeconds - elapsed);
    }

    public void setCooldown(UUID uuid, String kitKey) {
        cooldowns.computeIfAbsent(uuid, k -> new HashMap<>())
                .put(kitKey, System.currentTimeMillis());
    }

    // Check if player has a bypass permission for this kit's cooldown
    private long getCooldownSeconds(UUID uuid, String kitKey) {
        // Check for bypass permission
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
        if (player != null && player.hasPermission("kits.cooldown.bypass." + kitKey)) return 0;

        return plugin.getConfig().getLong("kit-cooldowns." + kitKey, 0);
    }

    public String formatTime(long seconds) {
        if (seconds >= 60) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        }
        return seconds + "s";
    }
}