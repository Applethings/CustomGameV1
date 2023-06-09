package ga.pmc.auskip;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.UUID;

public class Stats {

    private final JavaPlugin plugin;
    private final int defaultMana = 100;
    private final HashMap<UUID, Mana> playerMana = new HashMap<>();

    public Stats(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateStats(Player player) {
        int strength = 0;
        float speed = 0.2f;
        int defense = 0;

        ItemStack[] armorContents = player.getInventory().getArmorContents();
        if (armorContents != null) {
            for (ItemStack item : armorContents) {
                if (item != null) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasAttributeModifiers()) {
                        if (meta.getAttributeModifiers(Attribute.GENERIC_ARMOR) != null) {
                            for (AttributeModifier modifier : meta.getAttributeModifiers(Attribute.GENERIC_ARMOR)) {
                                double amount = modifier.getAmount();
                                defense += amount;
                            }
                        }
                        if (meta.getAttributeModifiers(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                            for (AttributeModifier modifier : meta.getAttributeModifiers(Attribute.GENERIC_ATTACK_DAMAGE)) {
                                double amount = modifier.getAmount();
                                strength += amount;
                            }
                        }
                        if (meta.getAttributeModifiers(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                            for (AttributeModifier modifier : meta.getAttributeModifiers(Attribute.GENERIC_MOVEMENT_SPEED)) {
                                double amount = modifier.getAmount();
                                speed += amount;
                            }
                        }
                    }
                }
            }
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.hasItemMeta() && mainHand.getItemMeta().hasAttributeModifiers() && mainHand.getItemMeta().getAttributeModifiers(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            for (AttributeModifier modifier : mainHand.getItemMeta().getAttributeModifiers(Attribute.GENERIC_ATTACK_DAMAGE)) {
                double amount = modifier.getAmount();
                strength += amount;
            }
            if (mainHand.getItemMeta().getAttributeModifiers(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                for (AttributeModifier modifier : mainHand.getItemMeta().getAttributeModifiers(Attribute.GENERIC_MOVEMENT_SPEED)) {
                    double amount = modifier.getAmount();
                    speed += amount;
                }
            }
        }

        int bonusMana = getBonusMana(player);
        Mana maxMana = new Mana(defaultMana + bonusMana);

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("§6Stats", "dummy", "§6Stats");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        Score strengthScore = objective.getScore(ChatColor.RED + "⚔ " + ChatColor.RED + "Strength: " + strength);
        strengthScore.setScore(5);

        Score speedScore = objective.getScore(ChatColor.BLUE + "✦ " + ChatColor.AQUA + "Speed: " + speed);
        speedScore.setScore(4);

        Score manaScore = objective.getScore(ChatColor.LIGHT_PURPLE + "❀ " + ChatColor.LIGHT_PURPLE + "Mana: " + playerMana.getOrDefault(player.getUniqueId(), new Mana(defaultMana)) + "/" + maxMana);
        manaScore.setScore(3);

        Score healthScore = objective.getScore(ChatColor.YELLOW + "❤ " + ChatColor.YELLOW + "Health: " + (int) player.getHealth());
        healthScore.setScore(2);

        Score defenseScore = objective.getScore(ChatColor.GREEN + "✚ " + ChatColor.GREEN + "Defense: " + defense);
        defenseScore.setScore(1);

        player.setScoreboard(scoreboard);
    }

    private int getBonusMana(Player player) {
        int bonusMana = 0;
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        if (mainHandItem != null && mainHandItem.getType() == Material.BLAZE_ROD) {
            ItemMeta meta = mainHandItem.getItemMeta();
            if (meta != null && meta.hasEnchant(Enchantment.ARROW_INFINITE)) {
                bonusMana += 500;
            }
        } else if (mainHandItem != null && mainHandItem.getType() == Material.STICK) {
            ItemMeta meta = mainHandItem.getItemMeta();
            if (meta != null && meta.hasEnchant(Enchantment.MENDING)) {
                bonusMana += 100;
            }
        }
        return bonusMana;
    }




    private static final int MANA_REGEN_TICKS = 10;
    private static final int MANA_REGEN_AMOUNT = 1;

    public void startManaRegeneration(Player player) {
        if (!player.isOnline()) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                Mana maxMana = new Mana(defaultMana + getBonusMana(player));
                Mana currentMana = playerMana.getOrDefault(player.getUniqueId(), new Mana(defaultMana));
                if (currentMana.isGreaterThan(maxMana)) {
                    currentMana = maxMana;
                }
                if (currentMana.isLessThan(maxMana)) {
                    currentMana = currentMana.plus(new Mana(MANA_REGEN_AMOUNT));
                }
                playerMana.put(player.getUniqueId(), currentMana);
                updateScoreboard(player);
            }
        }.runTaskTimer(plugin, MANA_REGEN_TICKS, MANA_REGEN_TICKS);
    }

    public void updateScoreboard(Player player) {
        try {
            updateStats(player);
        } catch (Exception e) {
            plugin.getLogger().severe("Error updating stats for player " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
