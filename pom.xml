package com.msmp.managers;

import com.msmp.data.CustomMob;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MobManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, CustomMob> mobs = new LinkedHashMap<>();

    public MobManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "mobs.yml");
        load();
    }

    public CustomMob createMob(String id) {
        CustomMob mob = new CustomMob(id);
        mobs.put(id.toLowerCase(), mob);
        save();
        return mob;
    }

    public CustomMob getMob(String id) {
        return mobs.get(id == null ? null : id.toLowerCase());
    }

    public boolean exists(String id) {
        return mobs.containsKey(id == null ? null : id.toLowerCase());
    }

    public Map<String, CustomMob> getAll() {
        return mobs;
    }

    public void delete(String id) {
        mobs.remove(id.toLowerCase());
        save();
    }

    public void load() {
        mobs.clear();
        if (!file.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String id : cfg.getKeys(false)) {
            CustomMob mob = new CustomMob(id);
            String path = id + ".";
            mob.setDisplayName(cfg.getString(path + "displayName", id));
            try {
                mob.setEntityType(EntityType.valueOf(cfg.getString(path + "entityType", "ZOMBIE")));
            } catch (IllegalArgumentException ignored) {}
            mob.setMaxHealth(cfg.getDouble(path + "maxHealth", 20.0));
            mob.setExpDrop(cfg.getInt(path + "expDrop", 5));
            mob.setWeapon(matOrAir(cfg.getString(path + "weapon")));
            mob.setHelmet(matOrAir(cfg.getString(path + "helmet")));
            mob.setChestplate(matOrAir(cfg.getString(path + "chestplate")));
            mob.setLeggings(matOrAir(cfg.getString(path + "leggings")));
            mob.setBoots(matOrAir(cfg.getString(path + "boots")));
            mobs.put(id.toLowerCase(), mob);
        }
    }

    private Material matOrAir(String name) {
        if (name == null) return Material.AIR;
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Material.AIR;
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (CustomMob mob : mobs.values()) {
            String path = mob.getId() + ".";
            cfg.set(path + "displayName", mob.getDisplayName());
            cfg.set(path + "entityType", mob.getEntityType().name());
            cfg.set(path + "maxHealth", mob.getMaxHealth());
            cfg.set(path + "expDrop", mob.getExpDrop());
            cfg.set(path + "weapon", mob.getWeapon().name());
            cfg.set(path + "helmet", mob.getHelmet().name());
            cfg.set(path + "chestplate", mob.getChestplate().name());
            cfg.set(path + "leggings", mob.getLeggings().name());
            cfg.set(path + "boots", mob.getBoots().name());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить mobs.yml: " + e.getMessage());
        }
    }
}
