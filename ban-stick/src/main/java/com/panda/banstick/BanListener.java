package com.yourname.banstick;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class BanListener implements Listener {

    private final BanStickPlugin plugin;

    public BanListener(BanStickPlugin plugin) {
        this.plugin = plugin;
    }

    // RIGHT CLICK -> open menu
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

        // don't allow self
        if (staff.getUniqueId().equals(target.getUniqueId())) {
            staff.sendMessage(ChatColor.RED + "You can't ban yourself.");
            return;
        }

        // open GUI (normal, not dry run)
        plugin.getBanMenu().openBanMenu(staff, target, false);
    }

    // LEFT CLICK (hit) -> immediate funny perm ban
    @EventHandler
    public void onHitWithBanStick(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player staff)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        // only if staff is holding the ban stick
        ItemStack inHand = staff.getInventory().getItemInMainHand();
        if (!plugin.isBanStick(inHand)) return;

        // permission check
        if (!staff.hasPermission("banstick.use")) {
            staff.sendMessage(ChatColor.RED + "You are not allowed to use the Ban Stick.");
            return;
        }

        // stop normal damage (we're doing our own thing)
        event.setCancelled(true);

        // funny knockback
        Vector dir = target.getLocation().toVector().subtract(staff.getLocation().toVector()).normalize();
        dir.setY(0.4); // little lift
        target.setVelocity(dir.multiply(1.1));

        // lightning at target
        Location loc = target.getLocation();
        target.getWorld().strikeLightningEffect(loc); // effect only; use strikeLightning if you want damage

        // broadcast funny message
        String msg = ChatColor.DARK_RED + target.getName()
                + ChatColor.GRAY + " has been smitten by "
                + ChatColor.GOLD + staff.getName()
                + ChatColor.GRAY + " for crimes against the server.";
        Bukkit.getServer().broadcastMessage(msg);

        // perm ban with funny reason + log handled in BanManager
        plugin.getBanManager().permBanPlayer(
                target,
                "Smited by Ban Stick",
                staff
        );
    }
}
