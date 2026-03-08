package github.catchaos8.catsKits;

import org.bukkit.entity.Player;

public class MessageManager {

    private final CatsKits plugin;

    public MessageManager(CatsKits plugin) {
        this.plugin = plugin;
    }

    public String get(String key) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6Kits§8] §r");
        String msg = plugin.getConfig().getString("messages." + key, "§cMessage not found: " + key);
        return msg.replace("{prefix}", prefix);
    }

    public String get(String key, String... replacements) {
        String msg = get(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public void send(Player player, String key, String... replacements) {
        player.sendMessage(get(key, replacements));
    }
}