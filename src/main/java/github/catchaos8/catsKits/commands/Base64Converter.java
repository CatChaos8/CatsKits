package github.catchaos8.catsKits.commands;

import github.catchaos8.catsKits.CatsKits;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;

@SuppressWarnings("UnstableApiUsage")
public class Base64Converter implements BasicCommand {

    private final CatsKits plugin;

    public Base64Converter(CatsKits plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            stack.getSender().sendMessage("Players only.");
            return;
        }

        if (!player.hasPermission("kits.admin")) {
            player.sendMessage("§cNo permission.");
            return;
        }

        var item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage("§cHold an item in your main hand.");
            return;
        }

        String base64 = Base64.getEncoder().encodeToString(item.serializeAsBytes());
        player.sendMessage("§aBase64 for §e" + item.getType().name() + "§a:");
        player.sendMessage(base64);
        plugin.getLogger().info("Base64 for " + item.getType().name() + ": " + base64);
    }
}