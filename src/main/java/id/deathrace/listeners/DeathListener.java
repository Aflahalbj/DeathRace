package id.deathrace.listeners;

import id.deathrace.DeathRace;
import id.deathrace.managers.GameManager;
import id.deathrace.utils.DeathKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class DeathListener implements Listener {

    private final DeathRace plugin;
    private final GameManager gm;

    private final Set<UUID> frozenBoots = new HashSet<>();
    private final Set<UUID> voidLevitation = new HashSet<>();
    private final Set<UUID> waterPushed = new HashSet<>();
    private final Set<UUID> borderBlocked = new HashSet<>();
    
    private final Set<UUID> activeChickenParachute = new HashSet<>();
    private final Set<UUID> eatingAnimation = new HashSet<>();
    private final Map<UUID, BukkitRunnable> activeLavaTasks = new HashMap<>();
    private final Set<UUID> pendingBedExplosion = new HashSet<>();
    private final Set<UUID> pendingRespawnAnchor = new HashSet<>();
    private final Set<UUID> recentCampfireBurn = new HashSet<>();
    private final Set<UUID> witchPotionVictims = new HashSet<>();

    public DeathListener(DeathRace plugin, GameManager gm) {
        this.plugin = plugin;
        this.gm = gm;
        startSpiderFearTask();
        startFearMobTask();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!gm.isRunning() || !gm.isRegistered(player)) return;

        EntityDamageEvent.DamageCause cause = player.getLastDamageCause() != null
                ? player.getLastDamageCause().getCause()
                : null;
        
        String deathKey = null;

        if (pendingBedExplosion.contains(player.getUniqueId())) {
            deathKey = "BED";
            pendingBedExplosion.remove(player.getUniqueId());

        } else if (pendingRespawnAnchor.contains(player.getUniqueId())) {
            deathKey = "RESPAWN_ANCHOR";
            pendingRespawnAnchor.remove(player.getUniqueId());

        } else if (player.hasMetadata("LOMBA_EPEARL_KILL")) {
            deathKey = "ENDER_PEARL";
            player.removeMetadata("LOMBA_EPEARL_KILL", plugin);

        } else if (cause != null) {
            deathKey = mapCauseToKey(player, cause);
        }

        // DEBUG - hapus setelah fix
        plugin.getLogger().warning("[DEBUG] " + player.getName() + " mati | cause=" + cause + " | deathKey=" + deathKey
            + " | blockFeet=" + player.getLocation().getBlock().getType()
            + " | blockBelow=" + player.getLocation().clone().subtract(0,1,0).getBlock().getType()
            + " | recentCampfire=" + recentCampfireBurn.contains(player.getUniqueId()));

        if (deathKey == null) return;

        if (!gm.isDeathUsed(player, deathKey)) {
            gm.recordDeath(player, deathKey);
            int score = gm.getScore(player);
            Bukkit.broadcast(Component.text()
                    .append(Component.text("☠ ", NamedTextColor.RED))
                    .append(Component.text(player.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text(" mati karena " + DeathKeys.getDisplay(deathKey) + "! Score: " + score, NamedTextColor.GOLD))
                    .build());

            // Aktifkan world border protection setelah mati dari border
            if (deathKey.equals("WORLD_BORDER")) {
                borderBlocked.add(player.getUniqueId());
            }
        }
    }

    private String mapCauseToKey(Player player, EntityDamageEvent.DamageCause cause) {
        if (cause == null) return null;

        Bukkit.broadcast(
                Component.text(
                        "[DEBUG] Cause = " + cause,
                        NamedTextColor.RED
                )
        );

        switch (cause) {
            case CONTACT:

                if (player.getLastDamageCause() instanceof EntityDamageByBlockEvent bdmg) {

                    Block b = bdmg.getDamager();

                    if (b != null) {

                        if (b.getType() == Material.SWEET_BERRY_BUSH) {
                            return "SWEET_BERRY";
                        }

                        if (b.getType() == Material.CACTUS) {
                            return "CACTUS";
                        }

                        // stalagmite bawah
                        if (b.getType() == Material.POINTED_DRIPSTONE) {
                            return "DRIPSTONE";
                        }
                    }
                }

                return null;

            case DROWNING:
                return "DROWNING";

            case FLY_INTO_WALL:
                return "ELYTRA";

            case BLOCK_EXPLOSION:
                if (player.getLastDamageCause() instanceof EntityDamageByBlockEvent bdmg) {
                    Block block = bdmg.getDamager();
                    if (block != null && org.bukkit.Tag.BEDS.isTagged(block.getType())) {
                        return "BED";
                    }
                    if (block != null && block.getType() == Material.RESPAWN_ANCHOR) {
                        return "RESPAWN_ANCHOR";
                    }
                }
                return "TNT";

            case ENTITY_EXPLOSION:
                if (player.getLastDamageCause() instanceof EntityDamageByEntityEvent edmg) {
                    if (edmg.getDamager() instanceof Creeper) return "CREEPER";
                    if (edmg.getDamager() instanceof Firework) return "FIREWORK";
                    if (edmg.getDamager() instanceof TNTPrimed) return "TNT";
                    if (edmg.getDamager() instanceof EnderCrystal) return "END_CRYSTAL";
                    if (edmg.getDamager() instanceof Wither || edmg.getDamager() instanceof WitherSkull) return "WITHER";
                }
                return "TNT";

            case FALL:

                Block blockBelow = player.getLocation()
                        .clone()
                        .subtract(0, 0.2, 0)
                        .getBlock();

                if (blockBelow.getType() == Material.POINTED_DRIPSTONE) {
                    return "DRIPSTONE";
                }

                return "FALL";

            case FALLING_BLOCK:
                if (player.getLastDamageCause() instanceof EntityDamageByEntityEvent edmg
                        && edmg.getDamager() instanceof FallingBlock fb) {
                    if (fb.getBlockData().getMaterial() == Material.POINTED_DRIPSTONE) return "DRIPSTONE";
                }
                return "ANVIL";

            case LAVA:
                return "LAVA";

            case CAMPFIRE:
                return "CAMPFIRE";

            case FIRE:
            case FIRE_TICK:

                // Cek apakah player berdiri di/dekat campfire (cek beberapa posisi)
                if (isOnCampfire(player) || recentCampfireBurn.contains(player.getUniqueId())) {
                    return "CAMPFIRE";
                }

                return "FIRE";

            case LIGHTNING:
                return "LIGHTNING";

            case HOT_FLOOR:

                if (isOnCampfire(player)) {
                    return "CAMPFIRE";
                }

                return "MAGMA";

            case FREEZE:
                return "FREEZE";

            case VOID:
                return "VOID";

            case WORLD_BORDER:
                return "WORLD_BORDER";

            case CUSTOM:
                // Minecraft modern kadang kirim CUSTOM untuk world border damage
                WorldBorder wb = player.getWorld().getWorldBorder();
                Location wbCenter = wb.getCenter();
                double wbHalf = wb.getSize() / 2.0;
                Location wbLoc = player.getLocation();
                boolean wbOutside = wbLoc.getX() < wbCenter.getX() - wbHalf
                        || wbLoc.getX() > wbCenter.getX() + wbHalf
                        || wbLoc.getZ() < wbCenter.getZ() - wbHalf
                        || wbLoc.getZ() > wbCenter.getZ() + wbHalf;
                if (wbOutside) return "WORLD_BORDER";
                return null;

            case STARVATION:
                return "HUNGER";

            case SUFFOCATION:
                return "SUFFOCATION";

            case THORNS:
                return "THORNS";

            case ENTITY_ATTACK:
            case PROJECTILE:
            case MAGIC:

                if (player.getLastDamageCause()
                        instanceof EntityDamageByEntityEvent edmg) {

                    Entity damager = edmg.getDamager();

                    // ARROW 
                    if (damager instanceof Arrow
                            || damager instanceof SpectralArrow) {

                        if (damager instanceof Projectile projectile
                                && projectile.getShooter() instanceof Entity shooter) {

                            Bukkit.broadcast(
                                    Component.text(
                                            "Shooter = "
                                            + shooter.getType()
                                            + " | "
                                            + shooter.getClass().getName()
                                    )
                            );

                            // mob arrows
                            switch (shooter.getType().name()) {

                                case "PARCHED":
                                    return "PARCHED";

                                case "BOGGED":
                                    return "BOGGED";

                                case "STRAY":
                                    return "STRAY";

                                case "PILLAGER":
                                    return "PILLAGER";

                                case "PIGLIN":
                                    return "PIGLIN";

                                case "SKELETON":
                                    return "SKELETON";
                            }
                        }

                        // arrow biasa/dispenser/player
                        return "ARROW";
                    }

                    // TRIDENT 
                    if (damager instanceof Trident trident) {

                        if (trident.getShooter() instanceof Drowned) {
                            return "DROWNED";
                        }

                        return "TRIDENT";
                    }

                    // FIRE CHARGE / BLAZE 
                    if (damager instanceof SmallFireball sf) {

                        if (sf.getShooter() instanceof Blaze) {
                            return "BLAZE";
                        }

                        return "FIRE_CHARGE";
                    }

                    // GHAST FIREBALL 
                    if (damager instanceof Fireball fireball) {

                        if (fireball.getShooter() instanceof Ghast) {
                            return "GHAST";
                        }
                    }

                    // BREEZE
                    if (damager instanceof WindCharge wc) {

                        if (wc.getShooter() instanceof Breeze) {
                            return "BREEZE";
                        }
                    }

                    // EVOKER FANGS
                    if (damager instanceof EvokerFangs fangs) {

                        if (fangs.getOwner() instanceof Evoker) {
                            return "EVOKER";
                        }
                    }

                    // WITCH POTION
                    if (damager instanceof ThrownPotion potion) {

                        if (potion.getShooter() instanceof Witch) {
                            return "WITCH";
                        }
                    }

                    // projectile shooter generic
                    if (damager instanceof Projectile projectile
                            && projectile.getShooter() instanceof Entity shooter) {

                        damager = shooter;
                    }

                    // END CRYSTAL
                    if (damager instanceof EnderCrystal) {
                        return "END_CRYSTAL";
                    }

                    // PLAYER jangan
                    if (damager instanceof Player) {
                        return null;
                    }

                    if (damager instanceof AreaEffectCloud cloud) {

                        if (cloud.getSource() instanceof EnderDragon) {
                            return "ENDER_DRAGON";
                        }
                    }

                    return damager.getType().name();
                }

                if (cause == DamageCause.MAGIC) {

                    return "MAGIC";
                }

                return null;

            case WITHER:
                return "MAGIC";
                
            case POISON:
                Bukkit.broadcast(
                        Component.text(
                                "[DEBUG] MASUK POISON",
                                NamedTextColor.YELLOW
                        )
                );
                if (player.getLastDamageCause() instanceof EntityDamageByEntityEvent edmg) {
                    Entity damager = edmg.getDamager();
                    if (damager instanceof Bee) return "BEE";
                    if (damager instanceof CaveSpider) return "CAVE_SPIDER";
                    if (damager instanceof Spider) return "SPIDER";
                    if (damager instanceof IronGolem) return "IRON_GOLEM";
                }
                if (player.getLastDamageCause()
                        instanceof EntityDamageByEntityEvent edmg) {

                    Entity damager = edmg.getDamager();

                    Bukkit.broadcast(
                            Component.text(
                                    "[DEBUG] Poison Damager = "
                                            + damager.getType()
                                            + " | "
                                            + damager.getClass().getName(),
                                    NamedTextColor.GOLD
                            )
                    );
                }
                return null;

            default:
                return null;
        }
    }

    @EventHandler
    public void onPlayerMoveWater(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!gm.isRunning() || !gm.isRegistered(player)) return;

        if ((player.isInWater() || (player.getEyeLocation().getBlock().getType() == Material.WATER)) && gm.isDeathUsed(player, "DROWNING")) {
            pushLikeSoulSand(player);
        }

        if (player.getFallDistance() > 4 && gm.isDeathUsed(player, "FALL")) {
            if (!activeChickenParachute.contains(player.getUniqueId())) {
                spawnChickenParachute(player);
            }
        }

        if (player.getLocation().getY() < -3.0 && gm.isDeathUsed(player, "VOID")) {
            applyVoidLevitation(player);
        }

        Block under = player.getLocation()
                .clone()
                .subtract(0, 0.2, 0)
                .getBlock();

        if (under.getType() == Material.MAGMA_BLOCK
                && gm.isDeathUsed(player, "MAGMA")) {

            // paksa sneak terus
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.setSneaking(true);
                }
            });

            player.spawnParticle(
                    Particle.SMOKE,
                    player.getLocation().add(0, 0.1, 0),
                    1,
                    0.1,
                    0.02,
                    0.1,
                    0
            );

        } else {

            // lepas sneak kalau udah turun
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.setSneaking(false);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!gm.isRunning() || !gm.isRegistered(player)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();

        switch (cause) {
            case CONTACT:

                Material contactMat = null;

                if (event instanceof EntityDamageByBlockEvent contactEvt
                        && contactEvt.getDamager() != null) {

                    contactMat = contactEvt.getDamager().getType();
                }

                // SWEET BERRY
                if (contactMat == Material.SWEET_BERRY_BUSH
                        && gm.isDeathUsed(player, "SWEET_BERRY")) {

                    event.setCancelled(true);

                    shrinkBerry(player);

                    player.sendMessage(
                            Component.text(
                                    "🍓 Tidak bisa mati dengan berry lagi!",
                                    NamedTextColor.RED
                            )
                    );

                // CACTUS
                } else if (contactMat == Material.CACTUS
                        && gm.isDeathUsed(player, "CACTUS")) {

                    event.setCancelled(true);

                    shrinkCactus(player);

                    player.sendMessage(
                            Component.text(
                                    "🌵 Tidak bisa mati dengan kaktus lagi!",
                                    NamedTextColor.RED
                            )
                    );

                // DRIPSTONE
                } else if (contactMat == Material.POINTED_DRIPSTONE
                        && gm.isDeathUsed(player, "DRIPSTONE")) {

                    event.setCancelled(true);

                    Block dripstone =
                            ((EntityDamageByBlockEvent) event).getDamager();

                    if (dripstone != null) {

                        Location loc = dripstone.getLocation();

                        // particle pecah
                        player.getWorld().spawnParticle(
                                Particle.BLOCK,
                                loc.clone().add(0.5, 0.5, 0.5),
                                25,
                                0.2,
                                0.2,
                                0.2,
                                dripstone.getBlockData()
                        );

                        // sound pecah
                        player.getWorld().playSound(
                                loc,
                                Sound.BLOCK_POINTED_DRIPSTONE_BREAK,
                                1.0f,
                                1.0f
                        );

                        // drop item
                        player.getWorld().dropItemNaturally(
                                loc,
                                new ItemStack(Material.POINTED_DRIPSTONE)
                        );

                        dripstone.setType(Material.AIR);
                    }
                }
                break;
            case DROWNING:
                if (gm.isDeathUsed(player, "DROWNING")) event.setCancelled(true);
                break;
            case FLY_INTO_WALL:
                if (gm.isDeathUsed(player, "ELYTRA")) event.setCancelled(true);
                break;
            case BLOCK_EXPLOSION:

                boolean isBedExplosion = pendingBedExplosion.contains(player.getUniqueId());
                boolean isRespawnAnchorExplosion = pendingRespawnAnchor.contains(player.getUniqueId());

                // BED
                if (isBedExplosion) {
                    if (gm.isDeathUsed(player, "BED")) {
                        event.setCancelled(true);
                    }
                // RESPAWN ANCHOR
                } else if (isRespawnAnchorExplosion) {
                    if (gm.isDeathUsed(player, "RESPAWN_ANCHOR")) {
                        event.setCancelled(true);
                    } else {
                        // Pertama kali meledak - spawn particles
                        Location aLoc = player.getLocation();
                        aLoc.getWorld().spawnParticle(Particle.EXPLOSION, aLoc, 3, 0.5, 0.5, 0.5, 0);
                        aLoc.getWorld().spawnParticle(Particle.END_ROD, aLoc, 25, 0.5, 0.8, 0.5, 0.15);
                        aLoc.getWorld().playSound(aLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.8f);
                    }
                // TNT
                } else {
                    if (gm.isDeathUsed(player, "TNT")) {
                        event.setCancelled(true);
                    }
                }

                break;
            case PROJECTILE:

                if (event instanceof EntityDamageByEntityEvent edmg) {

                    Entity damager = edmg.getDamager();

                    // ARROW
                    if (damager instanceof Arrow
                            || damager instanceof SpectralArrow) {

                        Projectile projectile = (Projectile) damager;

                        // hanya dispenser / arrow tanpa mob
                        if (!(projectile.getShooter() instanceof Mob)
                                && gm.isDeathUsed(player, "ARROW")) {

                            event.setCancelled(true);
                            damager.remove();
                        }

                        player.getWorld().spawnParticle(
                                Particle.CRIT,
                                player.getLocation().add(0, 1, 0),
                                12,
                                0.3,
                                0.3,
                                0.3,
                                0.05
                        );

                        player.playSound(
                                player.getLocation(),
                                Sound.ENTITY_ARROW_HIT_PLAYER,
                                0.7f,
                                1.5f
                        );
                    }

                    // TRIDENT
                    else if (damager instanceof Trident trident) {

                        if (trident.getShooter() instanceof Drowned) {

                            if (gm.isDeathUsed(player, "DROWNED")) {
                                event.setCancelled(true);
                                damager.remove();
                            }

                        } else if (gm.isDeathUsed(player, "TRIDENT")) {

                            event.setCancelled(true);
                            damager.remove();
                        }

                        player.getWorld().spawnParticle(
                                Particle.SPLASH,
                                player.getLocation().add(0, 1, 0),
                                20,
                                0.4,
                                0.4,
                                0.4,
                                0.1
                        );

                        player.playSound(
                                player.getLocation(),
                                Sound.ITEM_TRIDENT_RETURN,
                                1.0f,
                                1.2f
                        );
                    }

                    else if (damager instanceof SmallFireball sf
                            && !(sf.getShooter() instanceof Blaze)
                            && gm.isDeathUsed(player, "FIRE_CHARGE")) {

                        event.setCancelled(true);

                        damager.remove();

                        player.setFireTicks(0);

                        player.getWorld().spawnParticle(
                                Particle.SMOKE,
                                player.getLocation().add(0, 1, 0),
                                15,
                                0.3,
                                0.3,
                                0.3,
                                0.03
                        );

                        player.playSound(
                                player.getLocation(),
                                Sound.BLOCK_FIRE_EXTINGUISH,
                                1.0f,
                                1.2f
                        );
                    }

                    else if (damager instanceof Fireball fireball
                            && fireball.getShooter() instanceof Ghast
                            && gm.isDeathUsed(player, "GHAST")) {

                        event.setCancelled(true);

                        damager.remove();

                        player.setFireTicks(0);

                        player.getWorld().spawnParticle(
                                Particle.SMOKE,
                                player.getLocation().add(0, 1, 0),
                                15,
                                0.3,
                                0.3,
                                0.3,
                                0.03
                        );

                        player.playSound(
                                player.getLocation(),
                                Sound.BLOCK_FIRE_EXTINGUISH,
                                1.0f,
                                1.2f
                        );
                    }

                    else if (damager instanceof SmallFireball sf
                            && sf.getShooter() instanceof Blaze
                            && gm.isDeathUsed(player, "BLAZE")) {

                        event.setCancelled(true);

                        damager.remove();

                        player.setFireTicks(0);

                        player.getWorld().spawnParticle(
                                Particle.SMOKE,
                                player.getLocation().add(0, 1, 0),
                                15,
                                0.3,
                                0.3,
                                0.3,
                                0.03
                        );

                        player.playSound(
                                player.getLocation(),
                                Sound.BLOCK_FIRE_EXTINGUISH,
                                1.0f,
                                1.2f
                        );
                    }
                }

                break;
            case ENTITY_EXPLOSION:
                if (event instanceof EntityDamageByEntityEvent edmg) {
                    if (edmg.getDamager() instanceof Creeper && gm.isDeathUsed(player, "CREEPER")) event.setCancelled(true);
                    else if (edmg.getDamager() instanceof Firework && gm.isDeathUsed(player, "FIREWORK")) event.setCancelled(true);
                    else if (edmg.getDamager() instanceof TNTPrimed && gm.isDeathUsed(player, "TNT")) event.setCancelled(true);
                    else if (edmg.getDamager() instanceof EnderCrystal && gm.isDeathUsed(player, "END_CRYSTAL")) event.setCancelled(true);
                }
                break;
            case FALL:

                Block below = player.getLocation()
                        .clone()
                        .subtract(0, 1.0, 0)
                        .getBlock();

                Block feet = player.getLocation().getBlock();
                Block head = player.getEyeLocation().getBlock();

                boolean dripstoneFall =
                    below.getType() == Material.POINTED_DRIPSTONE
                    || feet.getType() == Material.POINTED_DRIPSTONE
                    || head.getType() == Material.POINTED_DRIPSTONE;

                if (dripstoneFall && gm.isDeathUsed(player, "DRIPSTONE")) {

                    event.setCancelled(true);

                } else if (gm.isDeathUsed(player, "FALL")) {

                    event.setCancelled(true);
                }

                break;
            case FALLING_BLOCK:
                boolean isDripstone = false;
                if (event instanceof EntityDamageByEntityEvent fallingEvt && fallingEvt.getDamager() instanceof FallingBlock fb) {
                    isDripstone = fb.getBlockData().getMaterial() == Material.POINTED_DRIPSTONE;
                }
                if (isDripstone && gm.isDeathUsed(player, "DRIPSTONE")) {
                    event.setCancelled(true);
                    breakFallingBlock(event);
                } else if (!isDripstone && gm.isDeathUsed(player, "ANVIL")) {
                    event.setCancelled(true);
                    breakFallingBlock(event);
                }
                break;
            case LAVA:
                if (gm.isDeathUsed(player, "LAVA")) {
                    event.setCancelled(true);
                    player.setFireTicks(0);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, false, false));
                    startLavaLoopEffect(player);
                }
                break;
            case CAMPFIRE:
                if (gm.isDeathUsed(player, "CAMPFIRE")) {
                    event.setCancelled(true);
                    extinguishNearbyCampfire(player);
                }
                break;
            case FIRE:
            case FIRE_TICK:

                Block fireBlock = player.getLocation()
                        .clone()
                        .subtract(0, 1, 0)
                        .getBlock();

                boolean isCampfireDamage = fireBlock.getType() == Material.CAMPFIRE
                        || fireBlock.getType() == Material.SOUL_CAMPFIRE;

                // Fallback: cek block di event langsung
                if (!isCampfireDamage && event instanceof EntityDamageByBlockEvent campfireEvt) {
                    Block b = campfireEvt.getDamager();
                    if (b != null && (b.getType() == Material.CAMPFIRE || b.getType() == Material.SOUL_CAMPFIRE)) {
                        isCampfireDamage = true;
                    }
                }

                // Fallback: cek recentCampfireBurn (player masih terbakar dari campfire sebelumnya)
                if (!isCampfireDamage && recentCampfireBurn.contains(player.getUniqueId())) {
                    isCampfireDamage = true;
                }

                // CAMPFIRE
                if (isCampfireDamage) {
                    // Tag player supaya FIRE_TICK berikutnya tetap ke-detect sebagai CAMPFIRE
                    if (!recentCampfireBurn.contains(player.getUniqueId())) {
                        recentCampfireBurn.add(player.getUniqueId());
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                recentCampfireBurn.remove(player.getUniqueId());
                            }
                        }.runTaskLater(plugin, 60L); // 3 detik, cukup cover fire tick
                    }

                    if (gm.isDeathUsed(player, "CAMPFIRE")) {
                        event.setCancelled(true);
                        player.setFireTicks(0);
                        recentCampfireBurn.remove(player.getUniqueId());
                        extinguishNearbyCampfire(player);
                        break;
                    }
                }

                // FIRE BIASA
                if (!isCampfireDamage && gm.isDeathUsed(player, "FIRE")) {

                    event.setCancelled(true);

                    player.setFireTicks(0);

                    spawnLoveEffect(player);
                }

                break;
            case LIGHTNING:
                if (gm.isDeathUsed(player, "LIGHTNING")) event.setCancelled(true);
                break;
            case HOT_FLOOR:

                Block hotBlock = player.getLocation().getBlock();

                Block underHot = player.getLocation()
                        .clone()
                        .subtract(0, 1, 0)
                        .getBlock();

                if (hotBlock.getType() == Material.CAMPFIRE
                        || hotBlock.getType() == Material.SOUL_CAMPFIRE
                        || underHot.getType() == Material.CAMPFIRE
                        || underHot.getType() == Material.SOUL_CAMPFIRE) {

                    // Tag recentCampfireBurn supaya FIRE_TICK berikutnya tetap ke-detect sebagai CAMPFIRE
                    if (!recentCampfireBurn.contains(player.getUniqueId())) {
                        recentCampfireBurn.add(player.getUniqueId());
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                recentCampfireBurn.remove(player.getUniqueId());
                            }
                        }.runTaskLater(plugin, 60L);
                    }

                    if (gm.isDeathUsed(player, "CAMPFIRE")) {
                        event.setCancelled(true);
                        player.setFireTicks(0);
                        recentCampfireBurn.remove(player.getUniqueId());
                        extinguishNearbyCampfire(player);
                        break;
                    }

                    break;
                }

                // MAGMA
                if (gm.isDeathUsed(player, "MAGMA")) {

                    event.setCancelled(true);
                }

                break;
            case MAGIC:

                if (event instanceof EntityDamageByEntityEvent edmg
                        && edmg.getDamager() instanceof ThrownPotion potion
                        && potion.getShooter() instanceof Witch) {

                    break; // biarin witch damage masuk
                }

                if (gm.isDeathUsed(player, "MAGIC")) {
                    event.setCancelled(true);
                }

                break;
            case DRAGON_BREATH:

                if (gm.isDeathUsed(player, "ENDER_DRAGON")) {

                    event.setCancelled(true);
                }

                break;
            case FREEZE:
                if (gm.isDeathUsed(player, "FREEZE")) event.setCancelled(true);
                break;
            case VOID:
                if (gm.isDeathUsed(player, "VOID")) event.setCancelled(true);
                break;
            case STARVATION:
                if (gm.isDeathUsed(player, "HUNGER")) {
                    event.setCancelled(true);
                    feedPlayer(player);
                }
                break;
            case SUFFOCATION:
                if (gm.isDeathUsed(player, "SUFFOCATION")) {
                    event.setCancelled(true);
                    breakBlockInFace(player);
                }
                break;
            case THORNS:
                if (gm.isDeathUsed(player, "THORNS")) event.setCancelled(true);
                break;
            case WORLD_BORDER:
            case CUSTOM:
                if (gm.isDeathUsed(player, "WORLD_BORDER")) {
                    event.setCancelled(true);
                    // Teleport masuk border
                    WorldBorder wb2 = player.getWorld().getWorldBorder();
                    Location c2 = wb2.getCenter();
                    double h2 = wb2.getSize() / 2.0;
                    Location p2 = player.getLocation();
                    boolean isOutside = p2.getX() < c2.getX() - h2 || p2.getX() > c2.getX() + h2
                            || p2.getZ() < c2.getZ() - h2 || p2.getZ() > c2.getZ() + h2;
                    if (isOutside) {
                        double safeX = Math.max(c2.getX() - h2 + 3, Math.min(p2.getX(), c2.getX() + h2 - 3));
                        double safeZ = Math.max(c2.getZ() - h2 + 3, Math.min(p2.getZ(), c2.getZ() + h2 - 3));
                        World world = player.getWorld();

                        int safeY = world.getHighestBlockYAt((int) safeX, (int) safeZ) + 1;

                        Location safeLoc = new Location(
                                world,
                                safeX,
                                safeY,
                                safeZ,
                                player.getLocation().getYaw(),
                                player.getLocation().getPitch()
                        );
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.teleport(safeLoc);
                            player.setNoDamageTicks(40);
                            player.playSound(safeLoc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 0.8f);
                            player.spawnParticle(Particle.PORTAL, safeLoc.clone().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.2);
                            player.sendMessage(Component.text("🌍 Border menolakmu keluar!", NamedTextColor.RED));
                        });
                    }
                }
                break;
            default:
                // HOSTILE MOB FEAR IMMUNITY
                if (event instanceof EntityDamageByEntityEvent mobEvt
                        && mobEvt.getDamager() instanceof Mob mob) {

                    // spider udah punya handler sendiri
                    if (!(mob instanceof Spider)) {

                        String key = mob.getType().name();

                        if (gm.isDeathUsed(player, key)) {

                            event.setCancelled(true);

                            mob.setTarget(null);

                            // PUFFERFISH jangan ngembang
                            if (mob instanceof PufferFish puffer) {
                                puffer.setPuffState(0);
                            }

                            // GHAST stop charge
                            if (mob instanceof Ghast ghast) {
                                ghast.setCharging(false);
                            }

                            return;
                        }
                    }
                }
                // BEE
                if (event instanceof EntityDamageByEntityEvent edmgBee
                        && edmgBee.getDamager() instanceof Bee bee
                        && gm.isDeathUsed(player, "BEE")) {
                    event.setCancelled(true);
                    // Bee ngehadap player dan kasih madu
                    bee.setTarget(null);
                    bee.lookAt(player);
                    bee.setAnger(0);
                    Location honeyLoc = player.getLocation().add(0, 1, 0);
                    player.getWorld().spawnParticle(
                            Particle.DRIPPING_HONEY,
                            player.getLocation().add(0, 1, 0),
                            35,
                            0.4,
                            0.5,
                            0.4,
                            0.02
                    );

                    player.getWorld().spawnParticle(
                            Particle.FALLING_HONEY,
                            player.getLocation().add(0, 1, 0),
                            20,
                            0.3,
                            0.4,
                            0.3,
                            0.01
                    );

                    player.playSound(
                            bee.getLocation(),
                            Sound.ITEM_HONEY_BOTTLE_DRINK,
                            1.0f,
                            1.2f
                    );
                    player.getWorld().spawnParticle(Particle.WAX_ON, honeyLoc, 20, 0.4, 0.5, 0.4, 0.05);
                    player.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_POLLINATE, 1.0f, 1.0f);
                    break;
                }
                // CAVE_SPIDER / SPIDER — damage dealt to player (malam hari = spider menyerang)
                if (event instanceof EntityDamageByEntityEvent edmgSpider) {
                    Entity sp = edmgSpider.getDamager();
                    String spKey = (sp instanceof CaveSpider) ? "CAVE_SPIDER" : (sp instanceof Spider) ? "SPIDER" : null;
                    if (spKey != null && gm.isDeathUsed(player, spKey)) {
                        event.setCancelled(true);
                    }
                }
                // IRON_GOLEM
                if (event instanceof EntityDamageByEntityEvent edmgGolem
                        && edmgGolem.getDamager() instanceof IronGolem golem
                        && gm.isDeathUsed(player, "IRON_GOLEM")) {
                    event.setCancelled(true);
                    golemGiveFlower(golem, player);
                }
                break;
        }
    }

    @EventHandler
    public void onEnderPearlThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;
        if (!gm.isRunning() || !gm.isRegistered(player)) return;

        // Particles saat lempar pearl
        player.getWorld().spawnParticle(Particle.PORTAL, pearl.getLocation(), 15, 0.2, 0.2, 0.2, 0.05);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1.0f, 1.0f);
    }

    @EventHandler
    public void onEnderPearlDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!gm.isRunning() || !gm.isRegistered(player)) return;
        if (!(event.getDamager() instanceof EnderPearl)) return;

        // Particles saat pearl landing
        Location landLoc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.PORTAL, landLoc, 30, 0.4, 0.8, 0.4, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        player.setMetadata("LOMBA_EPEARL_KILL", new FixedMetadataValue(plugin, true));

        if (gm.isDeathUsed(player, "ENDER_PEARL")) {
            event.setCancelled(true);
            player.removeMetadata("LOMBA_EPEARL_KILL", plugin);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEndCrystalClick(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof EnderCrystal crystal)) return;
        Player player = event.getPlayer();
        if (!gm.isRunning() || !gm.isRegistered(player)) return;
        if (!gm.isDeathUsed(player, "END_CRYSTAL")) return;

        // Sudah pernah mati sama end crystal - hapus crystal, jangan meledak
        event.setCancelled(true);
        Location loc = crystal.getLocation().add(0.5, 0.5, 0.5);
        event.setCancelled(true);

        crystal.remove();

        event.getPlayer().swingMainHand();

        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 40, 0.6, 0.8, 0.6, 0.15);
        loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 30, 0.4, 0.4, 0.4, 0.03);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, 0.8f, 1.5f);
        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEndCrystalExplosion(EntityExplodeEvent event) {
        if (!gm.isRunning()) return;
        if (!(event.getEntity() instanceof EnderCrystal crystal)) return;

        Location loc = crystal.getLocation();

        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 5, 0.5, 0.5, 0.5, 0);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 60, 1.2, 1.2, 1.2, 0.25);
        loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 50, 1.0, 1.0, 1.0, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, 1.0f, 1.5f);
    }

    @EventHandler
    public void onEndCrystalExplode(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!gm.isRunning() || !gm.isRegistered(player)) return;
        if (!(event.getDamager() instanceof EnderCrystal)) return;

        if (gm.isDeathUsed(player, "END_CRYSTAL")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEnderPearlTeleport(PlayerTeleportEvent event) {

        Player player = event.getPlayer();

        if (!gm.isRunning() || !gm.isRegistered(player)) return;

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                && gm.isDeathUsed(player, "ENDER_PEARL")) {

            // kasih immunity damage pearl sebentar
            player.setNoDamageTicks(40);

            player.getWorld().spawnParticle(
                    Particle.PORTAL,
                    player.getLocation(),
                    20,
                    0.4,
                    0.8,
                    0.4,
                    0.1
            );

            player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_ENDERMAN_TELEPORT,
                    1.0f,
                    1.2f
            );
        }
    }

    @EventHandler
    public void onEntityEnterBlock(EntityEnterBlockEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!gm.isRunning() || !gm.isRegistered(player)) return;
        if (event.getBlock().getType() != Material.POWDER_SNOW) return;
        if (!gm.isDeathUsed(player, "FREEZE")) return;

        if (!frozenBoots.contains(player.getUniqueId())) {
            frozenBoots.add(player.getUniqueId());
            ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
            ItemMeta meta = boots.getItemMeta();
            boots.setItemMeta(meta);
            
            player.getInventory().setBoots(boots);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!gm.isRunning() || !gm.isRegistered(player)) return;
        if (!frozenBoots.contains(player.getUniqueId())) return;

        boolean isBootsSlot = (event.getSlot() == 36 || event.getRawSlot() == 36);
        boolean isShiftClickBoots = event.isShiftClick()
                && event.getCurrentItem() != null
                && event.getCurrentItem().getType() == Material.LEATHER_BOOTS;

        if (isBootsSlot || isShiftClickBoots) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractBed(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!gm.isRunning() || !gm.isRegistered(player)) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        World.Environment env = player.getWorld().getEnvironment();
        boolean isDangerousWorld = env == World.Environment.NETHER || env == World.Environment.THE_END;
        boolean isBed = Tag.BEDS.isTagged(block.getType());
        // FIX DETEKSI BED EXPLOSION
        if (isDangerousWorld && isBed) {

            pendingBedExplosion.add(player.getUniqueId());

            new BukkitRunnable() {
                @Override
                public void run() {
                    pendingBedExplosion.remove(player.getUniqueId());
                }
            }.runTaskLater(plugin, 40L);
        }

        if (isDangerousWorld && isBed && gm.isDeathUsed(player, "BED")) {
            event.setCancelled(true);
            Location bedLoc = block.getLocation().add(0.5, 0.5, 0.5);
            block.setType(Material.AIR);
            
            block.getWorld().spawnParticle(Particle.GUST, bedLoc, 35, 0.6, 0.6, 0.6, 0.3);
            block.getWorld().playSound(bedLoc, Sound.ENTITY_BREEZE_WIND_BURST, 1.2f, 1.0f);
            block.getWorld().playSound(bedLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
        }
    }

    @EventHandler
    public void onPlayerInteractRespawnAnchor(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!gm.isRunning() || !gm.isRegistered(player)) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.RESPAWN_ANCHOR) return;

        World.Environment env = player.getWorld().getEnvironment();
        // Respawn anchor meledak di Overworld dan The End (bukan di Nether)
        boolean isDangerousWorld = env == World.Environment.NORMAL || env == World.Environment.THE_END;
        if (!isDangerousWorld) return;

        // Tag pending supaya onPlayerDeath bisa detect
        pendingRespawnAnchor.add(player.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                pendingRespawnAnchor.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, 40L);

        if (gm.isDeathUsed(player, "RESPAWN_ANCHOR")) {
            event.setCancelled(true);
            Location anchorLoc = block.getLocation().add(0.5, 0.5, 0.5);
            block.setType(Material.AIR);
            block.getWorld().spawnParticle(Particle.EXPLOSION, anchorLoc, 3, 0.3, 0.3, 0.3, 0.1);
            block.getWorld().spawnParticle(Particle.END_ROD, anchorLoc, 30, 0.5, 0.5, 0.5, 0.1);
            block.getWorld().playSound(anchorLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
            block.getWorld().playSound(anchorLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onElytraFireworkBoost(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!gm.isRunning() || !gm.isRegistered(player)) return;
        if (!player.isGliding() || !gm.isDeathUsed(player, "ELYTRA")) return;

        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.FIREWORK_ROCKET) {
            event.setCancelled(true); 
            player.sendMessage(Component.text("🪂 Tidak bisa menggunakkan firework!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.6f, 1.5f);
        }
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!gm.isRunning() || !gm.isRegistered(player)) return;
        if (!gm.isDeathUsed(player, "MAGIC")) return;

        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null) return;

        Set<PotionEffectType> harmful = Set.of(
                PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.INSTANT_DAMAGE
        );

        if (harmful.contains(newEffect.getType())) {

            // jangan cancel kalau dari witch
            if (witchPotionVictims.contains(player.getUniqueId())) {
                return;
            }

            event.setCancelled(true);

            player.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.REGENERATION,
                            100,
                            1
                    )
            );
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {

        if (!(event.getPotion().getShooter() instanceof Witch))
            return;

        for (LivingEntity entity : event.getAffectedEntities()) {

            if (entity instanceof Player player) {
                witchPotionVictims.add(player.getUniqueId());

                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        witchPotionVictims.remove(player.getUniqueId()),
                        40L
                );
            }
        }
    }

    private void startLavaLoopEffect(Player player) {
        if (activeLavaTasks.containsKey(player.getUniqueId())) return;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !gm.isRunning() || 
                    (!player.getLocation().getBlock().getType().name().contains("LAVA") && 
                        player.getFireTicks() <= 0)) {
                    activeLavaTasks.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.2, 0), 4, 0.3, 0.4, 0.3, 0);
            }
        };
        task.runTaskTimer(plugin, 0L, 5L);
        activeLavaTasks.put(player.getUniqueId(), task);
    }
    
    private void spawnLoveEffect(Player player) {

        player.getWorld().spawnParticle(
                Particle.HEART,
                player.getLocation().add(0, 1.2, 0),
                4,
                0.3,
                0.4,
                0.3,
                0
        );
    }

    private void spawnChickenParachute(Player player) {

        activeChickenParachute.add(player.getUniqueId());

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.SLOW_FALLING,
                        160,
                        0,
                        false,
                        false
                )
        );

        Location loc = player.getLocation().add(0, 2.4, 0);

        Chicken chicken = (Chicken) player.getWorld().spawnEntity(loc, EntityType.CHICKEN);

        chicken.setAI(true);
        chicken.setGravity(false);
        chicken.setSilent(true);
        chicken.setInvulnerable(true);
        player.addPassenger(chicken);
        Bukkit.broadcast(
            Component.text(
                "<Chicken> " + player.getName() + " berat banget jirr"
            )
        );

        new BukkitRunnable() {

            int ticks = 0;

            @Override
            public void run() {

                if (!player.isOnline()
                        || !chicken.isValid()
                        || player.getLocation()
                            .clone()
                            .subtract(0, 0.15, 0)
                            .getBlock()
                            .getType()
                            .isSolid()) {

                    player.removePassenger(chicken);

                    player.removePotionEffect(PotionEffectType.SLOW_FALLING);

                    chicken.remove();

                    activeChickenParachute.remove(player.getUniqueId());

                    cancel();
                    return;
                }

                if (ticks % 5 == 0) {

                    player.getWorld().playSound(
                            chicken.getLocation(),
                            Sound.ENTITY_CHICKEN_AMBIENT,
                            0.15f,
                            1.5f
                    );
                }

                ticks++;
            }

        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void pushLikeSoulSand(Player player) {
        if (waterPushed.contains(player.getUniqueId())) return;
        waterPushed.add(player.getUniqueId());

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks > 40 || !player.isInWater() || (player.getEyeLocation().getBlock().getType() != Material.WATER)) {
                    waterPushed.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                Vector vel = player.getVelocity();
                vel.setY(Math.min(vel.getY() + 0.25, 0.55));
                player.setVelocity(vel);

                Location playerLoc = player.getLocation();
                for (double y = 0; y <= 1.5; y += 0.5) {
                    player.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, playerLoc.clone().add(0, y, 0), 2, 0.1, 0, 0.1, 0.02);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void shrinkCactus(Player player) {
        Location loc = player.getLocation();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 2; dy >= -1; dy--) { 
                for (int dz = -2; dz <= 2; dz++) {
                    Block block = loc.clone().add(dx, dy, dz).getBlock();
                    if (block.getType() == Material.CACTUS) {
                        Block below = block.getRelative(0, -1, 0);
                        if (below.getType() != Material.CACTUS) {
                            block.setType(Material.POTTED_CACTUS);
                        } else {
                            block.setType(Material.AIR);
                        }
                    }
                }
            }
        }
    }

    private void shrinkBerry(Player player) {
        Location loc = player.getLocation();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Block block = loc.clone().add(dx, 0, dz).getBlock();
                if (block.getType() == Material.SWEET_BERRY_BUSH) {
                    org.bukkit.block.data.Ageable ageable = (org.bukkit.block.data.Ageable) block.getBlockData();
                    ageable.setAge(0);
                    block.setBlockData(ageable);
                }
            }
        }
    }

    private void applyVoidLevitation(Player player) {

        if (voidLevitation.contains(player.getUniqueId())) return;

        voidLevitation.add(player.getUniqueId());

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.LEVITATION,
                        120,
                        10,
                        false,
                        false
                )
        );

        new BukkitRunnable() {

            int ticks = 0;

            @Override
            public void run() {

                if (!player.isOnline()
                        || ticks > 120
                        || player.isDead()) {

                    voidLevitation.remove(player.getUniqueId());

                    cancel();
                    return;
                }

                // partikel kaki
                player.getWorld().spawnParticle(
                        Particle.CLOUD,
                        player.getLocation().add(0, 0.1, 0),
                        4,
                        0.2,
                        0.02,
                        0.2,
                        0.01
                );

                ticks++;
            }

        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void breakBlockInFace(Player player) {
        Block b = player.getEyeLocation().getBlock();
        if (b.getType().isSolid()) {
            player.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(b.getType()));
            b.setType(Material.AIR);
        }
    }

    private void breakFallingBlock(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent edmg && edmg.getDamager() instanceof FallingBlock fb) {
            fb.getWorld().dropItemNaturally(fb.getLocation(), new ItemStack(fb.getBlockData().getMaterial()));
            fb.remove();
        }
    }

    private boolean isOnCampfire(Player player) {
        Location loc = player.getLocation();
        // Cek block yang player injak (y-1), posisi kaki (y), dan posisi mata
        Block[] toCheck = {
            loc.getBlock(),                          // posisi kaki
            loc.clone().subtract(0, 1, 0).getBlock(), // block di bawah kaki
            loc.clone().subtract(0, 0.5, 0).getBlock() // tengah badan
        };
        for (Block b : toCheck) {
            if (b.getType() == Material.CAMPFIRE || b.getType() == Material.SOUL_CAMPFIRE) {
                return true;
            }
        }
        return false;
    }

    private void extinguishNearbyCampfire(Player player) {
        Location loc = player.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block b = loc.clone().add(dx, 0, dz).getBlock();
                if (b.getType() == Material.CAMPFIRE || b.getType() == Material.SOUL_CAMPFIRE) {
                    org.bukkit.block.data.type.Campfire cf = (org.bukkit.block.data.type.Campfire) b.getBlockData();
                    if (cf.isLit()) { cf.setLit(false); b.setBlockData(cf); }
                }
            }
        }
    }

    private void feedPlayer(Player player) {

        if (player.isDead()) return;

        if (eatingAnimation.contains(player.getUniqueId())) {
            return;
        }

        eatingAnimation.add(player.getUniqueId());

        Bukkit.broadcast(
                Component.text(
                        "<pororo> " + player.getName() + " kelaperan jirr, nih gw kasih MBG!"
                )
        );

        new BukkitRunnable() {

            int ticks = 0;

            @Override
            public void run() {

                if (!player.isOnline() || !gm.isRunning()) {
                    eatingAnimation.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                int food = player.getFoodLevel();

                if (food >= 20) {
                    eatingAnimation.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                player.getWorld().spawnParticle(
                        Particle.ITEM,
                        player.getLocation().add(0, 1.5, 0),
                        3,
                        0.2,
                        0.2,
                        0.2,
                        0.05,
                        new ItemStack(Material.BEETROOT_SOUP)
                );

                if (ticks % 10 == 0) {
                    player.playSound(
                            player.getLocation(),
                            Sound.ENTITY_GENERIC_EAT,
                            1.0f,
                            1.0f
                    );

                    player.setFoodLevel(Math.min(20, food + 1));
                }

                ticks++;

                if (ticks > 200) {
                    player.setFoodLevel(20);
                    player.setSaturation(20f);

                    eatingAnimation.remove(player.getUniqueId());

                    cancel();
                }
            }

        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void checkHunger(Player player) {

        if (player.isDead()) return;

        if (!gm.isRunning() || !gm.isRegistered(player)) return;
        if (!gm.isDeathUsed(player, "HUNGER")) return;
        if (eatingAnimation.contains(player.getUniqueId())) return;

        if (player.getFoodLevel() <= 1) {
            feedPlayer(player);
        }
    }

    // ========== PLAYER HITS MOB ==========
    private final Set<EntityType> fearfulMobs = Set.of(

        EntityType.PIGLIN,
        EntityType.PUFFERFISH,
        EntityType.BLAZE,
        EntityType.BOGGED,
        EntityType.BREEZE,

        EntityType.ELDER_GUARDIAN,

        EntityType.ENDERMITE,
        EntityType.EVOKER,
        EntityType.GHAST,
        EntityType.GUARDIAN,
        EntityType.HOGLIN,
        EntityType.HUSK,
        EntityType.MAGMA_CUBE,

        EntityType.PIGLIN_BRUTE,
        EntityType.PILLAGER,
        EntityType.RAVAGER,

        EntityType.SILVERFISH,
        EntityType.SKELETON,
        EntityType.SLIME,
        EntityType.STRAY,
        EntityType.VEX,
        EntityType.VINDICATOR,
        EntityType.WARDEN,
        EntityType.WITCH,

        EntityType.WITHER_SKELETON,
        EntityType.ZOGLIN,
        EntityType.ZOMBIE,
        EntityType.ZOMBIE_VILLAGER
    );
    // Set untuk throttle spider flee agar tidak spam tiap tick
    private final Set<UUID> fleeingSpiders = new HashSet<>();
    // Set untuk throttle golem flower agar tidak spam
    private final Set<UUID> golemCooldown = new HashSet<>();

    /**
     * Event: player mukul entity (bee / cave spider / spider / iron golem / villager)
     * Dihandle via EntityDamageByEntityEvent dengan damager = player
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerHitMob(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!gm.isRunning() || !gm.isRegistered(player)) return;

        Entity target = event.getEntity();

        if (target instanceof Enderman enderman
                && gm.isDeathUsed(player, "ENDERMAN")) {

            event.setCancelled(true);

            enderman.getWorld().playSound(
                    enderman.getLocation(),
                    Sound.ENTITY_ENDERMAN_TELEPORT,
                    1.0f,
                    1.2f
            );

            enderman.getWorld().spawnParticle(
                    Particle.PORTAL,
                    enderman.getLocation().add(0, 1, 0),
                    50,
                    0.5,
                    1,
                    0.5,
                    0.1
            );

            enderman.teleportRandomly();

            return;
        }

        if (target.getType() == EntityType.ZOMBIFIED_PIGLIN
                && gm.isDeathUsed(player, "ZOMBIFIED_PIGLIN")) {

            event.setCancelled(true);

            LivingEntity piglin = (LivingEntity) target;

            piglin.setHealth(0);

            return;
        }

        // === BEE: player mukul bee, player sudah pernah mati sama bee ===
        if (target instanceof Bee bee && gm.isDeathUsed(player, "BEE")) {
            event.setCancelled(true); // cancel damage, bee damai
            bee.setAnger(0);
            bee.setTarget(null);
            // Bee ngehadap player
            bee.lookAt(player);
            player.getWorld().spawnParticle(
                    Particle.DRIPPING_HONEY,
                    player.getLocation().add(0, 1, 0),
                    35,
                    0.4,
                    0.5,
                    0.4,
                    0.02
            );

            player.getWorld().spawnParticle(
                    Particle.FALLING_HONEY,
                    player.getLocation().add(0, 1, 0),
                    20,
                    0.3,
                    0.4,
                    0.3,
                    0.01
            );

            player.playSound(
                    bee.getLocation(),
                    Sound.ITEM_HONEY_BOTTLE_DRINK,
                    1.0f,
                    1.2f
            );
            player.getWorld().spawnParticle(Particle.WAX_ON, bee.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.02);
            player.getWorld().playSound(bee.getLocation(), Sound.ENTITY_BEE_POLLINATE, 1.0f, 1.0f);
            return;
        }

        // === CAVE SPIDER (siang): mukul → spawn cobweb ===
        if (target instanceof CaveSpider caveSpider && gm.isDeathUsed(player, "CAVE_SPIDER")) {
            boolean isDaytime = player.getWorld().getTime() < 13000;
            if (isDaytime) {
                event.setCancelled(true);
                spawnCobwebAround(caveSpider.getLocation());
                player.getWorld().playSound(caveSpider.getLocation(), Sound.BLOCK_COBWEB_PLACE, 1.0f, 0.8f);
                return;
            }
        }

        // === SPIDER (siang): mukul → spawn cobweb ===
        if (target instanceof Spider spider && !(target instanceof CaveSpider) && gm.isDeathUsed(player, "SPIDER")) {
            boolean isDaytime = player.getWorld().getTime() < 13000;
            if (isDaytime) {
                event.setCancelled(true);
                spawnCobwebAround(spider.getLocation());
                player.getWorld().playSound(spider.getLocation(), Sound.BLOCK_COBWEB_PLACE, 1.0f, 0.8f);
                return;
            }
        }

        if ((target instanceof Llama || target instanceof TraderLlama)
                && gm.isDeathUsed(player, target.getType().name())) {

            event.setCancelled(true);

            ((Mob) target).lookAt(player);

            spitWater(target, player);

            return;
        }

        // === IRON GOLEM: player mukul iron golem ===
        if (target instanceof IronGolem golem && gm.isDeathUsed(player, "IRON_GOLEM")) {
            event.setCancelled(true);
            golemGiveFlower(golem, player);
            return;
        }

    }

    @EventHandler
    public void onPlayerLookAtEnderman(PlayerMoveEvent event) {

        Player player = event.getPlayer();

        if (!gm.isRunning())
            return;

        if (!gm.isRegistered(player))
            return;

        if (!gm.isDeathUsed(player, "ENDERMAN"))
            return;

        for (Entity entity : player.getNearbyEntities(72, 8, 72)) {

            if (!(entity instanceof Enderman enderman))
                continue;

            if (!player.hasLineOfSight(enderman))
                continue;

            Vector toEnderman = enderman.getEyeLocation()
                    .toVector()
                    .subtract(player.getEyeLocation().toVector())
                    .normalize();

            double dot = player.getEyeLocation()
                    .getDirection()
                    .normalize()
                    .dot(toEnderman);

            // semakin mendekati 1 = semakin tepat ditatap
            if (dot < 0.97)
                continue;

            enderman.getWorld().spawnParticle(
                    Particle.PORTAL,
                    enderman.getLocation().add(0, 1, 0),
                    50,
                    0.5,
                    1,
                    0.5,
                    0.1
            );

            enderman.getWorld().playSound(
                    enderman.getLocation(),
                    Sound.ENTITY_ENDERMAN_SCREAM,
                    1.0f,
                    1.5f
            );

            enderman.setHealth(0);
        }
    }

    @EventHandler
    public void onGolemTarget(EntityTargetLivingEntityEvent event) {

        if (!(event.getEntity() instanceof IronGolem)) return;

        if (!(event.getTarget() instanceof Player player)) return;

        if (!gm.isRunning() || !gm.isRegistered(player)) return;

        if (!gm.isDeathUsed(player, "IRON_GOLEM")) return;

        // golem jadi baik
        event.setCancelled(true);
    }

    @EventHandler
    public void onDragonTarget(EntityTargetLivingEntityEvent event) {

        if (!(event.getEntity() instanceof EnderDragon))
            return;

        if (!(event.getTarget() instanceof Player player))
            return;

        if (!gm.isRunning())
            return;

        if (!gm.isRegistered(player))
            return;

        if (!gm.isDeathUsed(player, "ENDER_DRAGON"))
            return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {

        if (!(event.getEntity().getShooter() instanceof Mob mob))
            return;
        if (mob instanceof Ghast) {

            Bukkit.broadcast(
                    Component.text(
                            "[DEBUG GHAST] deathUsed="
                                    + gm.isDeathUsed(
                                            Bukkit.getOnlinePlayers()
                                                    .iterator()
                                                    .next(),
                                            "GHAST"
                                    ),
                            NamedTextColor.LIGHT_PURPLE
                    )
            );
        }

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!gm.isRegistered(player))
                continue;

            String key = mob.getType().name();

            if (!gm.isDeathUsed(player, key))
                continue;

            if (mob instanceof Ghast
                    && gm.isDeathUsed(player, "GHAST")) {

                event.setCancelled(true);
                return;
            }

            if (mob.getLocation().distance(player.getLocation()) <= 20) {

                event.setCancelled(true);
                return;
            }
        }
    }

    private void spitWater(Entity llama, Player player) {

        LivingEntity mob = (LivingEntity) llama;

        // matiin AI dulu
        mob.setAI(false);

        // paksa nengok ke player
        Vector lookDir = player.getEyeLocation()
                .toVector()
                .subtract(mob.getEyeLocation().toVector());

        Location loc = mob.getLocation();
        loc.setDirection(lookDir);

        mob.teleport(loc);

        // kasih jeda dikit biar sempet muter
        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            mob.getWorld().playSound(
                    mob.getLocation(),
                    Sound.ENTITY_LLAMA_SPIT,
                    1.0f,
                    1.0f
            );

            new BukkitRunnable() {

                double distance = 0;

                @Override
                public void run() {

                    if (!mob.isValid()
                            || !player.isOnline()) {

                        mob.setAI(true);
                        cancel();
                        return;
                    }

                    distance += 0.4;

                    Location llamaEye = mob.getEyeLocation();

                    Vector direction = player.getEyeLocation()
                            .toVector()
                            .subtract(llamaEye.toVector())
                            .normalize();

                    Vector drop = new Vector(
                            0,
                            -(distance * 0.03),
                            0
                    );

                    Location point = llamaEye.clone().add(
                            direction.clone()
                                    .multiply(distance)
                                    .add(drop)
                    );

                    point.getWorld().spawnParticle(
                            Particle.SPLASH,
                            point,
                            2,
                            0,
                            0,
                            0,
                            0
                    );

                    point.getWorld().spawnParticle(
                            Particle.BUBBLE,
                            point,
                            1,
                            0,
                            0,
                            0,
                            0
                    );

                    // kena player
                    if (point.distanceSquared(player.getEyeLocation()) < 0.8) {

                        point.getWorld().spawnParticle(
                                Particle.SPLASH,
                                point,
                                30,
                                0.25,
                                0.25,
                                0.25,
                                0.15
                        );

                        point.getWorld().spawnParticle(
                                Particle.BUBBLE,
                                point,
                                15,
                                0.2,
                                0.2,
                                0.2,
                                0.05
                        );

                        point.getWorld().playSound(
                                point,
                                Sound.ENTITY_PLAYER_SPLASH,
                                0.8f,
                                1.4f
                        );

                        mob.setAI(true);
                        cancel();
                        return;
                    }

                    if (distance > 8) {

                        mob.setAI(true);
                        cancel();
                    }
                }

            }.runTaskTimer(plugin, 0L, 1L);

        }, 5L);
    }

    public void startFearMobTask() {

        new BukkitRunnable() {

            @Override
            public void run() {

                if (!gm.isRunning()) return;

                for (Player player : Bukkit.getOnlinePlayers()) {

                    if (!gm.isRegistered(player)) continue;

                    for (Entity e : player.getWorld().getNearbyEntities(
                            player.getLocation(),
                            20,
                            8,
                            20
                    )) {

                        if (!(e instanceof Mob mob)) continue;

                        // ===== ENDER DRAGON =====

                        if (mob instanceof EnderDragon dragon
                                && gm.isDeathUsed(player, "ENDER_DRAGON")) {

                            dragon.setTarget(null);

                            double distance = dragon.getLocation()
                                    .distance(player.getLocation());

                            if (distance <= 15) {

                                Location eggLoc =
                                        dragon.getWorld()
                                                .getHighestBlockAt(
                                                        dragon.getLocation()
                                                )
                                                .getLocation()
                                                .add(0, 1, 0);

                                dragon.getWorld().spawnParticle(
                                        Particle.DRAGON_BREATH,
                                        dragon.getLocation(),
                                        100,
                                        2,
                                        2,
                                        2,
                                        0
                                );

                                dragon.remove();

                                eggLoc.getBlock()
                                        .setType(Material.DRAGON_EGG);
                            }

                            continue;
                        }

                        if (mob instanceof Shulker shulker
                                && gm.isDeathUsed(player, "SHULKER")) {

                            shulker.setPeek(0);

                            shulker.setTarget(null);
                            
                            shulker.setSilent(true);

                            continue;
                        }

                        if (mob instanceof Phantom phantom
                                && gm.isDeathUsed(player, "PHANTOM")) {

                            phantom.setTarget(null);

                            double angle =
                                    (System.currentTimeMillis() / 250.0)
                                            + phantom.getEntityId();

                            Location orbit =
                                    player.getLocation()
                                            .clone()
                                            .add(
                                                    Math.cos(angle) * 5,
                                                    8,
                                                    Math.sin(angle) * 5
                                            );

                            phantom.getPathfinder()
                                    .moveTo(orbit);

                            continue;
                        }

                        // spider udah punya task sendiri
                        if (mob instanceof Spider) continue;

                        if (!fearfulMobs.contains(mob.getType())) continue;

                        String key = mob.getType().name();

                        if (!gm.isDeathUsed(player, key)) continue;

                        double distance = mob.getLocation()
                                .distance(player.getLocation());

                        // cuma pas udah aggro range
                        if (distance > 16) continue;

                        // stop target
                        mob.setTarget(null);

                        // PUFFERFISH jangan ngembang
                        if (mob instanceof PufferFish puffer) {
                            puffer.setPuffState(0);
                            puffer.setSilent(true);
                        }

                        // GHAST stop charge
                        if (mob instanceof Ghast ghast) {
                            ghast.setCharging(false);
                        }

                        if (mob.getTarget() != null) {
                            mob.setTarget(null);
                        }

                        // SLIME & MAGMA CUBE
                        if (mob instanceof Slime slime) {

                            slime.addPotionEffect(
                                    new PotionEffect(
                                            PotionEffectType.JUMP_BOOST,
                                            20,
                                            20,
                                            false,
                                            false
                                    )
                            );
                        }

                        Location mobLoc = mob.getLocation();

                        Vector away = mobLoc.toVector()
                                .subtract(player.getLocation().toVector());

                        if (away.lengthSquared() == 0) {
                            away = new Vector(1, 0, 0);
                        } else {
                            away.normalize();
                        }

                        Location targetLoc = mobLoc.clone().add(
                                away.getX() * 10,
                                0,
                                away.getZ() * 10
                        );

                        // jalan natural
                        mob.getPathfinder().moveTo(targetLoc);
                        // VEX
                        if (mob instanceof Vex vex) {

                            Vector vexAway = targetLoc.toVector()
                                    .subtract(vex.getLocation().toVector())
                                    .normalize()
                                    .multiply(0.7);

                            vex.setVelocity(vexAway);
                        }
                        mob.lookAt(targetLoc);

                        mob.addPotionEffect(
                                new PotionEffect(
                                        PotionEffectType.SPEED,
                                        20,
                                        3,
                                        false,
                                        false
                                )
                        );
                    }
                }

            }

        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void startSpiderFearTask() {

        new BukkitRunnable() {

            @Override
            public void run() {

                if (!gm.isRunning()) return;

                for (Player player : Bukkit.getOnlinePlayers()) {

                    if (!gm.isRegistered(player)) continue;

                    boolean isNight =
                            player.getWorld().getTime() >= 13000;

                    if (!isNight) continue;

                    for (Entity e : player.getWorld().getNearbyEntities(
                            player.getLocation(),
                            17,
                            8,
                            17
                    )) {

                        if (!(e instanceof Spider spider)) continue;

                        boolean isCave =
                                spider instanceof CaveSpider;

                        String key = isCave
                                ? "CAVE_SPIDER"
                                : "SPIDER";

                        if (!gm.isDeathUsed(player, key)) continue;

                        double distance = spider.getLocation()
                                .distance(player.getLocation());

                        // cuma pas udah aggro range
                        if (distance > 16) continue;

                        spider.setTarget(null);

                        Location spiderLoc = spider.getLocation();

                        Vector away = spiderLoc.toVector()
                                .subtract(player.getLocation().toVector())
                                .normalize();

                        Location targetLoc = spiderLoc.clone().add(
                                away.getX() * 10,
                                0,
                                away.getZ() * 10
                        );

                        spider.getPathfinder().moveTo(targetLoc);

                        spider.addPotionEffect(
                                new PotionEffect(
                                        PotionEffectType.SPEED,
                                        20,
                                        2,
                                        false,
                                        false
                                )
                        );
                    }
                }
            }

        }.runTaskTimer(plugin, 0L, 10L);
    }

    /**
     * Helper: Iron Golem ngehadap player, jalan ke player, dan kasih bunga poppy + chat
     */
    private void golemGiveFlower(IronGolem golem, Player player) {

        if (golemCooldown.contains(golem.getUniqueId())) return;

        golemCooldown.add(golem.getUniqueId());

        // stop marah
        golem.setTarget(null);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (golem.isValid()) {
                golem.setTarget(null);
            }
        }, 1L);

        // animasi vanilla kasih bunga
        golem.playEffect(EntityEffect.IRON_GOLEM_ROSE);

        // hati
        golem.getWorld().spawnParticle(
                Particle.HEART,
                golem.getLocation().add(0, 2, 0),
                5,
                0.3,
                0.3,
                0.3,
                0.1
        );

        // kasih bunga
        ItemStack flower = new ItemStack(Material.POPPY, 1);

        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(flower);
        } else {
            player.getWorld().dropItemNaturally(
                    player.getLocation(),
                    flower
            );
        }

        Bukkit.broadcast(
                Component.text(
                        "<Iron Golem> ngapain lu colek-colek, nih bunga buat lu"
                )
        );

        // lock ngeliatin player
        new BukkitRunnable() {

            int ticksElapsed = 0;

            @Override
            public void run() {

                if (!golem.isValid()
                        || !player.isOnline()
                        || ticksElapsed >= 60) {

                    cancel();
                    return;
                }

                Location golemEye = golem.getEyeLocation();
                Location playerEye = player.getEyeLocation();

                Vector direction = playerEye.toVector()
                        .subtract(golemEye.toVector());

                golem.teleport(
                        golem.getLocation().setDirection(direction)
                );

                ticksElapsed++;
            }

        }.runTaskTimer(plugin, 0L, 1L);

        // cooldown
        new BukkitRunnable() {
            @Override
            public void run() {
                golemCooldown.remove(golem.getUniqueId());
            }
        }.runTaskLater(plugin, 60L);
    }

    /**
     * Helper: spawn cobweb di sekitar lokasi spider
     */
    private void spawnCobwebAround(Location loc) {
        // Coba pasang cobweb di blok kosong di sekitar spider (radius 1, y same)
        int placed = 0;
        for (int dx = -1; dx <= 1 && placed < 3; dx++) {
            for (int dz = -1; dz <= 1 && placed < 3; dz++) {
                Block b = loc.clone().add(dx, 0, dz).getBlock();
                if (b.getType() == Material.AIR) {
                    b.setType(Material.COBWEB);
                    placed++;
                    // Hapus cobweb setelah 5 detik biar ga permanen
                    new BukkitRunnable() {
                        @Override public void run() {
                            if (b.getType() == Material.COBWEB) b.setType(Material.AIR);
                        }
                    }.runTaskLater(plugin, 100L);
                }
            }
        }
    }

    public void cleanupPlayer(UUID uuid) {
        frozenBoots.remove(uuid);
        voidLevitation.remove(uuid);
        waterPushed.remove(uuid);
        borderBlocked.remove(uuid);
        activeChickenParachute.remove(uuid);
        eatingAnimation.remove(uuid);
        recentCampfireBurn.remove(uuid);
        pendingRespawnAnchor.remove(uuid);
        if(activeLavaTasks.containsKey(uuid)) {
            activeLavaTasks.get(uuid).cancel();
            activeLavaTasks.remove(uuid);
        }
    }

    public Set<UUID> getBorderBlocked() { return borderBlocked; }
}