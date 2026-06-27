package com.msmp.listeners;

import com.msmp.data.CustomMob;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Применяет настройки CustomMob к реальной сущности на сервере
 * и помечает её тегом, чтобы:
 *  - выдавать кастомный опыт при смерти
 *  - в будущем можно было фильтровать кастомных мобов
 */
public class SpawnListener implements Listener {

    public static NamespacedKey customMobKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, "msmp_custom_mob_id");
    }

    private final JavaPlugin plugin;

    public SpawnListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Применить настройки кастомного моба к уже созданной сущности.
     * Вызывается из SpawnerTask после Location.getWorld().spawnEntity(...).
     */
    public void applyCustomMob(LivingEntity entity, CustomMob mob) {
        entity.setCustomName(mob.getDisplayName());
        entity.setCustomNameVisible(true);

        try {
            entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)
                    .setBaseValue(mob.getMaxHealth());
            entity.setHealth(mob.getMaxHealth());
        } catch (Exception ex) {
            plugin.getLogger().warning("Не удалось задать здоровье мобу " + mob.getId() + ": " + ex.getMessage());
        }

        EntityEquipment eq = entity.getEquipment();
        if (eq != null) {
            if (mob.getWeapon() != Material.AIR) eq.setItemInMainHand(new ItemStack(mob.getWeapon()));
            if (mob.getHelmet() != Material.AIR) eq.setHelmet(new ItemStack(mob.getHelmet()));
            if (mob.getChestplate() != Material.AIR) eq.setChestplate(new ItemStack(mob.getChestplate()));
            if (mob.getLeggings() != Material.AIR) eq.setLeggings(new ItemStack(mob.getLeggings()));
            if (mob.getBoots() != Material.AIR) eq.setBoots(new ItemStack(mob.getBoots()));
            // не даём дроп ванильного шмота при смерти, раз он "выдан" — опционально
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
        String mobId = e.getEntity().getPersistentDataContainer().get(customMobKey(plugin), PersistentDataType.STRING);
        if (mobId == null) return;

        // переопределяем дроп опыта согласно настройке кастомного моба
        com.msmp.MSMPPlugin msmpPlugin = (com.msmp.MSMPPlugin) plugin;
        CustomMob mob = msmpPlugin.getMobManager().getMob(mobId);
        if (mob != null) {
            e.setDroppedExp(mob.getExpDrop());
        }
    }
}
