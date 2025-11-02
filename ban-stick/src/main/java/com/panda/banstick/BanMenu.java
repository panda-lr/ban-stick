package com.yourname.banstick;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * BanMenu supports:
 * 1) Duration Menu
 *    - 1h, 1d, 7d, Custom Length (hours), PERMA, Cancel
 * 2) Reason Menu
 *    - preset reasons, custom reason via chat
 *
 * Dry-run mode mirrors real actions but does not actually ban/kick.
 */
public class BanMenu implements Listener {

    private final BanStickPlugin plugin;

    // Which target this staff member is acting on
    // menuTarget[staffUUID] = targetUUID
    private final Map<UUID, UUID> menuTarget = new HashMap<>();

    // Whether this staff member is in dry-run
    private final Map<UUID, Boolean> dryRun = new HashMap<>();

    // PendingBan is the ban we're about to apply once we have duration + reason
    private final Map<UUID, PendingBan> pendingBan = new HashMap<>();

    // Waiting for staff to type a custom reason in chat
    private final Set<UUID> awaitingCustomReason = new HashSet<>();

    // Waiting for staff to type a custom duration (in hours) in chat
    private final Set<UUID> awaitingCustomDuration = new HashSet<>();

    /**
     * PendingBan:
     * - targetUUID: who will be banned
     * - durationMs: how long (ms). If null or <0 => perm ban
     * - dry: dry-run mode?
     * We first fill duration, then later applyReason -> finalise.
     */
    private static class PendingBan {
        UUID targetUUID;
        Long durationMs;   // null or <0 => perm ban
        boolean dry;

        PendingBan(UUID targetUUID, Long durationMs, boolean dry) {
            this.targetUUID = targetUUID;
            this.durationMs = durationMs;
            this.dry = dry;
        }

        boolean isPerm() {
            return durationMs == null || durationMs < 0;
        }
    }

    public BanMenu(BanStickPlugin plugin) {
        this.plugin = plugin;
    }

    /* ----------------------- UI helpers ----------------------- */

    private ItemStack menuItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        for (String l : loreLines) {
            lore.add(ChatColor.GRAY + l);
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private String durationTitle(String targetName) {
        return ChatColor.DARK_RED + "Ban " + targetName;
    }

    private String reasonTitle(String targetName) {
        return ChatColor.DARK_RED + "Ban " + targetName + ChatColor.DARK_GRAY + " — Reason";
    }

    private boolean isDurationMenu(String title) {
        if (title == null) return false;
        String t = ChatColor.stripColor(title);
        return t.startsWith("Ban ") && !t.contains("Reason");
    }

    private boolean isReasonMenu(String title) {
        if (title == null) return false;
        String t = ChatColor.stripColor(title);
        return t.startsWith("Ban ") && t.contains("Reason");
    }

    /* ----------------------- Open Menus ----------------------- */

    /** Open first menu (duration) */
    public void openBanMenu(Player staff, Player target) {
        openBanMenu(staff, target, false);
    }

    /** Open first menu (duration) with dry-run option */
    public void openBanMenu(Player staff, Player target, boolean isDryRun) {

        Inventory inv = Bukkit.createInventory(
                staff,
                9,
                durationTitle(target.getName())
        );

        // Slot layout:
        // [0] 1 Hour
        // [1] 1 Day
        // [2] 7 Days
        // [3] Custom Length (hours)
        // [5] PERMA BAN
        // [8] Cancel

        inv.setItem(0, menuItem(
                Material.YELLOW_WOOL,
                ChatColor.GOLD + "Temp Ban 1 Hour",
                "Kick and block for 1 hour."
        ));

        inv.setItem(1, menuItem(
                Material.ORANGE_WOOL,
                ChatColor.RED + "Temp Ban 1 Day",
                "Kick and block for 24 hours."
        ));

        inv.setItem(2, menuItem(
                Material.RED_CONCRETE,
                ChatColor.DARK_RED + "Temp Ban 7 Days",
                "Kick and block for 7 days."
        ));

        inv.setItem(3, menuItem(
                Material.CLOCK,
                ChatColor.AQUA + "Custom Length (hours)",
                "Click, then type number of hours in chat.",
                "Type 'cancel' to abort."
        ));

        inv.setItem(5, menuItem(
                Material.RED_WOOL,
                ChatColor.DARK_RED + "" + ChatColor.BOLD + "PERMA BAN",
                "Kick and permanently ban."
        ));

        inv.setItem(8, menuItem(
                Material.BARRIER,
                ChatColor.GRAY + "Cancel",
                "Close menu"
        ));

        // remember context so we know who we're targeting
        menuTarget.put(staff.getUniqueId(), target.getUniqueId());
        dryRun.put(staff.getUniqueId(), isDryRun);

        staff.openInventory(inv);
    }

    /** Open second menu (reason) */
    private void openReasonMenu(Player staff, Player target) {
        Inventory inv = Bukkit.createInventory(
                staff,
                27,
                reasonTitle(target.getName())
        );

        // Common reasons
        inv.setItem(10, menuItem(Material.DIAMOND_SWORD, ChatColor.AQUA + "Hacking/Cheats",
                "Disallowed clients, kill aura, fly, etc."));
        inv.setItem(11, menuItem(Material.TNT, ChatColor.GOLD + "Griefing",
                "Destroying builds, stealing, sabotage."));
        inv.setItem(12, menuItem(Material.PAPER, ChatColor.YELLOW + "Spam / Ads",
                "Chat flooding, self-promo, server IPs."));
        inv.setItem(13, menuItem(Material.WITHER_ROSE, ChatColor.RED + "Toxicity / Harassment",
                "Abusive language, slurs, threats."));
        inv.setItem(14, menuItem(Material.CHEST_MINECART, ChatColor.LIGHT_PURPLE + "Exploiting / Duping",
                "Dupes, glitches, unfair advantage."));
        inv.setItem(15, menuItem(Material.GOLD_ORE, ChatColor.GREEN + "X-Ray / Resource Cheats",
                "Unfair ore detection, x-ray clients."));
        inv.setItem(16, menuItem(Material.REDSTONE_TORCH, ChatColor.BLUE + "AFK Macros / Automation",
                "Macro fishing, autoclickers, etc."));

        // Custom reason
        inv.setItem(22, menuItem(Material.ANVIL, ChatColor.WHITE + "Custom (type in chat)",
                "Click, then type your reason in chat.",
                "Type 'cancel' to abort."
        ));

        // Cancel
        inv.setItem(26, menuItem(Material.BARRIER, ChatColor.GRAY + "Cancel", "Abort ban"));

        staff.openInventory(inv);
    }

    /* ----------------------- Inventory Click (all menus) ----------------------- */

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player staff)) return;

        String title = event.getView().getTitle();
        if (!isDurationMenu(title) && !isReasonMenu(title)) return;

        event.setCancelled(true); // block taking items

        UUID staffId = staff.getUniqueId();
        UUID targetId = menuTarget.get(staffId);
        Player target = (targetId != null) ? Bukkit.getPlayer(targetId) : null;

        // If target logged off while we're deciding
        if (target == null) {
            staff.sendMessage(ChatColor.RED + "That player is no longer online.");
            cleanup(staffId);
            staff.closeInventory();
            return;
        }

        int slot = event.getRawSlot();

        /* ----- DURATION MENU ACTIONS ----- */
        if (isDurationMenu(title)) {

            boolean isDry = dryRun.getOrDefault(staffId, false);

            if (slot == 0) {
                // 1 hour
                long oneHourMs = 60L * 60L * 1000L;
                pendingBan.put(staffId, new PendingBan(target.getUniqueId(), oneHourMs, isDry));
                openReasonMenu(staff, target);
                return;
            }

            if (slot == 1) {
                // 1 day
                long oneDayMs = 24L * 60L * 60L * 1000L;
                pendingBan.put(staffId, new PendingBan(target.getUniqueId(), oneDayMs, isDry));
                openReasonMenu(staff, target);
                return;
            }

            if (slot == 2) {
                // 7 days
                long sevenDayMs = 7L * 24L * 60L * 60L * 1000L;
                pendingBan.put(staffId, new PendingBan(target.getUniqueId(), sevenDayMs, isDry));
                openReasonMenu(staff, target);
                return;
            }

            if (slot == 3) {
                // Custom length in hours
                awaitingCustomDuration.add(staffId);

                // we haven't set pendingBan yet, but we know staff+target+dry
                // We'll create PendingBan after they type the number of hours
                staff.closeInventory();
                staff.sendMessage(ChatColor.AQUA + "Type number of hours to ban "
                        + target.getName() + " for (or 'cancel').");
                return;
            }

            if (slot == 5) {
                // PERMA BAN
                pendingBan.put(staffId, new PendingBan(target.getUniqueId(), -1L, isDry));
                openReasonMenu(staff, target);
                return;
            }

            if (slot == 8) {
                staff.sendMessage(ChatColor.GRAY + "Cancelled.");
                cleanup(staffId);
                staff.closeInventory();
                return;
            }

            return;
        }

        /* ----- REASON MENU ACTIONS ----- */
        if (isReasonMenu(title)) {

            // Slot -> reason mappings
            Map<Integer, String> reasons = Map.of(
                    10, "Hacking/Cheats",
                    11, "Griefing",
                    12, "Spam/Ads",
                    13, "Toxicity/Harassment",
                    14, "Exploiting/Duping",
                    15, "X-Ray/Resource Cheats",
                    16, "AFK Macros/Automation"
            );

            if (reasons.containsKey(slot)) {
                String reason = reasons.get(slot);
                PendingBan pb = pendingBan.get(staffId);

                if (pb == null) {
                    staff.sendMessage(ChatColor.RED + "No pending ban found.");
                    cleanup(staffId);
                    staff.closeInventory();
                    return;
                }

                applyBanWithReason(staff, pb, reason);
                staff.closeInventory();
                return;
            }

            if (slot == 22) {
                // custom reason via chat
                awaitingCustomReason.add(staffId);
                staff.closeInventory();
                staff.sendMessage(ChatColor.YELLOW + "Type your custom ban reason in chat. "
                        + "Type 'cancel' to abort.");
                return;
            }

            if (slot == 26) {
                staff.sendMessage(ChatColor.GRAY + "Cancelled.");
                cleanup(staffId);
                staff.closeInventory();
                return;
            }
        }
    }

    /* ----------------------- Chat Handlers ----------------------- */

    // Handle custom duration entry (hours)
    @EventHandler
    public void onChatCustomDuration(AsyncPlayerChatEvent event) {
        Player staff = event.getPlayer();
        UUID staffId = staff.getUniqueId();

        if (!awaitingCustomDuration.contains(staffId)) return;

        event.setCancelled(true);

        String msg = event.getMessage().trim();

        // user aborts
        if (msg.equalsIgnoreCase("cancel")) {
            awaitingCustomDuration.remove(staffId);
            // if they cancel at duration step, we haven't created pendingBan yet
            menuTarget.remove(staffId);
            dryRun.remove(staffId);

            new BukkitRunnable() {
                @Override
                public void run() {
                    staff.sendMessage(ChatColor.GRAY + "Ban cancelled.");
                }
            }.runTask(plugin);
            return;
        }

        // parse hours
        long hours;
        try {
            hours = Long.parseLong(msg);
        } catch (NumberFormatException ex) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    staff.sendMessage(ChatColor.RED + "Not a number. Ban cancelled.");
                }
            }.runTask(plugin);

            awaitingCustomDuration.remove(staffId);
            menuTarget.remove(staffId);
            dryRun.remove(staffId);
            return;
        }

        if (hours <= 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    staff.sendMessage(ChatColor.RED + "Duration must be > 0. Ban cancelled.");
                }
            }.runTask(plugin);

            awaitingCustomDuration.remove(staffId);
            menuTarget.remove(staffId);
            dryRun.remove(staffId);
            return;
        }

        // convert hours -> ms
        long durationMs = hours * 60L * 60L * 1000L;

        // create PendingBan now, on main thread
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID targetId = menuTarget.get(staffId);
                Player target = (targetId != null) ? Bukkit.getPlayer(targetId) : null;
                boolean isDry = dryRun.getOrDefault(staffId, false);

                if (target == null) {
                    staff.sendMessage(ChatColor.RED + "That player is no longer online.");
                    cleanup(staffId);
                    awaitingCustomDuration.remove(staffId);
                    return;
                }

                pendingBan.put(staffId, new PendingBan(target.getUniqueId(), durationMs, isDry));
                awaitingCustomDuration.remove(staffId);

                // now open reason menu
                openReasonMenu(staff, target);
            }
        }.runTask(plugin);
    }

    // Handle custom reason entry
    @EventHandler
    public void onChatCustomReason(AsyncPlayerChatEvent event) {
        Player staff = event.getPlayer();
        UUID staffId = staff.getUniqueId();

        if (!awaitingCustomReason.contains(staffId)) return;

        event.setCancelled(true);

        String msg = event.getMessage().trim();

        if (msg.equalsIgnoreCase("cancel")) {
            awaitingCustomReason.remove(staffId);
            pendingBan.remove(staffId);
            menuTarget.remove(staffId);
            dryRun.remove(staffId);

            new BukkitRunnable() {
                @Override
                public void run() {
                    staff.sendMessage(ChatColor.GRAY + "Custom reason cancelled.");
                }
            }.runTask(plugin);
            return;
        }

        PendingBan pb = pendingBan.get(staffId);
        if (pb == null) {
            awaitingCustomReason.remove(staffId);
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                applyBanWithReason(staff, pb, msg);
                awaitingCustomReason.remove(staffId);
            }
        }.runTask(plugin);
    }

    /* ----------------------- Inventory Close Cleanup ----------------------- */

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player staff)) return;

        String title = event.getView().getTitle();

        // If they close Duration or Reason menu, we MIGHT clean up,
        // but not if we're intentionally waiting on chat input.
        UUID staffId = staff.getUniqueId();

        boolean waitingOnDuration = awaitingCustomDuration.contains(staffId);
        boolean waitingOnReason = awaitingCustomReason.contains(staffId);

        if (isDurationMenu(title) || isReasonMenu(title)) {
            if (!waitingOnDuration && !waitingOnReason) {
                cleanup(staffId);
            }
        }
    }

    private void cleanup(UUID staffId) {
        // Don't touch awaitingCustomDuration/Reason here
        // because those states mean "we're waiting on them"
        if (!awaitingCustomDuration.contains(staffId) && !awaitingCustomReason.contains(staffId)) {
            menuTarget.remove(staffId);
            dryRun.remove(staffId);
            pendingBan.remove(staffId);
        }
    }

    /* ----------------------- Apply Ban ----------------------- */

    private void applyBanWithReason(Player staff, PendingBan pb, String reason) {
        UUID staffId = staff.getUniqueId();
        UUID targetId = pb.targetUUID;
        Player target = Bukkit.getPlayer(targetId);

        if (target == null) {
            staff.sendMessage(ChatColor.RED + "That player is no longer online.");
            cleanup(staffId);
            return;
        }

        if (pb.dry) {
            String durationText = pb.isPerm() ? "PERMANENT" : humanDuration(pb.durationMs);
            staff.sendMessage(ChatColor.GREEN + "[DRY RUN] Would ban " + target.getName()
                    + " for " + durationText + " (Reason: " + reason + ").");
            plugin.getLogger().info("[DRY RUN] " + staff.getName() + " -> " + durationText
                    + " ban " + target.getName() + " | Reason: " + reason);
        } else {
            if (pb.isPerm()) {
                plugin.getBanManager().permBanPlayer(target, reason, staff);
                staff.sendMessage(ChatColor.DARK_RED + "Permanently banned " + target.getName()
                        + " for: " + ChatColor.GRAY + reason);
            } else {
                plugin.getBanManager().tempBanPlayer(target, pb.durationMs, reason, staff);
                staff.sendMessage(ChatColor.RED + "Banned " + target.getName() + " for "
                        + ChatColor.GOLD + humanDuration(pb.durationMs)
                        + ChatColor.RED + " (Reason: " + ChatColor.GRAY + reason + ChatColor.RED + ").");
            }
        }

        // cleanup final
        awaitingCustomReason.remove(staffId);
        awaitingCustomDuration.remove(staffId);
        cleanup(staffId);
    }

    private String humanDuration(long ms) {
        long totalSeconds = ms / 1000;
        long days = totalSeconds / 86400;
        totalSeconds %= 86400;
        long hours = totalSeconds / 3600;
        totalSeconds %= 3600;
        long minutes = totalSeconds / 60;

        List<String> parts = new ArrayList<>();
        if (days > 0) parts.add(days + "d");
        if (hours > 0) parts.add(hours + "h");
        if (minutes > 0) parts.add(minutes + "m");
        if (parts.isEmpty()) {
            parts.add((ms / 1000) + "s");
        }
        return String.join(" ", parts);
    }
}
