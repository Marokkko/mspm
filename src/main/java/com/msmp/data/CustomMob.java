package com.msmp.data;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель кастомного моба.
 * Хранится в mobs.yml
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
    private double moveSpeed;
    private int lootDropChance;
    // Кастомный дроп: список пар (material, chance%)
    private final List<LootEntry> customDrops;

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
        this.customDrops = new ArrayList<>();
    }

    // ---- Вложенный класс записи дропа ----
    public static class LootEntry {
        private Material material;
        private int minAmount;
        private int maxAmount;
        private int chance; // 1–100

        public LootEntry(Material material, int minAmount, int maxAmount, int chance) {
            this.material  = material;
            this.minAmount = Math.max(1, minAmount);
            this.maxAmount = Math.max(this.minAmount, maxAmount);
            this.chance    = Math.max(1, Math.min(100, chance));
        }

        public Material getMaterial()  { return material; }
        public int getMinAmount()      { return minAmount; }
        public int getMaxAmount()      { return maxAmount; }
        public int getChance()         { return chance; }

        public void setChance(int c)      { this.chance    = Math.max(1, Math.min(100, c)); }
        public void setMinAmount(int min) { this.minAmount = Math.max(1, min); }
        public void setMaxAmount(int max) { this.maxAmount = Math.max(minAmount, max); }
    }

    // ---- Геттеры / Сеттеры ----

    public String getId()           { return id; }

    public String getDisplayName()              { return displayName; }
    public void setDisplayName(String n)        { this.displayName = n; }

    public EntityType getEntityType()           { return entityType; }
    public void setEntityType(EntityType t)     { this.entityType = t; }

    public double getMaxHealth()                { return maxHealth; }
    public void setMaxHealth(double v)          { this.maxHealth = Math.max(1, v); }

    public Material getWeapon()                 { return weapon; }
    public void setWeapon(Material m)           { this.weapon = m; }

    public Material getHelmet()                 { return helmet; }
    public void setHelmet(Material m)           { this.helmet = m; }

    public Material getChestplate()             { return chestplate; }
    public void setChestplate(Material m)       { this.chestplate = m; }

    public Material getLeggings()               { return leggings; }
    public void setLeggings(Material m)         { this.leggings = m; }

    public Material getBoots()                  { return boots; }
    public void setBoots(Material m)            { this.boots = m; }

    public int getExpDrop()                     { return expDrop; }
    public void setExpDrop(int v)               { this.expDrop = Math.max(0, v); }

    public double getMoveSpeed()                { return moveSpeed; }
    public void setMoveSpeed(double v)          { this.moveSpeed = Math.max(0.05, Math.min(1.0, v)); }

    public int getLootDropChance()              { return lootDropChance; }
    public void setLootDropChance(int v)        { this.lootDropChance = Math.max(0, Math.min(100, v)); }

    public List<LootEntry> getCustomDrops()     { return customDrops; }

    public void addCustomDrop(Material mat, int min, int max, int chance) {
        customDrops.add(new LootEntry(mat, min, max, chance));
    }

    public void removeCustomDrop(int index) {
        if (index >= 0 && index < customDrops.size()) customDrops.remove(index);
    }
}
