package com.msmp.data;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/**
 * Модель кастомного моба, который настраивается через GUI и хранится в mobs.yml
 */
public class CustomMob {

    private final String id;
    private String displayName;
    private EntityType entityType;
    private double maxHealth;
    private Material weapon;
    private Material helmet;
    private Material chestplate;
    private Material leggings;
    private Material boots;
    private int expDrop;
    // === НОВЫЕ ПОЛЯ ===
    private double moveSpeed;       // скорость (0.1 = стандарт зомби, 0.25 = быстрый)
    private int lootDropChance;     // шанс дропа лута в % (0-100)

    public CustomMob(String id) {
        this.id = id;
        this.displayName = id;
        this.entityType = EntityType.ZOMBIE;
        this.maxHealth = 20.0;
        this.weapon = Material.AIR;
        this.helmet = Material.AIR;
        this.chestplate = Material.AIR;
        this.leggings = Material.AIR;
        this.boots = Material.AIR;
        this.expDrop = 5;
        this.moveSpeed = 0.23;
        this.lootDropChance = 100;
    }

    public String getId() { return id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }

    public double getMaxHealth() { return maxHealth; }
    public void setMaxHealth(double maxHealth) { this.maxHealth = maxHealth; }

    public Material getWeapon() { return weapon; }
    public void setWeapon(Material weapon) { this.weapon = weapon; }

    public Material getHelmet() { return helmet; }
    public void setHelmet(Material helmet) { this.helmet = helmet; }

    public Material getChestplate() { return chestplate; }
    public void setChestplate(Material chestplate) { this.chestplate = chestplate; }

    public Material getLeggings() { return leggings; }
    public void setLeggings(Material leggings) { this.leggings = leggings; }

    public Material getBoots() { return boots; }
    public void setBoots(Material boots) { this.boots = boots; }

    public int getExpDrop() { return expDrop; }
    public void setExpDrop(int expDrop) { this.expDrop = expDrop; }

    public double getMoveSpeed() { return moveSpeed; }
    public void setMoveSpeed(double moveSpeed) { this.moveSpeed = Math.max(0.05, Math.min(1.0, moveSpeed)); }

    public int getLootDropChance() { return lootDropChance; }
    public void setLootDropChance(int lootDropChance) { this.lootDropChance = Math.max(0, Math.min(100, lootDropChance)); }
}
