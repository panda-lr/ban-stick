# 🔨 Ban Stick

**Ban Stick** is a lightweight PaperMC moderation plugin that lets staff ban players quickly and cleanly using a GUI — no more typing long commands or remembering syntax.

Right-click a player with the **Ban Stick**, or use `/banmenu`, to open a full menu where you can:
- Choose ban duration (1h, 1d, 7d, custom length, or permanent)
- Choose or type a reason (preset or custom)
- Confirm — and the player is banned (or temp-banned)

Also includes a **dry-run test mode** so you can safely test menus without actually banning anyone.

---

## ⚙️ Features

✅ Quick & visual ban GUI  
✅ Multiple durations: 1 hour, 1 day, 7 days, custom hours, permanent  
✅ Preset reasons (Hacking, Griefing, Spam, etc.)  
✅ Custom reason via chat  
✅ `/banmenu test` dry-run mode (for safe testing)  
✅ All temp bans saved to `bans.yml`  
✅ Automatically unbans players after their time expires  
✅ Fully integrated with the Paper API — no dependencies  

---

## 💬 Commands

| Command | Description | Permission | Default |
|----------|--------------|-------------|----------|
| `/banstick` | Gives you the Ban Stick item | `banstick.get` | OP |
| `/banmenu <player>` | Opens the real Ban Menu GUI for the target player | `banstick.use` | OP |
| `/banmenu test <player>` | Opens the **dry-run** Ban Menu (no bans applied, safe testing) | `banstick.use` | OP |

---

## 🧰 Permissions

| Permission | Description | Default |
|-------------|--------------|----------|
| `banstick.get` | Allows giving yourself the Ban Stick via `/banstick` | OP |
| `banstick.use` | Allows using the Ban Stick or the `/banmenu` command | OP |

---

## 🧠 How to Use

1. Make sure you’re OP or have the correct permissions.  
2. Run `/banstick` to get your Ban Stick (a Blaze Rod).  
3. Right-click any player to open the Ban Menu.  
4. Choose a duration, then a reason — and confirm.  
5. To test safely, use `/banmenu test <player>` which won’t actually ban anyone.

---

## 📂 File Structure

Temp bans are stored in:

/plugins/BanStick/bans.yml

Each entry includes the player UUID, expiry time, and reason.  
Expired bans are automatically removed when the player rejoins.

---

## 🧪 Testing Mode

For testing menus or configuration without banning anyone:

/banmenu test <yourname>

You’ll see [DRY RUN] messages in chat and console — no bans are saved or applied.

---
	
## 🧩 Compatibility

Minecraft 1.20.x – 1.21.x

Paper / Purpur compatible

No external dependencies

Works fine with LuckPerms or any other permission plugin

---

## 📜 License

MIT License
(c) 2025 [panda-lr]

Feel free to fork, modify, and use this in your own projects.

---
