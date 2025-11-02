package com.yourname.banstick;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

public class BanListener implements Listener {

    private final BanStickPlugin plugin;

    public BanListener(BanStickPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUseBanStick(PlayerInteractEntityEvent event) {
        Player staff = event.getPlayer();
        Entity clicked = event.getRightClicked();

        if (!(clicked instanceof Player target)) {
            return;
        }

        // must hold the ban stick
        ItemStack inHand = staff.getInventory().getItemInMainHand();
        if (!plugin.isBanStick(inHand)) {
            return;
        }

        // permission check
        if (!staff.hasPermission("banstick.use")) {
            staff.sendMessage(ChatColor.RED + "You are not allowed to use the Ban Stick.");
            return;
        }

        // don't allow self-ban
        if (staff.getUniqueId().equals(target.getUniqueId())) {
            staff.sendMessage(ChatColor.RED + "You can't ban yourself, relax.");
            return;
        }

        // Open menu
        plugin.getBanMenu().openBanMenu(staff, target);
    }
}
