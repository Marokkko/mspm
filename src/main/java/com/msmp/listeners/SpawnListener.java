package com.msmp.listeners;

import com.msmp.data.CustomMob;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class SpawnListener implements Listener {

    public static NamespacedKey customMobKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, "msmp_custom_mob_id");
    }

    private final JavaPlugin plugin;
    private final Random rng = new Random();

    public SpawnListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void applyCustomMob(LivingEntity entity, CustomMob mob) {
        entity.setCustomName(mob.getDisplayName());
        entity.setCustomNameVisible(true);

        setAttribute(entity, Attribute.MAX_HEALTH, mob.getMaxHealth());
        entity.setHealth(mob.getMaxHealth());
        setAttribute(entity, Attribute.MOVEMENT_SPEED, mob.getMoveSpeed());

        EntityEquipment eq = entity.getEquipment();
        if (eq != null) {
            if (mob.getWeapon()     != Material.AIR) eq.setItemInMainHand(new ItemStack(mob.getWeapon()));
            if (mob.getHelmet()     != Material.AIR) eq.setHelmet(new ItemStack(mob.getHelmet()));
            if (mob.getChestplate() != Material.AIR) eq.setChestplate(new ItemStack(mob.getChestplate()));
            if (mob.getLeggings()   != Material.AIR) eq.setLeggings(new ItemStack(mob.getLeggings()));
            if (mob.getBoots()      != Material.AIR) eq.setBoots(new ItemStack(mob.getBoots()));
            eq.setItemInMainHandDropChance(0f);
            eq.setHelmetDropChance(0f);
            eq.setChestplateDropChance(0f);
            eq.setLeggingsDropChance(0f);
            eq.setBootsDropChance(0f);
        }

        entity.getPersistentDataContainer().set(customMobKey(plugin), PersistentDataType.STRING, mob.getId());
    }

    private void setAttribute(LivingEntity entity, Attribute attr, double value) {
        try {
            AttributeInstance inst = entity.getAttribute(attr);
            if (inst != null) inst.setBaseValue(value);
        } catch (Exception ex) {
            plugin.getLogger().warning("Не удалось задать " + attr + " мобу: " + ex.getMessage());
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        String mobId = e.getEntity().getPersistentDataContainer()
                .get(customMobKey(plugin), PersistentDataType.STRING);
        if (mobId == null) return;

        com.msmp.MSMPPlugin msmpPlugin = (com.msmp.MSMPPlugin) plugin;
        CustomMob mob = msmpPlugin.getMobManager().getMob(mobId);
        if (mob == null) return;

        // Опыт
        e.setDroppedExp(mob.getExpDrop());

        // Ванильный дроп — по шансу lootDropChance
        if (mob.getLootDropChance() <= 0) {
            e.getDrops().clear();
        } else if (mob.getLootDropChance() < 100) {
            if (rng.nextInt(100) >= mob.getLootDropChance()) {
                e.getDrops().clear();
            }
        }

        // Кастомный дроп
        for (CustomMob.LootEntry entry : mob.getCustomDrops()) {
            if (rng.nextInt(100) < entry.getChance()) {
                int amount = entry.getMinAmount();
                if (entry.getMaxAmount() > entry.getMinAmount()) {
                    amount += rng.nextInt(entry.getMaxAmount() - entry.getMinAmount() + 1);
                }
                e.getDrops().add(new ItemStack(entry.getMaterial(), amount));
            }
        }
    }
}
