package id.deathrace.listeners;

import id.deathrace.DeathRace;
import id.deathrace.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class ExtraListener implements Listener {

    private final DeathRace plugin;
    private final GameManager gm;
    private final DeathListener deathListener;

    public ExtraListener(DeathRace plugin, GameManager gm, DeathListener deathListener) {
        this.plugin = plugin;
        this.gm = gm;
        this.deathListener = deathListener;
    }

    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event) {
        if (!gm.isRunning()) return;
        Block block = event.getBlock();
        if (!(block.getState() instanceof Dispenser)) return;

        Material item = event.getItem().getType();
        boolean isDangerous = item == Material.ARROW || item == Material.SPECTRAL_ARROW
                || item == Material.TIPPED_ARROW || item == Material.FIRE_CHARGE || item == Material.FIREWORK_ROCKET;
        if (!isDangerous) return;

        for (Entity nearby : block.getWorld().getNearbyEntities(block.getLocation(), 8, 8, 8)) {
            if (nearby instanceof Player player && gm.isRegistered(player) && gm.isDeathUsed(player, "DISPENSER")) {
                event.setCancelled(true); 
                block.getWorld().playSound(block.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1.0f, 1.2f);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDispenserProjectileDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!gm.isRunning() || !gm.isRegistered(player)) return;

        Entity damager = event.getDamager();
        if (damager instanceof Arrow || damager instanceof Fireball) {
            if (gm.isDeathUsed(player, "DISPENSER")) {
                event.setCancelled(true);
                damager.remove(); 
            } else {
                player.setMetadata("LOMBA_DISPENSER_KILL", new FixedMetadataValue(plugin, true));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onExplosionPrime(ExplosionPrimeEvent event) {

        if (!gm.isRunning()) return;

        Entity entity = event.getEntity();

        // CREEPER
        if (entity instanceof Creeper creeper) {

            for (Entity nearby : creeper.getNearbyEntities(6, 6, 6)) {

                if (nearby instanceof Player player
                        && gm.isRegistered(player)
                        && gm.isDeathUsed(player, "CREEPER")) {

                    event.setCancelled(true);

                    removeCreeperWithFart(creeper);

                    return;
                }
            }
        }

        // TNT
        if (entity instanceof TNTPrimed tnt) {

            for (Entity nearby : tnt.getNearbyEntities(8, 8, 8)) {

                if (nearby instanceof Player player
                        && gm.isRegistered(player)
                        && gm.isDeathUsed(player, "TNT")) {

                    event.setCancelled(true);

                    removeTNTWithFart(tnt);

                    return;
                }
            }
        }
    }

    @EventHandler
    public void onTNTPrime(EntitySpawnEvent event) {
        if (!gm.isRunning()) return;
        if (!(event.getEntity() instanceof TNTPrimed tnt)) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!tnt.isValid()) return;
                for (Entity nearby : tnt.getNearbyEntities(8, 8, 8)) {
                    if (nearby instanceof Player player && gm.isRegistered(player) && gm.isDeathUsed(player, "TNT")) {
                        removeTNTWithFart(tnt);
                        return;
                    }
                }
            }
        }.runTaskLater(plugin, 2L);
    }

    private void removeTNTWithFart(TNTPrimed tnt) {
        Location loc = tnt.getLocation();
        tnt.remove();
        loc.getWorld().spawnParticle(Particle.POOF, loc, 15, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.BLOCK_SLIME_BLOCK_BREAK, 1.0f, 0.5f);
    }

    private void removeCreeperWithFart(Creeper creeper) {
        Location loc = creeper.getLocation();
        creeper.remove();
        loc.getWorld().spawnParticle(Particle.POOF, loc, 15, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.BLOCK_HONEY_BLOCK_BREAK, 1.0f, 0.5f);
    }

    public void cleanupPlayer(UUID uuid) {
        if (this.deathListener != null) {
            this.deathListener.cleanupPlayer(uuid);
        }
    }
}