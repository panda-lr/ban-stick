package com.yourname.banstick;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BanManager implements Listener {

    private final BanStickPlugin plugin;

    // tempBans[uuid] = BanInfo
    private final Map<UUID, BanInfo> tempBans = new HashMap<>();

    private File bansFile;
    private YamlConfiguration bansConfig;

    // log file for ALL bans (temp + perm)
    private File banLogFile;

    public BanManager(BanStickPlugin plugin) {
        this.plugin = plugin;
    }

    // Represents one temp ban
    public static class BanInfo {
        public long until;       // epoch millis
        public String reason;
        public BanInfo(long until, String reason) {
            this.until = until;
            this.reason = reason;
        }
    }

    public void loadBans() {
        // temp bans
        bansFile = new File(plugin.getDataFolder(), "bans.yml");
        if (!bansFile.exists()) {
            bansFile.getParentFile().mkdirs();
            try {
                bansFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create bans.yml: " + e.getMessage());
            }
        }

        bansConfig = YamlConfiguration.loadConfiguration(bansFile);
        tempBans.clear();

        if (bansConfig.contains("bans")) {
            for (String uuidStr : bansConfig.getConfigurationSection("bans").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                long until = bansConfig.getLong("bans." + uuidStr + ".until");
                String reason = bansConfig.getString("bans." + uuidStr + ".reason", "Banned");
                tempBans.put(uuid, new BanInfo(until, reason));
            }
        }

        // log file
        banLogFile = new File(plugin.getDataFolder(), "banlog.txt");
        if (!banLogFile.exists()) {
            try {
                banLogFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create banlog.txt: " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + tempBans.size() + " temp bans.");
    }

    public void saveBans() {
        if (bansConfig == null) return;

        bansConfig.set("bans", null); // wipe then rewrite
        for (Map.Entry<UUID, BanInfo> entry : tempBans.entrySet()) {
            String key = "bans." + entry.getKey().toString();
            bansConfig.set(key + ".until", entry.getValue().until);
            bansConfig.set(key + ".reason", entry.getValue().reason);
        }

        try {
            bansConfig.save(bansFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save bans.yml: " + e.getMessage());
        }
    }

    // helper to append to banlog.txt
    private void logBan(String staffName, String targetName, String type, String reason, Long untilMillis) {
        if (banLogFile == null) return;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        String time = fmt.format(Instant.now());

        String untilStr = (untilMillis == null) ? "-" : fmt.format(Instant.ofEpochMilli(untilMillis));

        String line = String.format(
                "[%s] staff=%s target=%s type=%s reason=%s until=%s%n",
                time,
                staffName,
                targetName,
                type,
                reason,
                untilStr
        );

        try (FileWriter fw = new FileWriter(banLogFile, true)) {
            fw.write(line);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not write to banlog.txt: " + e.getMessage());
        }
    }

    // Create a temp ban
    public void tempBanPlayer(Player target, long durationMillis, String reason, Player staff) {
        long until = System.currentTimeMillis() + durationMillis;
        tempBans.put(target.getUniqueId(), new BanInfo(until, reason));
        saveBans();

        // log
        logBan(
                staff != null ? staff.getName() : "CONSOLE",
                target.getName(),
                "TEMP",
                reason,
                until
        );

        // kick them immediately
        target.kickPlayer(ChatColor.RED + "You are temporarily banned.\n"
                + ChatColor.GRAY + "Reason: " + reason + "\n"
                + ChatColor.GRAY + "Until: " + Instant.ofEpochMilli(until));

        Bukkit.getLogger().info((staff != null ? staff.getName() : "CONSOLE") + " temp-banned " + target.getName()
                + " until " + until + " for: " + reason);
    }

    // Create a perm ban (uses server's ban list)
    public void permBanPlayer(Player target, String reason, Player staff) {
        Bukkit.getBanList(BanList.Type.NAME).addBan(
                target.getName(),
                reason,
                null, // no expiry = perm
                staff != null ? staff.getName() : "CONSOLE"
        );

        // log
        logBan(
                staff != null ? staff.getName() : "CONSOLE",
                target.getName(),
                "PERM",
                reason,
                null
        );

        target.kickPlayer(ChatColor.DARK_RED + "You are permanently banned.\n"
                + ChatColor.GRAY + "Reason: " + reason);

        Bukkit.getLogger().info((staff != null ? staff.getName() : "CONSOLE") + " perm-banned " + target.getName()
                + " for: " + reason);
    }

    // When a player tries to join, stop them if they are still temp banned
    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!tempBans.containsKey(uuid)) return;

        BanInfo info = tempBans.get(uuid);
        long now = System.currentTimeMillis();

        if (now >= info.until) {
            // ban expired
            tempBans.remove(uuid);
            saveBans();
            return;
        }

        event.disallow(
                PlayerLoginEvent.Result.KICK_BANNED,
                ChatColor.RED + "You are temporarily banned.\n"
                        + ChatColor.GRAY + "Reason: " + info.reason + "\n"
                        + ChatColor.GRAY + "Until: " + Instant.ofEpochMilli(info.until)
        );
    }
}
