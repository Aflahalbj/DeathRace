package id.deathrace.managers;

import id.deathrace.DeathRace;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
// import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GameManager {

    public enum GameMode { SOLO, GLOBAL }
    public enum GameState { WAITING, RUNNING }

    private final DeathRace plugin;
    private GameState state = GameState.WAITING;
    private GameMode mode = GameMode.SOLO;

    // Registered players
    private final Set<UUID> registeredPlayers = new LinkedHashSet<>();
    // Score per player
    private final Map<UUID, Integer> scores = new HashMap<>();
    // SOLO: death causes used per player
    private final Map<UUID, Set<String>> soloUsedDeaths = new HashMap<>();
    // GLOBAL: death causes used by anyone
    private final Set<String> globalUsedDeaths = new HashSet<>();

    // SOLO: Map<deathKey, List<UUID>> — siapa aja yang udah mati dari death itu
    private final Map<String, List<UUID>> soloDeathKillers = new HashMap<>();
    // GLOBAL: Map<deathKey, UUID> — siapa yang pertama mati dari death itu
    private final Map<String, UUID> globalDeathKiller = new HashMap<>();

    // Border info (diset saat /start)
    private Location borderCenter = null;
    private double borderSize = 10000;

    // Timer
    private int roundDurationSeconds = 300;
    private int timeRemainingSeconds = 0;
    private BukkitRunnable timerTask = null;
    private BossBar bossBar = null;

    public GameManager(DeathRace plugin) {
        this.plugin = plugin;
    }

    public boolean registerPlayer(Player player) {
        if (registeredPlayers.contains(player.getUniqueId())) return false;
        registeredPlayers.add(player.getUniqueId());
        scores.put(player.getUniqueId(), 0);
        soloUsedDeaths.put(player.getUniqueId(), new HashSet<>());
        return true;
    }

    public boolean unregisterPlayer(Player player) {
        if (!registeredPlayers.contains(player.getUniqueId())) return false;
        registeredPlayers.remove(player.getUniqueId());
        scores.remove(player.getUniqueId());
        soloUsedDeaths.remove(player.getUniqueId());
        if (bossBar != null) bossBar.removeViewer(player);
        return true;
    }

    public void startGame(GameMode gameMode, Location center, double size) {
        this.mode = gameMode;
        this.state = GameState.RUNNING;
        this.borderCenter = center;
        this.borderSize = size;

        soloUsedDeaths.clear();
        globalUsedDeaths.clear();
        soloDeathKillers.clear();
        globalDeathKiller.clear();

        for (UUID uuid : registeredPlayers) {
            scores.put(uuid, 0);
            soloUsedDeaths.put(uuid, new HashSet<>());
        }
        startTimer();
    }

    public void stopGame() {
        this.state = GameState.WAITING;
        globalUsedDeaths.clear();
        soloUsedDeaths.clear();
        soloDeathKillers.clear();
        globalDeathKiller.clear();
        stopTimer();
    }

    public void setRoundDuration(int seconds) { this.roundDurationSeconds = seconds; }
    public int getRoundDurationSeconds() { return roundDurationSeconds; }
    public Location getBorderCenter() { return borderCenter; }
    public double getBorderSize() { return borderSize; }

    private void startTimer() {
        timeRemainingSeconds = roundDurationSeconds;

        bossBar = BossBar.bossBar(
                buildBossBarTitle(), 1.0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS
        );

        for (UUID uuid : registeredPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.showBossBar(bossBar);
        }

        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning()) { cancel(); return; }

                timeRemainingSeconds--;

                if (timeRemainingSeconds <= 0) {
                    cancel();
                    hideBossBar();
                    announceTimeUp();
                    return;
                }

                float progress = (float) timeRemainingSeconds / roundDurationSeconds;
                bossBar.progress(Math.max(0f, Math.min(1f, progress)));

                if (progress > 0.5f) bossBar.color(BossBar.Color.GREEN);
                else if (progress > 0.25f) bossBar.color(BossBar.Color.YELLOW);
                else bossBar.color(BossBar.Color.RED);

                bossBar.name(buildBossBarTitle());
            }
        };
        timerTask.runTaskTimer(plugin, 20L, 20L);
    }

    private Component buildBossBarTitle() {
        int minutes = timeRemainingSeconds / 60;
        int seconds = timeRemainingSeconds % 60;
        String timeStr = String.format("%d:%02d", minutes, seconds);

        float progress = roundDurationSeconds > 0
                ? (float) timeRemainingSeconds / roundDurationSeconds : 1f;

        NamedTextColor color;
        if (progress > 0.5f) color = NamedTextColor.GREEN;
        else if (progress > 0.25f) color = NamedTextColor.YELLOW;
        else color = NamedTextColor.RED;

        return Component.text()
                .append(Component.text("☠ DEATH RACE ", NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text("| ", NamedTextColor.DARK_GRAY))
                .append(Component.text("⏱ " + timeStr, color, TextDecoration.BOLD))
                .build();
    }

    private void stopTimer() {
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }
        hideBossBar();
        timeRemainingSeconds = 0;
    }

    private void hideBossBar() {
        if (bossBar != null) {
            for (UUID uuid : registeredPlayers) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.hideBossBar(bossBar);
            }
            bossBar = null;
        }
    }

    public void showBossBarToPlayer(Player player) {
        if (bossBar != null && isRunning()) player.showBossBar(bossBar);
    }

    private void announceTimeUp() {
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        Bukkit.broadcast(Component.text(""));
        Bukkit.broadcast(Component.text("  ⏰ WAKTU HABIS! RONDE SELESAI!", NamedTextColor.RED, TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("  ══════ RANKING ══════", NamedTextColor.GOLD));

        String[] medals = {"🥇", "🥈", "🥉"};
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : sorted) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = "???";
            String medal = rank <= 3 ? medals[rank - 1] : "  " + rank + ".";
            NamedTextColor rankColor = rank == 1 ? NamedTextColor.GOLD
                    : rank == 2 ? NamedTextColor.GRAY
                    : rank == 3 ? NamedTextColor.GOLD
                    : NamedTextColor.WHITE;
            Bukkit.broadcast(Component.text(
                    "  " + medal + " " + name + " — " + entry.getValue() + " poin", rankColor
            ));
            rank++;
        }
        Bukkit.broadcast(Component.text("  ═══════════════════════", NamedTextColor.GOLD));
        Bukkit.broadcast(Component.text("  Gunakan /startsolo atau /startglobal untuk ronde baru!", NamedTextColor.AQUA));
        Bukkit.broadcast(Component.text(""));

        for (UUID uuid : registeredPlayers) scores.put(uuid, 0);
        this.state = GameState.WAITING;
        globalUsedDeaths.clear();
        soloUsedDeaths.clear();
        soloDeathKillers.clear();
        globalDeathKiller.clear();
    }

    public boolean isRunning() { return state == GameState.RUNNING; }
    public boolean isRegistered(Player player) { return registeredPlayers.contains(player.getUniqueId()); }
    public GameMode getMode() { return mode; }
    public GameState getState() { return state; }
    public Set<UUID> getRegisteredPlayers() { return Collections.unmodifiableSet(registeredPlayers); }
    public Map<UUID, Integer> getScores() { return Collections.unmodifiableMap(scores); }
    public int getTimeRemainingSeconds() { return timeRemainingSeconds; }

    public boolean isDeathUsed(Player player, String deathKey) {
        if (mode == GameMode.GLOBAL) {
            return globalUsedDeaths.contains(deathKey);
        } else {
            Set<String> used = soloUsedDeaths.getOrDefault(player.getUniqueId(), new HashSet<>());
            return used.contains(deathKey);
        }
    }

    public void recordDeath(Player player, String deathKey) {
        if (mode == GameMode.GLOBAL) {
            globalUsedDeaths.add(deathKey);
            globalDeathKiller.putIfAbsent(deathKey, player.getUniqueId());
        } else {
            soloUsedDeaths.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(deathKey);
            soloDeathKillers.computeIfAbsent(deathKey, k -> new ArrayList<>()).add(player.getUniqueId());
        }
        scores.merge(player.getUniqueId(), 1, Integer::sum);
    }

    public int getScore(Player player) {
        return scores.getOrDefault(player.getUniqueId(), 0);
    }

    public Set<String> getUsedDeaths(Player player) {
        if (mode == GameMode.GLOBAL) {
            return Collections.unmodifiableSet(globalUsedDeaths);
        } else {
            return Collections.unmodifiableSet(soloUsedDeaths.getOrDefault(player.getUniqueId(), new HashSet<>()));
        }
    }

    public void markDeathUsed(Player player, String deathKey) {
        if (mode == GameMode.GLOBAL) {
            globalUsedDeaths.add(deathKey);
        } else {
            soloUsedDeaths.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(deathKey);
        }
    }

    public Set<String> getGlobalUsedDeaths() { return Collections.unmodifiableSet(globalUsedDeaths); }

    public void unregisterByUUID(UUID uuid) {
        registeredPlayers.remove(uuid);
        scores.remove(uuid);
        soloUsedDeaths.remove(uuid);
    }

    public Set<String> getUsedDeaths(Player player, UUID uuid) {
        if (player != null) return getUsedDeaths(player);
        if (mode == GameMode.GLOBAL) return Collections.unmodifiableSet(globalUsedDeaths);
        return Collections.unmodifiableSet(soloUsedDeaths.getOrDefault(uuid, new HashSet<>()));
    }

    // ── /listdeathall helpers ──────────────────────────────────────────────────

    /**
     * SOLO: berapa banyak player yang udah mati dari deathKey ini
     */
    public List<UUID> getSoloDeathKillers(String deathKey) {
        return Collections.unmodifiableList(soloDeathKillers.getOrDefault(deathKey, new ArrayList<>()));
    }

    /**
     * GLOBAL: siapa player pertama yang mati dari deathKey ini (null = belum ada)
     */
    public UUID getGlobalDeathKiller(String deathKey) {
        return globalDeathKiller.get(deathKey);
    }

    /**
     * SOLO: berapa player yang udah pakai death ini (untuk sorting)
     */
    public int getSoloDeathCount(String deathKey) {
        return soloDeathKillers.getOrDefault(deathKey, Collections.emptyList()).size();
    }
}