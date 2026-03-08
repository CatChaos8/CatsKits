package github.catchaos8.catsKits;

import github.catchaos8.catsKits.commands.Base64Converter;
import github.catchaos8.catsKits.commands.KitsCommand;
import github.catchaos8.catsKits.gui.KitEditorGui;
import github.catchaos8.catsKits.gui.KitGui;
import github.catchaos8.catsKits.kits.CustomKitItemLoader;
import github.catchaos8.catsKits.kits.DefaultKitLoader;
import github.catchaos8.catsKits.kits.KitCooldownManager;
import github.catchaos8.catsKits.kits.KitManager;
import github.catchaos8.catsKits.listeners.GuiListener;
import github.catchaos8.catsKits.listeners.KillListener;
import github.catchaos8.catsKits.listeners.KitEditorListener;
import github.catchaos8.catsKits.listeners.KitListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class CatsKits extends JavaPlugin {

    private KitManager kitManager;
    private KitGui kitGui;
    private KitEditorGui kitEditorGui;
    private DefaultKitLoader defaultKitLoader;
    private GuiListener guiListener;
    private CustomKitItemLoader customKitItemLoader;
    private KitCooldownManager cooldownManager;
    private KillListener killListener;
    private MessageManager messageManager;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        defaultKitLoader = new DefaultKitLoader(this);
        customKitItemLoader = new CustomKitItemLoader(this); // add this
        kitManager = new KitManager(this);
        kitGui = new KitGui(this);
        kitEditorGui = new KitEditorGui(this);
        guiListener = new GuiListener(this);
        cooldownManager = new KitCooldownManager(this);
        messageManager = new MessageManager(this);


        // Pre-cache material list async so first click doesn't freeze server
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            List<Material> cache = new ArrayList<>();
            for (Material mat : Material.values()) {
                try {
                    if (mat.isItem() && !mat.isAir() && !mat.isLegacy()) {
                        cache.add(mat);
                    }
                } catch (Exception ignored) {}
            }
            KitEditorGui.ALL_ITEMS = cache;
            getLogger().info("Material cache built: " + cache.size() + " items.");
        });

        getServer().getPluginManager().registerEvents(new KitListener(this), this);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(new KitEditorListener(this), this);
        getServer().getPluginManager().registerEvents(new KillListener(this), this);

        KitsCommand kitsCommand = new KitsCommand(this);
        this.registerCommand("kits", kitsCommand);
        this.registerCommand("base64converter", new Base64Converter(this));
    }
    public CustomKitItemLoader getCustomKitItemLoader() { return customKitItemLoader; }
    public KitManager getKitManager() { return kitManager; }
    public KitGui getKitGui() { return kitGui; }
    public KitEditorGui getKitEditorGui() { return kitEditorGui; }
    public DefaultKitLoader getDefaultKitLoader() { return defaultKitLoader; }
    public GuiListener getGuiListener() { return guiListener; }
    public KitCooldownManager getCooldownManager() { return cooldownManager; }
    public MessageManager getMessages() { return messageManager; }

}