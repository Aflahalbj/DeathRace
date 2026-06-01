package id.deathrace;

import id.deathrace.commands.CommandHandler;
import id.deathrace.listeners.DeathListener;
import id.deathrace.listeners.ExtraListener;
import id.deathrace.managers.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class DeathRace extends JavaPlugin {

    private GameManager gameManager;
    private DeathListener deathListener;
    private ExtraListener extraListener;

    @Override
    public void onEnable() {
        gameManager = new GameManager(this);

        deathListener = new DeathListener(this, gameManager);
        extraListener = new ExtraListener(this, gameManager, deathListener);

        getServer().getPluginManager().registerEvents(deathListener, this);
        getServer().getPluginManager().registerEvents(extraListener, this);

        CommandHandler cmdHandler = new CommandHandler(gameManager, deathListener, extraListener);
        getCommand("regis").setExecutor(cmdHandler);
        getCommand("unregis").setExecutor(cmdHandler);
        getCommand("listplayer").setExecutor(cmdHandler);
        getCommand("startsolo").setExecutor(cmdHandler);
        getCommand("startglobal").setExecutor(cmdHandler);
        getCommand("listdeath").setExecutor(cmdHandler);
        getCommand("listdeathall").setExecutor(cmdHandler);
        getCommand("stopgame").setExecutor(cmdHandler);
        getCommand("settimer").setExecutor(cmdHandler);
        getCommand("maxhealth").setExecutor(cmdHandler);

        // Hunger tick: check every second (20 ticks) for low hunger players
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameManager.isRunning()) return;
                for (var uuid : gameManager.getRegisteredPlayers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        deathListener.checkHunger(p);
                    }
                }
            }
        }.runTaskTimer(this, 20L, 20L);

        getLogger().info("DeathRace plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("DeathRace plugin disabled!");
    }

    public GameManager getGameManager() { return gameManager; }
    public DeathListener getDeathListener() { return deathListener; }
    public ExtraListener getExtraListener() { return extraListener; }
}