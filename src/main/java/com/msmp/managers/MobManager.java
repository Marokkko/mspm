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
import java.util.List;
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

    public Map<String, CustomMob> getAll() { return mobs; }

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
            String p = id + ".";
            mob.setDisplayName(cfg.getString(p + "displayName", id));
            try { mob.setEntityType(EntityType.valueOf(cfg.getString(p + "entityType", "ZOMBIE"))); }
            catch (IllegalArgumentException ignored) {}
            mob.setMaxHealth(cfg.getDouble(p + "maxHealth", 20.0));
            mob.setExpDrop(cfg.getInt(p + "expDrop", 5));
            mob.setWeapon(matOrAir(cfg.getString(p + "weapon")));
            mob.setHelmet(matOrAir(cfg.getString(p + "helmet")));
            mob.setChestplate(matOrAir(cfg.getString(p + "chestplate")));
            mob.setLeggings(matOrAir(cfg.getString(p + "leggings")));
            mob.setBoots(matOrAir(cfg.getString(p + "boots")));
            mob.setMoveSpeed(cfg.getDouble(p + "moveSpeed", 0.23));
            mob.setLootDropChance(cfg.getInt(p + "lootDropChance", 100));

            // Кастомный дроп
            List<?> drops = cfg.getList(p + "customDrops");
            if (drops != null) {
                for (Object obj : drops) {
                    if (obj instanceof Map<?,?> map) {
                        Material mat = matOrAir((String) map.get("material"));
                        if (mat == Material.AIR) continue;
                        int min    = toInt(map.get("minAmount"), 1);
                        int max    = toInt(map.get("maxAmount"), 1);
                        int chance = toInt(map.get("chance"), 100);
                        mob.addCustomDrop(mat, min, max, chance);
                    }
                }
            }
            mobs.put(id.toLowerCase(), mob);
        }
    }

    private int toInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) { try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {} }
        return def;
    }

    private Material matOrAir(String name) {
        if (name == null) return Material.AIR;
        try { return Material.valueOf(name); }
        catch (IllegalArgumentException e) { return Material.AIR; }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (CustomMob mob : mobs.values()) {
            String p = mob.getId() + ".";
            cfg.set(p + "displayName",   mob.getDisplayName());
            cfg.set(p + "entityType",    mob.getEntityType().name());
            cfg.set(p + "maxHealth",     mob.getMaxHealth());
            cfg.set(p + "expDrop",       mob.getExpDrop());
            cfg.set(p + "weapon",        mob.getWeapon().name());
            cfg.set(p + "helmet",        mob.getHelmet().name());
            cfg.set(p + "chestplate",    mob.getChestplate().name());
            cfg.set(p + "leggings",      mob.getLeggings().name());
            cfg.set(p + "boots",         mob.getBoots().name());
            cfg.set(p + "moveSpeed",     mob.getMoveSpeed());
            cfg.set(p + "lootDropChance",mob.getLootDropChance());

            // Сериализуем кастомный дроп
            List<Map<String, Object>> dropList = new java.util.ArrayList<>();
            for (CustomMob.LootEntry e : mob.getCustomDrops()) {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("material",  e.getMaterial().name());
                m.put("minAmount", e.getMinAmount());
                m.put("maxAmount", e.getMaxAmount());
                m.put("chance",    e.getChance());
                dropList.add(m);
            }
            cfg.set(p + "customDrops", dropList);
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить mobs.yml: " + e.getMessage());
        }
    }
}
