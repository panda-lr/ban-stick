package com.yourname.banstick;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class BanStickPlugin extends JavaPlugin {

    private NamespacedKey stickKey;
    private BanManager banManager;
    private BanMenu banMenu;

    @Override
    public void onEnable() {
        getLogger().info("BanStick enabled");

        // key used to mark the item
        stickKey = new NamespacedKey(this, "is_ban_stick");

        // load ban data manager
        banManager = new BanManager(this);
        banManager.loadBans();

        // setup menu helper
        banMenu = new BanMenu(this);

        // register listeners
        Bukkit.getPluginManager().registerEvents(new BanListener(this), this);
        Bukkit.getPluginManager().registerEvents(banManager, this); // listens to PlayerLoginEvent
        Bukkit.getPluginManager().registerEvents(banMenu, this);    // listens for menu clicks
    }

    @Override
    public void onDisable() {
        banManager.saveBans();
        getLogger().info("BanStick disabled");
    }

    public BanManager getBanManager() {
        return banManager;
    }

    public BanMenu getBanMenu() {
        return banMenu;
    }

    // Create the Ban Stick item
    public ItemStack createBanStick() {
        ItemStack stick = new ItemStack(Material.BLAZE_ROD, 1); // blaze rod so it's more "admin"
        ItemMeta meta = stick.getItemMeta();

        meta.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Ban Stick");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Right-click a player to open");
        lore.add(ChatColor.GRAY + "the ban menu.");
        lore.add(ChatColor.DARK_GRAY + "Temp / Perm bans");
        meta.setLore(lore);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // tag the item so we know it's legit
        meta.getPersistentDataContainer().set(stickKey, PersistentDataType.INTEGER, 1);

        stick.setItemMeta(meta);
        return stick;
    }

    // Check if an item is the Ban Stick
    public boolean isBanStick(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.BLAZE_ROD) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        Integer marker = meta.getPersistentDataContainer().get(stickKey, PersistentDataType.INTEGER);
        return marker != null && marker == 1;
    }

    // /banstick command to give the stick
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    
        String cmd = command.getName().toLowerCase();
    
        // /banstick command — gives the Ban Stick item
        if (cmd.equals("banstick")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
    
            Player p = (Player) sender;
    
            if (!p.hasPermission("banstick.get")) {
                p.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
    
            p.getInventory().addItem(createBanStick());
            p.sendMessage(ChatColor.GREEN + "You have been given the Ban Stick.");
            return true;
        }
    
        // /banmenu [test] <player> — opens the ban menu (optionally in dry-run mode)
        if (cmd.equals("banmenu")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
    
            if (!sender.hasPermission("banstick.use")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
    
            Player staff = (Player) sender;
    
            if (args.length == 0) {
                staff.sendMessage(ChatColor.YELLOW + "Usage: /banmenu <player> OR /banmenu test <player>");
                return true;
            }
    
            boolean isTest = args[0].equalsIgnoreCase("test");
            String targetName = isTest && args.length >= 2 ? args[1] : args[0];
    
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                staff.sendMessage(ChatColor.RED + "Player not found or not online: " + targetName);
                return true;
            }
    
            if (isTest) {
                getBanMenu().openBanMenu(staff, target, true);
                staff.sendMessage(ChatColor.GREEN + "Opened DRY-RUN ban menu for " + target.getName() + ". (No real bans will be applied.)");
            } else {
                getBanMenu().openBanMenu(staff, target, false);
            }
    
            return true;
        }
    
        return false;
    }    
}
