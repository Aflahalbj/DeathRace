package id.deathrace.commands;

import id.deathrace.DeathRace;
import id.deathrace.listeners.DeathListener;
import id.deathrace.listeners.ExtraListener;
import id.deathrace.managers.GameManager;
import id.deathrace.utils.DeathKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
            case "regis" -> handleRegis(sender, args);
            case "unregis" -> handleUnregis(sender, args);
            case "listplayer" -> handleListPlayer(sender);
            case "startsolo" -> handleStart(sender, GameManager.GameMode.SOLO);
            case "startglobal" -> handleStart(sender, GameManager.GameMode.GLOBAL);
            case "listdeath" -> handleListDeath(sender);
            case "stopgame" -> handleStop(sender);
            case "settimer" -> handleSetTimer(sender, args);
            default -> false;
        };
    }

    // /regis <playername>
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
            target.sendMessage(Component.text("✅ Kamu telah didaftarkan ke Lomba Mati!", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("⚠ " + target.getName() + " sudah terdaftar!", NamedTextColor.YELLOW));
        }
        return true;
    }

    // /unregis <playername>
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
                target.sendMessage(Component.text("⚠ Kamu telah dikeluarkan dari Lomba Mati.", NamedTextColor.YELLOW));
            } else {
                sender.sendMessage(Component.text("⚠ " + target.getName() + " tidak terdaftar!", NamedTextColor.YELLOW));
            }
        } else {
            UUID found = null;
            for (UUID uuid : gm.getRegisteredPlayers()) {
                String offlineName = Bukkit.getOfflinePlayer(uuid).getName();
                if (args[0].equalsIgnoreCase(offlineName)) {
                    found = uuid;
                    break;
                }
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

    // /listplayer
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
        sender.sendMessage(Component.text("  📋 DAFTAR PLAYER LOMBA MATI", NamedTextColor.GOLD, TextDecoration.BOLD));
        String modeStr = gm.isRunning() ? (gm.getMode() == GameManager.GameMode.SOLO ? "SOLO" : "GLOBAL") : "WAITING";
        sender.sendMessage(Component.text("  Mode: " + modeStr, NamedTextColor.AQUA));
        if (gm.isRunning()) {
            int remaining = gm.getTimeRemainingSeconds();
            int m = remaining / 60, s = remaining % 60;
            sender.sendMessage(Component.text(String.format("  ⏱ Sisa waktu: %d:%02d", m, s), NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));

        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(gm.getScores().entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : sorted) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = entry.getKey().toString().substring(0, 8);
            boolean online = Bukkit.getPlayer(entry.getKey()) != null;
            String status = online ? "🟢" : "🔴";
            sender.sendMessage(Component.text(
                "  " + rank + ". " + status + " " + name + " — Score: " + entry.getValue(),
                rank == 1 ? NamedTextColor.GOLD : (rank == 2 ? NamedTextColor.GRAY : NamedTextColor.WHITE)
            ));
            rank++;
        }
        sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
        return true;
    }

    // /startsolo or /startglobal
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

        gm.startGame(mode);
        String modeName = mode == GameManager.GameMode.SOLO ? "SOLO" : "GLOBAL";
        String modeDesc = mode == GameManager.GameMode.SOLO
                ? "Tiap player punya death list masing-masing."
                : "Semua player berbagi satu death list bersama.";

        int dur = gm.getRoundDurationSeconds();
        int durM = dur / 60, durS = dur % 60;

        Bukkit.broadcast(Component.text(""));
        Bukkit.broadcast(Component.text("  ☠ LOMBA MATI DIMULAI! ☠", NamedTextColor.RED, TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("  Mode: " + modeName, NamedTextColor.GOLD));
        Bukkit.broadcast(Component.text("  " + modeDesc, NamedTextColor.YELLOW));
        Bukkit.broadcast(Component.text(String.format("  ⏱ Durasi ronde: %d:%02d", durM, durS), NamedTextColor.AQUA));
        Bukkit.broadcast(Component.text("  Player terdaftar: " + gm.getRegisteredPlayers().size(), NamedTextColor.AQUA));
        Bukkit.broadcast(Component.text("  Mati dengan cara berbeda-beda untuk dapat poin!", NamedTextColor.WHITE));
        Bukkit.broadcast(Component.text(""));
        return true;
    }

    // /stopgame
    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("deathrace.admin")) {
            sender.sendMessage(Component.text("❌ Kamu tidak punya permission!", NamedTextColor.RED));
            return true;
        }

        if (!gm.isRunning()) {
            sender.sendMessage(Component.text("⚠ Game tidak sedang berjalan.", NamedTextColor.YELLOW));
            return true;
        }

        // Announce final scores
        Bukkit.broadcast(Component.text(""));
        Bukkit.broadcast(Component.text("  ☠ LOMBA MATI SELESAI! ☠", NamedTextColor.RED, TextDecoration.BOLD));
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
                rank == 1 ? NamedTextColor.GOLD : (rank == 2 ? NamedTextColor.GRAY : NamedTextColor.WHITE)
            ));
            rank++;
        }
        Bukkit.broadcast(Component.text(""));
        Bukkit.broadcast(Component.text("  Gunakan /startsolo atau /startglobal untuk ronde baru!", NamedTextColor.AQUA));

        gm.stopGame();
        return true;
    }

    // /listdeath — bisa dipakai semua player, tapi cuma tampil yang SUDAH dipakai
    private boolean handleListDeath(CommandSender sender) {
        if (!gm.isRunning()) {
            sender.sendMessage(Component.text("⚠ Game belum berjalan.", NamedTextColor.YELLOW));
            return true;
        }

        if (gm.getMode() == GameManager.GameMode.GLOBAL) {
            Set<String> used = gm.getGlobalUsedDeaths();
            if (used.isEmpty()) {
                sender.sendMessage(Component.text("💀 Belum ada death yang dipakai di ronde ini.", NamedTextColor.YELLOW));
                return true;
            }
            sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("  💀 DEATH SUDAH DIPAKAI (GLOBAL)", NamedTextColor.RED, TextDecoration.BOLD));
            sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
            for (String key : used) {
                sender.sendMessage(Component.text(
                    "  ❌ " + DeathKeys.getDisplay(key),
                    NamedTextColor.RED
                ));
            }
            sender.sendMessage(Component.text("  Total terpakai: " + used.size() + "/" + DeathKeys.DEATH_DISPLAY.size(), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));

        } else {
            // SOLO mode
            if (sender instanceof Player player && gm.isRegistered(player)) {
                Set<String> used = gm.getUsedDeaths(player);
                if (used.isEmpty()) {
                    sender.sendMessage(Component.text("💀 Kamu belum pake death apapun di ronde ini.", NamedTextColor.YELLOW));
                    return true;
                }
                sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
                sender.sendMessage(Component.text("  💀 DEATH KAMU YANG SUDAH DIPAKAI", NamedTextColor.RED, TextDecoration.BOLD));
                sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));
                for (String key : used) {
                    sender.sendMessage(Component.text(
                        "  ❌ " + DeathKeys.getDisplay(key),
                        NamedTextColor.RED
                    ));
                }
                sender.sendMessage(Component.text("  Total terpakai: " + used.size() + "/" + DeathKeys.DEATH_DISPLAY.size(), NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("═══════════════════════", NamedTextColor.GOLD));

            } else if (sender.hasPermission("deathrace.admin")) {
                // Admin console — show all players' used deaths
                sender.sendMessage(Component.text("═══ DEATH SUDAH DIPAKAI - SEMUA PLAYER (SOLO) ═══", NamedTextColor.GOLD));
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

    // /settimer <detik> atau /settimer <menit>m
    private boolean handleSetTimer(CommandSender sender, String[] args) {
        if (!sender.hasPermission("deathrace.admin")) {
            sender.sendMessage(Component.text("❌ Kamu tidak punya permission!", NamedTextColor.RED));
            return true;
        }

        if (gm.isRunning()) {
            sender.sendMessage(Component.text("❌ Gabisa set timer saat game sedang berjalan!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            int cur = gm.getRoundDurationSeconds();
            sender.sendMessage(Component.text(String.format("⏱ Timer sekarang: %d:%02d. Usage: /settimer <detik> atau /settimer <angka>m (menit)", cur / 60, cur % 60), NamedTextColor.YELLOW));
            return true;
        }

        String input = args[0].toLowerCase();
        int seconds;
        try {
            if (input.endsWith("m")) {
                int minutes = Integer.parseInt(input.replace("m", ""));
                seconds = minutes * 60;
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
        int m = seconds / 60, s = seconds % 60;
        sender.sendMessage(Component.text(String.format("✅ Timer ronde diset ke %d:%02d!", m, s), NamedTextColor.GREEN));
        return true;
    }

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