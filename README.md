# 🔨 Ban Stick

**Ban Stick** is a lightweight **PaperMC** moderation plugin that lets staff ban players quickly, cleanly, and dramatically ⚡

Right-click a player with the **Ban Stick**, or use `/banmenu`, to open a full GUI menu for choosing a duration and reason —  
or simply **left-click** to instantly smite them with lightning and a permanent ban.

No need to type long commands ever again — it’s fast, visual, and fun.

---

## ⚙️ Features

✅ Quick & visual **ban GUI** (no commands needed)  
✅ **Instant left-click ban** — knocks back, strikes lightning, perm bans with a funny broadcast  
✅ Ban durations: **1h, 1d, 7d, custom hours, permanent**  
✅ Preset reasons (Hacking, Griefing, Spam, etc.)  
✅ Custom reason via chat  
✅ `/banmenu test` dry-run mode (safe GUI testing)  
✅ All temp bans saved to `bans.yml`  
✅ All bans (temp + perm) **logged to `banlog.txt`** with timestamp, staff, and reason  
✅ Auto-unban for expired temp bans  
✅ Fully integrated with the **Paper API** — zero dependencies  

---

## 💬 Commands

| Command | Description | Permission | Default |
|----------|-------------|-------------|----------|
| `/banstick` | Gives you the Ban Stick item | `banstick.get` | OP |
| `/banmenu <player>` | Opens the Ban Menu GUI for the target player | `banstick.use` | OP |
| `/banmenu test <player>` | Opens the **dry-run** Ban Menu (no bans applied, for testing) | `banstick.use` | OP |

---

## 🧰 Permissions

| Permission | Description | Default |
|-------------|-------------|----------|
| `banstick.get` | Allows giving yourself the Ban Stick via `/banstick` | OP |
| `banstick.use` | Allows using the Ban Stick or `/banmenu` | OP |

---

## 🧠 How to Use

1. Make sure you’re OP or have the required permissions.  
2. Run `/banstick` to get your Ban Stick (a Blaze Rod).  
3. **Right-click** a player → opens the Ban Menu GUI.  
4. Choose a duration and reason to confirm.  
5. **Left-click** a player → immediately strikes lightning ⚡ and permanently bans them with a funny global message.  
6. Use `/banmenu test <player>` to safely test the menus — no bans applied.

---

## 📂 File Structure

| File | Purpose |
|------|----------|
| `plugins/BanStick/bans.yml` | Active **temporary bans** with expiry time and reason |
| `plugins/BanStick/banlog.txt` | **Permanent log** of *all* bans (temp + perm), including timestamps and staff |

Expired temp bans are automatically removed when the player rejoins.

---

## 🧪 Testing Mode

For testing menus or configuration without banning anyone:

/banmenu test <yourname>

You’ll see [DRY RUN] messages in chat and console — no bans are saved or applied.

## ⚡ Instant Lightning Ban

Hit a player (left-click) while holding the Ban Stick to:
- Knock them back slightly  
- Strike them with lightning (effect only)  
- Announce a funny broadcast message  
- Permanently ban them instantly  

All lightning bans are also written to `banlog.txt`.

---

## 🧩 Compatibility

- **Minecraft:** 1.20.x – 1.21.x  
- **Server:** Paper / Purpur  
- **Dependencies:** None  
- **Permissions:** Works perfectly with LuckPerms or similar

---

## 🧱 Building from Source

**Requirements:**
- Java 17+  
- Maven 3.8+  

**Steps:**
```bash
git clone https://github.com/panda-lr/ban-stick.git
cd ban-stick
mvn package

The compiled plugin will appear at:

target/banstick-1.0.0.jar

Drop it into your Paper server’s /plugins folder and restart.

---

❤️ Credits

This project was inspired by the original Ban Stick (SpigotMC)

by HuganicFirtic, created for older Minecraft versions.
This plugin is a modern re-imagining built from scratch for Paper 1.20+
with new GUI menus, temp bans, logging, and instant lightning smites.

---

## 📜 License

MIT License
(c) 2025 [panda-lr]

Feel free to fork, modify, and use this in your own projects.

---
