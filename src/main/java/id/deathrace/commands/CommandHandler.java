package id.deathrace.commands;

import id.deathrace.listeners.DeathListener;
import id.deathrace.listeners.ExtraListener;
import id.deathrace.managers.GameManager;
import id.deathrace.utils.DeathKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CommandHandler implements CommandExecutor {

    private final GameManager gm;
    private final DeathListener deathListener;
    private final ExtraListener extraListener;

    public CommandHandler(GameManager gm, DeathListener deathListener, ExtraListener extraListener) {
        this.gm = gm;
        this.deathListener = deathListener;
        this.extraListener = extraListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase()) {
            case "regis"        -> handleRegis(sender, args);
            case "unregis"      -> handleUnregis(sender, args);
            case "listplayer"   -> handleListPlayer(sender);
            case "startsolo"    -> handleStart(sender, GameManager.GameMode.SOLO);
            case "startglobal"  -> handleStart(sender, GameManager.GameMode.GLOBAL);
            case "listdeath"    -> handleListDeath(sender);
            case "listdeathall" -> handleListDeathAll(sender);
            case "stopgame"     -> handleStop(sender);
            case "settimer"     -> handleSetTimer(sender, args);
            case "maxhealth"    -> handleMaxHealth(sender, args);
            default             -> false;
        };
    }

    // ── /regis ────────────────────────────────────────────────────────────────

    private boolean handleRegis(CommandSender sender, String[] args) {
        if (!sender.hasPermission("deathrace.admin")) {
            sender.sendMessage(Component.text("❌ Kamu tidak punya permission!", NamedTextColor.RED));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(Component.text("❌ Usage: /regis <playername>", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("❌ Player " + args[0] + " tidak ditemukan / offline!", NamedTextColor.RED));
            return true;
        }
        if (gm.isRunning()) {
            sender.sendMessage(Component.text("❌ Game sedang berjalan! Stop game dulu sebelum register.", NamedTextColor.RED));
            return true;
        }
        boolean success = gm.registerPlayer(target);
        if (success) {
            sender.sendMessage(Component.text("✅ " + target.getName() + " berhasil didaftarkan!", NamedTextColor.GREEN));
            target.sendMessage(Component.text("✅ Kamu telah didaftarkan ke Death Race!", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("⚠ " + target.getName() + " sudah terdaftar!", NamedTextColor.YELLOW));
        }
        return true;
    }

    // ── /unregis ──────────────────────────────────────────────────────────────

    private boolean handleUnregis(CommandSender sender, String[] args) {
        if (!sender.hasPermission("deathrace.admin")) {
            sender.sendMessage(Component.text("❌ Kamu tidak punya permission!", NamedTextColor.RED));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(Component.text("❌ Usage: /unregis <playername>", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null) {
            boolean success = gm.unregisterPlayer(target);
            if (success) {
                deathListener.cleanupPlayer(target.getUniqueId());
                extraListener.cleanupPlayer(target.getUniqueId());
                sender.sendMessage(Component.text("✅ " + target.getName() + " berhasil diunregister!", NamedTextColor.GREEN));
                target.sendMessage(Component.text("⚠ Kamu telah dikeluarkan dari Death Race.", NamedTextColor.YELLOW));
            } else {
                sender.sendMessage(Component.text("⚠ " + target.getName() + " tidak terdaftar!", NamedTextColor.YELLOW));
            }
        } else {
            UUID found = null;
            for (UUID uuid : gm.getRegisteredPlayers()) {
                String offlineName = Bukkit.getOfflinePlayer(uuid).getName();
                if (args[0].equalsIgnoreCase(offlineName)) { found = uuid; break; }
            }
            if (found != null) {
                gm.unregisterByUUID(found);
                deathListener.cleanupPlayer(found);
                extraListener.cleanupPlayer(found);
                sender.sendMessage(Component.text("✅ " + args[0] + " (offline) berhasil diunregister!", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("❌ Player " + args[0] + " tidak ditemukan!", NamedTextColor.RED));
            }
        }
        return true;
    }

    // ── /listplayer ───────────────────────────────────────────────────────────

    private boolean handleListPlayer(CommandSender sender) {
        if (!sender.hasPermission("deathrace.admin")) {
            sender.sendMessage(Component.text("❌ Kamu tidak punya permission!", NamedTextColor.RED));
            return true;
        }
        Set<UUID> players = gm.getRegisteredPlayers();
        if (players.isEmpty()) {
            sender.sendMessage(Component.text("📋 Belum ada player yang terdaftar.", NamedTextColor.YELLOW));
            return true;
        }
        sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  📋 DAFTAR PLAYER DEATH RACE", NamedTextColor.GOLD, TextDecoration.BOLD));
        String modeStr = gm.isRunning() ? (gm.getMode() == GameManager.GameMode.SOLO ? "SOLO" : "GLOBAL") : "WAITING";
        sender.sendMessage(Component.text("  Mode: " + modeStr, NamedTextColor.AQUA));
        if (gm.isRunning()) {
            int remaining = gm.getTimeRemainingSeconds();
            sender.sendMessage(Component.text(String.format("  ⏱ Sisa waktu: %d:%02d", remaining / 60, remaining % 60), NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(gm.getScores().entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : sorted) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = entry.getKey().toString().substring(0, 8);
            boolean online = Bukkit.getPlayer(entry.getKey()) != null;
            sender.sendMessage(Component.text(
                "  " + rank + ". " + (online ? "🟢" : "🔴") + " " + name + " — Score: " + entry.getValue(),
                rank == 1 ? NamedTextColor.GOLD : rank == 2 ? NamedTextColor.GRAY : NamedTextColor.WHITE
            ));
            rank++;
        }
        sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
        return true;
    }

    // ── /startsolo & /startglobal ─────────────────────────────────────────────

    private boolean handleStart(CommandSender sender, GameManager.GameMode mode) {
        if (!sender.hasPermission("deathrace.admin")) {
            sender.sendMessage(Component.text("❌ Kamu tidak punya permission!", NamedTextColor.RED));
            return true;
        }
        if (gm.isRunning()) {
            sender.sendMessage(Component.text("⚠ Game sudah berjalan! Gunakan /stopgame dulu.", NamedTextColor.YELLOW));
            return true;
        }
        if (gm.getRegisteredPlayers().isEmpty()) {
            sender.sendMessage(Component.text("❌ Belum ada player yang terdaftar! Gunakan /regis dulu.", NamedTextColor.RED));
            return true;
        }

        World world;
        Location centerLoc;
        if (sender instanceof Player op) {
            centerLoc = op.getLocation();
            world = op.getWorld();
        } else {
            world = Bukkit.getWorlds().get(0);
            centerLoc = world.getSpawnLocation();
        }

        double borderSize = 10000;

        WorldBorder wb = world.getWorldBorder();
        wb.setCenter(centerLoc);
        wb.setSize(borderSize);

        for (UUID uuid : gm.getRegisteredPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;

            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.getInventory().setArmorContents(new ItemStack[4]);
            p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
            p.setFoodLevel(20);
            p.setSaturation(20f);
            p.setFireTicks(0);
            Location teleLoc = centerLoc.clone();
            int highestY = world.getHighestBlockYAt(teleLoc);
            teleLoc.setY(highestY + 1);
            p.teleport(teleLoc);
        }

        world.setDifficulty(Difficulty.HARD);
        world.setPVP(false);

        gm.startGame(mode, centerLoc, borderSize);

        String modeName = mode == GameManager.GameMode.SOLO ? "SOLO" : "GLOBAL";
        String modeDesc = mode == GameManager.GameMode.SOLO
                ? "Tiap player punya death list masing-masing."
                : "Semua player berbagi satu death list bersama.";
        int dur = gm.getRoundDurationSeconds();

        Bukkit.broadcast(Component.text(""));
        Bukkit.broadcast(Component.text("  ☠ DEATH RACE DIMULAI! ☠", NamedTextColor.RED, TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("  Mode: " + modeName, NamedTextColor.GOLD));
        Bukkit.broadcast(Component.text("  " + modeDesc, NamedTextColor.YELLOW));
        Bukkit.broadcast(Component.text(String.format("  ⏱ Durasi ronde: %d:%02d", dur / 60, dur % 60), NamedTextColor.AQUA));
        Bukkit.broadcast(Component.text("  Player terdaftar: " + gm.getRegisteredPlayers().size(), NamedTextColor.AQUA));
        Bukkit.broadcast(Component.text("  🌍 Border: " + (int) borderSize + "Blocks", NamedTextColor.AQUA));
        Bukkit.broadcast(Component.text("  ⚔ PVP: OFF  |  Difficulty: HARD", NamedTextColor.YELLOW));
        Bukkit.broadcast(Component.text("  Mati dengan cara yang berbeda untuk mendapatkan poin!", NamedTextColor.WHITE));
        Bukkit.broadcast(Component.text(""));
        return true;
    }

    // ── /stopgame ─────────────────────────────────────────────────────────────

    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("deathrace.admin")) {
            sender.sendMessage(Component.text("❌ Kamu tidak punya permission!", NamedTextColor.RED));
            return true;
        }
        if (!gm.isRunning()) {
            sender.sendMessage(Component.text("⚠ Game tidak sedang berjalan.", NamedTextColor.YELLOW));
            return true;
        }

        Bukkit.broadcast(Component.text(""));
        Bukkit.broadcast(Component.text("  ☠ DEATH RACE SELESAI! ☠", NamedTextColor.RED, TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("  ═══ HASIL AKHIR ═══", NamedTextColor.GOLD));

        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(gm.getScores().entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        int rank = 1;
        String[] medals = {"🥇", "🥈", "🥉"};
        for (Map.Entry<UUID, Integer> entry : sorted) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = "???";
            String medal = rank <= 3 ? medals[rank - 1] : "  " + rank + ".";
            Bukkit.broadcast(Component.text(
                "  " + medal + " " + name + " — " + entry.getValue() + " poin",
                rank == 1 ? NamedTextColor.GOLD : rank == 2 ? NamedTextColor.GRAY : NamedTextColor.WHITE
            ));
            rank++;
        }
        Bukkit.broadcast(Component.text(""));
        Bukkit.broadcast(Component.text("  Gunakan /startsolo atau /startglobal untuk ronde baru!", NamedTextColor.AQUA));

        gm.stopGame();
        return true;
    }

    // ── /listdeath ────────────────────────────────────────────────────────────

    private boolean handleListDeath(CommandSender sender) {
        if (!gm.isRunning()) {
            sender.sendMessage(Component.text("⚠ Game belum berjalan.", NamedTextColor.YELLOW));
            return true;
        }
        if (gm.getMode() == GameManager.GameMode.GLOBAL) {
            Set<String> used = gm.getGlobalUsedDeaths();
            if (used.isEmpty()) {
                sender.sendMessage(Component.text("💀 Belum pernah mati di ronde ini.", NamedTextColor.YELLOW));
                return true;
            }
            sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("  💀 DEATH LIST (GLOBAL)", NamedTextColor.RED, TextDecoration.BOLD));
            sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
            for (String key : used) {
                sender.sendMessage(Component.text("  ❌ " + DeathKeys.getDisplay(key), NamedTextColor.RED));
            }
            sender.sendMessage(Component.text("  Total terpakai: " + used.size() + "/" + DeathKeys.DEATH_DISPLAY.size(), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
        } else {
            if (sender instanceof Player player && gm.isRegistered(player)) {
                Set<String> used = gm.getUsedDeaths(player);
                if (used.isEmpty()) {
                    sender.sendMessage(Component.text("💀 Belum pernah mati di ronde ini.", NamedTextColor.YELLOW));
                    return true;
                }
                sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
                sender.sendMessage(Component.text("  💀 DEATH LIST (SOLO)", NamedTextColor.RED, TextDecoration.BOLD));
                sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
                for (String key : used) {
                    sender.sendMessage(Component.text("  ❌ " + DeathKeys.getDisplay(key), NamedTextColor.RED));
                }
                sender.sendMessage(Component.text("  Total terpakai: " + used.size() + "/" + DeathKeys.DEATH_DISPLAY.size(), NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
            } else if (sender.hasPermission("deathrace.admin")) {
                sender.sendMessage(Component.text("═══ DEATH LIST - SEMUA PLAYER (SOLO) ═══", NamedTextColor.GOLD));
                for (UUID uuid : gm.getRegisteredPlayers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();
                    if (name == null) name = uuid.toString().substring(0, 8);
                    Set<String> used = gm.getUsedDeaths(p != null ? p : null, uuid);
                    sender.sendMessage(Component.text(name + " (" + used.size() + " poin): " + formatUsedKeys(used), NamedTextColor.YELLOW));
                }
            } else {
                sender.sendMessage(Component.text("⚠ Kamu tidak terdaftar di game.", NamedTextColor.YELLOW));
            }
        }
        return true;
    }

    // ── /listdeathall ─────────────────────────────────────────────────────────

    private boolean handleListDeathAll(CommandSender sender) {
        if (!sender.hasPermission("deathrace.admin")) {
            sender.sendMessage(Component.text("❌ Kamu tidak punya permission!", NamedTextColor.RED));
            return true;
        }
        if (!gm.isRunning()) {
            sender.sendMessage(Component.text("⚠ Game belum berjalan.", NamedTextColor.YELLOW));
            return true;
        }

        boolean isGlobal = gm.getMode() == GameManager.GameMode.GLOBAL;

        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text(
            "  💀 SEMUA DEATH LIST — " + (isGlobal ? "GLOBAL" : "SOLO"),
            NamedTextColor.GOLD, TextDecoration.BOLD
        ));
        sender.sendMessage(Component.text("  (✅ = sudah dipakai  |  ❌ = belum dipakai)"));
        sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));

        List<String> allKeys = new ArrayList<>(DeathKeys.DEATH_DISPLAY.keySet());

        if (isGlobal) {
            Set<String> usedGlobal = gm.getGlobalUsedDeaths();
            allKeys.sort((a, b) -> {
                boolean aUsed = usedGlobal.contains(a);
                boolean bUsed = usedGlobal.contains(b);
                if (aUsed && !bUsed) return -1;
                if (!aUsed && bUsed) return 1;
                return a.compareTo(b);
            });

            int usedCount = 0;
            for (String key : allKeys) {
                boolean used = usedGlobal.contains(key);
                if (used) usedCount++;

                UUID killerUUID = gm.getGlobalDeathKiller(key);
                String killerName = killerUUID != null
                        ? Bukkit.getOfflinePlayer(killerUUID).getName()
                        : null;

                sender.sendMessage(buildGlobalDeathLine(key, used, killerName));
            }

            sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
            sender.sendMessage(Component.text(
                "  Total: " + usedCount + " / " + allKeys.size() + " death",
                NamedTextColor.YELLOW
            ));

        } else {
            allKeys.sort((a, b) -> {
                int ca = gm.getSoloDeathCount(a);
                int cb = gm.getSoloDeathCount(b);
                if (ca != cb) return cb - ca;
                return a.compareTo(b);
            });

            int totalUsed = 0;
            for (String key : allKeys) {
                List<UUID> killers = gm.getSoloDeathKillers(key);
                if (!killers.isEmpty()) totalUsed++;
                sender.sendMessage(buildSoloDeathLine(key, killers));
            }

            sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
            sender.sendMessage(Component.text(
                "  Total: " + totalUsed + " / " + allKeys.size() + " death",
                NamedTextColor.YELLOW
            ));
        }

        sender.sendMessage(Component.text(""));
        return true;
    }

    // ── /listdeathall helpers ─────────────────────────────────────────────────

    private Component buildGlobalDeathLine(String key, boolean used, String killerName) {
        String display = DeathKeys.getDisplay(key);

        Component prefix = used
                ? Component.text("  ✅ ", NamedTextColor.GREEN)
                : Component.text("  ❌ ", NamedTextColor.RED);

        Component deathLabel = Component.text(display,
                used ? NamedTextColor.GREEN : NamedTextColor.RED);

        if (!used || killerName == null) {
            return Component.text().append(prefix).append(deathLabel).build();
        }

        Player killer = Bukkit.getPlayerExact(killerName);
        UUID uuid = (killer != null) ? killer.getUniqueId() : Bukkit.getOfflinePlayer(killerName).getUniqueId();

        // Pakai buildSkullJson supaya texture ke-load, hover = nama player
        Component skullIcon = buildSkullJson(uuid, killerName);

        Component killerComp = Component.text()
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(skullIcon)
                .append(Component.text(" " + killerName, NamedTextColor.YELLOW))
                .build();

        return Component.text().append(prefix).append(deathLabel).append(killerComp).build();
    }

    private Component buildSoloDeathLine(String key, List<UUID> killers) {
        String display = DeathKeys.getDisplay(key);
        boolean used = !killers.isEmpty();

        Component prefix = used
                ? Component.text("  ✅ ", NamedTextColor.GREEN)
                : Component.text("  ❌ ", NamedTextColor.RED);

        Component deathLabel = Component.text(display,
                used ? NamedTextColor.GREEN : NamedTextColor.RED);

        if (!used) {
            return Component.text().append(prefix).append(deathLabel).build();
        }

        Component count = Component.text(" (" + killers.size() + "x) ", NamedTextColor.AQUA)
            .append(Component.text("| ", NamedTextColor.GRAY));

        // Kepala player — hover = nama player
        TextComponent.Builder skulls = Component.text();
        for (UUID uuid : killers) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null) name = "???";

            Component skullIcon = buildSkullJson(uuid, name)
                    .hoverEvent(HoverEvent.showText(
                            Component.text(name, NamedTextColor.YELLOW)
                    ));

            skulls.append(skullIcon);
        }

        return Component.text()
                .append(prefix)
                .append(deathLabel)
                .append(count)
                .append(skulls.build())
                .build();
    }

    /**
     * Build skull component via raw JSON — skin player ke-render langsung di chat.
     */
    private Component buildSkullJson(UUID uuid, String name) {
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        int[] id = {(int)(most >> 32), (int)most, (int)(least >> 32), (int)least};

        String textureValue = getTextureValue(uuid);

        String json;
        if (!textureValue.isEmpty()) {
            json = String.format(
                "{\"player\":{\"id\":[%d,%d,%d,%d],\"name\":\"%s\",\"properties\":[{\"name\":\"textures\",\"value\":\"%s\"}],\"hat\":true}}",
                id[0], id[1], id[2], id[3], name, textureValue
            );
        } else {
            json = String.format(
                "{\"player\":{\"id\":[%d,%d,%d,%d],\"name\":\"%s\",\"hat\":true}}",
                id[0], id[1], id[2], id[3], name
            );
        }

        try {
            return GsonComponentSerializer.gson().deserialize(json);
        } catch (Exception e) {
            return Component.text("👤", NamedTextColor.GRAY);
        }
    }

    /**
     * Ambil texture value dari profile cache.
     * Online player ambil langsung, offline dari cache server.
     */
    private String getTextureValue(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            for (var prop : online.getPlayerProfile().getProperties()) {
                if (prop.getName().equals("textures")) return prop.getValue();
            }
        }
        try {
            for (var prop : Bukkit.getOfflinePlayer(uuid).getPlayerProfile().getProperties()) {
                if (prop.getName().equals("textures")) return prop.getValue();
            }
        } catch (Exception ignored) {}
        return "";
    }

    // ── /settimer ─────────────────────────────────────────────────────────────

    private boolean handleSetTimer(CommandSender sender, String[] args) {
        if (!sender.hasPermission("deathrace.admin")) {
            sender.sendMessage(Component.text("❌ Kamu tidak punya permission!", NamedTextColor.RED));
            return true;
        }
        if (gm.isRunning()) {
            sender.sendMessage(Component.text("❌ Tidak bisa set timer saat game sedang berjalan!", NamedTextColor.RED));
            return true;
        }
        if (args.length < 1) {
            int cur = gm.getRoundDurationSeconds();
            sender.sendMessage(Component.text(String.format("⏱ Timer sekarang: %d:%02d. Usage: /settimer <detik> atau /settimer <angka>m", cur / 60, cur % 60), NamedTextColor.YELLOW));
            return true;
        }
        String input = args[0].toLowerCase();
        int seconds;
        try {
            if (input.endsWith("m")) {
                seconds = Integer.parseInt(input.replace("m", "")) * 60;
            } else {
                seconds = Integer.parseInt(input);
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("❌ Format salah! Contoh: /settimer 300 atau /settimer 5m", NamedTextColor.RED));
            return true;
        }
        if (seconds < 30) {
            sender.sendMessage(Component.text("❌ Timer minimal 30 detik!", NamedTextColor.RED));
            return true;
        }
        if (seconds > 3600) {
            sender.sendMessage(Component.text("❌ Timer maksimal 60 menit!", NamedTextColor.RED));
            return true;
        }
        gm.setRoundDuration(seconds);
        sender.sendMessage(Component.text(String.format("✅ Timer ronde diset ke %d:%02d!", seconds / 60, seconds % 60), NamedTextColor.GREEN));
        return true;
    }

    // ── /maxhealth ────────────────────────────────────────────────────────────

    private boolean handleMaxHealth(CommandSender sender, String[] args) {
        if (!sender.hasPermission("deathrace.admin")) {
            sender.sendMessage(Component.text("❌ Kamu tidak punya permission!", NamedTextColor.RED));
            return true;
        }

        // /maxhealth → lihat max health semua registered player
        if (args.length == 0) {
            if (gm.getRegisteredPlayers().isEmpty()) {
                sender.sendMessage(Component.text("⚠ Belum ada player yang terdaftar.", NamedTextColor.YELLOW));
                return true;
            }
            sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("  ❤ MAX HEALTH PLAYER", NamedTextColor.RED, TextDecoration.BOLD));
            sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
            for (UUID uuid : gm.getRegisteredPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();
                if (name == null) name = uuid.toString().substring(0, 8);
                if (p != null) {
                    double max = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue();
                    sender.sendMessage(Component.text(
                        "  " + name + " — " + (max / 2) + " ❤ (" + max + " HP)",
                        NamedTextColor.WHITE
                    ));
                } else {
                    sender.sendMessage(Component.text("  " + name + " — offline", NamedTextColor.GRAY));
                }
            }
            sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
            return true;
        }

        // /maxhealth <hp> → set semua registered player
        // /maxhealth <player> <hp> → set player spesifik
        if (args.length == 1) {
            double hp;
            try {
                hp = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("❌ Usage: /maxhealth <player> <hp> or /maxhealth <hp>", NamedTextColor.RED));
                return true;
            }
            if (!validateHp(sender, hp)) return true;

            int count = 0;
            for (UUID uuid : gm.getRegisteredPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) continue;
                setMaxHealth(p, hp);
                count++;
            }
            sender.sendMessage(Component.text(
                "✅ Max health semua player (" + count + ") diset ke " + (hp / 2) + " ❤ (" + hp + " HP)",
                NamedTextColor.GREEN
            ));
            return true;
        }

        // /maxhealth <player> <hp>
        if (args.length >= 2) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("❌ Player " + args[0] + " tidak ditemukan / offline!", NamedTextColor.RED));
                return true;
            }
            double hp;
            try {
                hp = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("❌ Usage: /maxhealth <player> <hp> or /maxhealth <hp>", NamedTextColor.RED));
                return true;
            }
            if (!validateHp(sender, hp)) return true;

            setMaxHealth(target, hp);
            sender.sendMessage(Component.text(
                "✅ Max health " + target.getName() + " diset ke " + (hp / 2) + " ❤ (" + hp + " HP)",
                NamedTextColor.GREEN
            ));
            target.sendMessage(Component.text(
                "⚠ Max health kamu diset ke " + (hp / 2) + " ❤ oleh admin.",
                NamedTextColor.RED
            ));
            return true;
        }

        return true;
    }

    private boolean validateHp(CommandSender sender, double hp) {
        if (hp < 2) {
            sender.sendMessage(Component.text("❌ Minimal 2 HP (1 ❤)!", NamedTextColor.RED));
            return false;
        }
        if (hp > 2048) {
            sender.sendMessage(Component.text("❌ Maksimal 2048 HP (1024 ❤)!", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    private void setMaxHealth(Player player, double hp) {
        var attr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        attr.setBaseValue(hp);
        // Kalau health sekarang melebihi max baru, clamp ke max
        if (player.getHealth() > hp) player.setHealth(hp);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String formatUsedKeys(Set<String> used) {
        if (used.isEmpty()) return "(belum ada)";
        StringBuilder sb = new StringBuilder();
        for (String key : used) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(DeathKeys.getDisplay(key));
        }
        return sb.toString();
    }
}