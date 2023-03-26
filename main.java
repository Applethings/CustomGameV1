package ga.pmc.auskip;

import ga.pmc.auskip.Stats;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
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
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            stats.updateStats(player);
        }
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.BLAZE_ROD && item.hasItemMeta() && item.getItemMeta().hasEnchant(Enchantment.ARROW_INFINITE)) {

            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Location location = player.getLocation();
                Vector direction = location.getDirection().normalize();
                WitherSkull skull = location.getWorld().spawn(location.add(direction), WitherSkull.class);
                skull.setShooter(player);
                skull.setCharged(true);
                skull.setGravity(false);
                skull.setVelocity(direction.multiply(2));

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Entity target = null;
                        double distance = 25;
                        for (Entity entity : skull.getNearbyEntities(distance, distance, distance)) {
                            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                                double entityDistance = entity.getLocation().distanceSquared(skull.getLocation());
                                if (entityDistance < distance) {
                                    target = entity;
                                    distance = entityDistance;
                                }
                            }
                        }

                        if (target != null) {
                            Vector targetDirection = target.getLocation().subtract(skull.getLocation()).toVector().normalize();
                            skull.setVelocity(targetDirection.multiply(2));
                        }
                    }
                }.runTaskTimer(this, 0, 1);
            }
        }
    }




    public Stats getStats() {
        return stats;
    }
}
