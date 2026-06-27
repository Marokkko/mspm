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

/**
 * Применяет настройки CustomMob к реальной сущности на сервере
 * и помечает её тегом, чтобы:
 *  - выдавать кастомный опыт при смерти
 *  - применять шанс дропа лута
 */
public class SpawnListener implements Listener {

    public static NamespacedKey customMobKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, "msmp_custom_mob_id");
    }

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public SpawnListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Применить настройки кастомного моба к уже созданной сущности.
     * Вызывается из SpawnerTask после spawnEntity.
     */
    public void applyCustomMob(LivingEntity entity, CustomMob mob) {
        entity.setCustomName(mob.getDisplayName());
        entity.setCustomNameVisible(true);

        // Здоровье
        try {
            AttributeInstance healthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
            if (healthAttr != null) {
                healthAttr.setBaseValue(mob.getMaxHealth());
                entity.setHealth(mob.getMaxHealth());
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Не удалось задать здоровье мобу " + mob.getId() + ": " + ex.getMessage());
        }

        // Скорость передвижения
        try {
            AttributeInstance speedAttr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.setBaseValue(mob.getMoveSpeed());
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Не удалось задать скорость мобу " + mob.getId() + ": " + ex.getMessage());
        }

        // Экипировка
        EntityEquipment eq = entity.getEquipment();
        if (eq != null) {
            if (mob.getWeapon()     != Material.AIR) eq.setItemInMainHand(new ItemStack(mob.getWeapon()));
            if (mob.getHelmet()     != Material.AIR) eq.setHelmet(new ItemStack(mob.getHelmet()));
            if (mob.getChestplate() != Material.AIR) eq.setChestplate(new ItemStack(mob.getChestplate()));
            if (mob.getLeggings()   != Material.AIR) eq.setLeggings(new ItemStack(mob.getLeggings()));
            if (mob.getBoots()      != Material.AIR) eq.setBoots(new ItemStack(mob.getBoots()));
            // Нулевой шанс дропа экипировки (контролируется lootDropChance)
            eq.setItemInMainHandDropChance(0f);
            eq.setHelmetDropChance(0f);
            eq.setChestplateDropChance(0f);
            eq.setLeggingsDropChance(0f);
            eq.setBootsDropChance(0f);
        }

        entity.getPersistentDataContainer().set(customMobKey(plugin), PersistentDataType.STRING, mob.getId());
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        String mobId = e.getEntity().getPersistentDataContainer()
                .get(customMobKey(plugin), PersistentDataType.STRING);
        if (mobId == null) return;

        com.msmp.MSMPPlugin msmpPlugin = (com.msmp.MSMPPlugin) plugin;
        CustomMob mob = msmpPlugin.getMobManager().getMob(mobId);
        if (mob == null) return;

        // Кастомный опыт
        e.setDroppedExp(mob.getExpDrop());

        // Шанс дропа ванильного лута (кости, гнилая плоть и т.д.)
        int chance = mob.getLootDropChance();
        if (chance <= 0) {
            e.getDrops().clear();
        } else if (chance < 100) {
            if (random.nextInt(100) >= chance) {
                e.getDrops().clear();
            }
        }
        // chance == 100 → ничего не делаем, дроп остаётся
    }
}
