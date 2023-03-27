package ga.pmc.auskip;

import ga.pmc.auskip.Stats;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {
    private Stats stats;
    private Map<UUID, ItemStack[]> playerArmor;

    @Override
    public void onEnable() {
        getLogger().info("Plugin enabled");
        getServer().getPluginManager().registerEvents(this, this);
        stats = new Stats(this);
        playerArmor = new HashMap<>();
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setHealth(20);
        player.setWalkSpeed(0.2f);
        player.setFoodLevel(20);
        player.setLevel(0);
        player.setExp(0.0f);
        stats.updateStats(player);
        stats.startManaRegeneration(player);
        playerArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        stats.updateStats(player);
        playerArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());
    }

    private long lastMoveEventTime = 0;
    private final long MOVE_EVENT_DELAY = 20;

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMoveEventTime >= MOVE_EVENT_DELAY) {
            stats.updateStats(player);
            playerArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());
            lastMoveEventTime = currentTime;

            ItemStack boots = player.getInventory().getBoots();
            if (boots != null && boots.getType() == Material.LEATHER_BOOTS && boots.getEnchantments().containsKey(Enchantment.DIG_SPEED)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20, 0, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0, false, false));

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ItemStack newBoots = player.getInventory().getBoots();
                        if (newBoots == null || newBoots.getType() != Material.LEATHER_BOOTS || !newBoots.getEnchantments().containsKey(Enchantment.DIG_SPEED)) {
                            player.removePotionEffect(PotionEffectType.JUMP);
                            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
                            this.cancel();
                            return;
                        }
                        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20, 0, false, false));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0, false, false));
                    }
                }.runTaskTimer(this, 20L, 20L);
            } else {
                player.removePotionEffect(PotionEffectType.JUMP);
                player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            }
        }
    }


    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            stats.updateStats(player);
            ItemStack[] currentArmor = player.getInventory().getArmorContents();
            ItemStack[] previousArmor = playerArmor.get(player.getUniqueId());
            if (previousArmor == null) {
                playerArmor.put(player.getUniqueId(), currentArmor);
                return;
            }
            boolean changed = false;
            for (int i = 0; i < currentArmor.length; i++) {
                if (!currentArmor[i].isSimilar(previousArmor[i])) {
                    changed = true;
                    break;
                }
            }
            if (changed) {
                stats.updateStats(player);
                stats.updateScoreboard(player);
                playerArmor.put(player.getUniqueId(), currentArmor);
            }
        }
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.STICK && itemInHand.getEnchantmentLevel(Enchantment.MENDING) > 0) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                new BukkitRunnable() {
                    private int traveledDistance = 0;
                    private final int maxDistance = 25;

                    @Override
                    public void run() {
                        Location currentLocation = player.getLocation();
                        currentLocation.add(currentLocation.getDirection().multiply(traveledDistance));

                        Particle particle = Particle.REDSTONE;
                        currentLocation.getWorld().spawnParticle(particle, currentLocation, 5, 0, 0, 0, 1, new Particle.DustOptions(Color.RED, 1));

                        List<Entity> nearbyEntities = (List<Entity>) currentLocation.getWorld().getNearbyEntities(currentLocation, 1, 1, 1);
                        nearbyEntities.remove(player);

                        for (Entity entity : nearbyEntities) {
                            if (entity instanceof LivingEntity) {
                                ((LivingEntity) entity).damage(20);
                                this.cancel();
                                return;
                            }
                        }

                        traveledDistance++;

                        if (traveledDistance >= maxDistance) {
                            this.cancel();
                        }
                    }
                }.runTaskTimer(this, 0L, 1L);
            }
        }
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            stats.updateStats(player);
        }
    }


    public Stats getStats() {
        return stats;
    }
}
