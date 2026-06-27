package com.msmp.managers;

import com.msmp.data.CustomSpawner;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SpawnerManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, CustomSpawner> spawners = new LinkedHashMap<>();

    public SpawnerManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spawners.yml");
        load();
    }

    public CustomSpawner createSpawner(String id, Location loc) {
        CustomSpawner sp = new CustomSpawner(id, loc);
        spawners.put(id.toLowerCase(), sp);
        save();
        return sp;
    }

    public CustomSpawner getSpawner(String id) {
        return spawners.get(id == null ? null : id.toLowerCase());
    }

    public boolean exists(String id) {
        return spawners.containsKey(id == null ? null : id.toLowerCase());
    }

    public Map<String, CustomSpawner> getAll() {
        return spawners;
    }

    public void delete(String id) {
        spawners.remove(id.toLowerCase());
        save();
    }

    public void load() {
        spawners.clear();
        if (!file.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String id : cfg.getKeys(false)) {
            String path = id + ".";
            String worldName = cfg.getString(path + "world");
            World world = worldName != null ? plugin.getServer().getWorld(worldName) : null;
            if (world == null) continue; // мир ещё не загружен/удалён — пропускаем
            double x = cfg.getDouble(path + "x");
            double y = cfg.getDouble(path + "y");
            double z = cfg.getDouble(path + "z");
            Location loc = new Location(world, x, y, z);

            CustomSpawner sp = new CustomSpawner(id, loc);
            sp.setSpawnTarget(cfg.getString(path + "spawnTarget", "ZOMBIE"));
            sp.setSpawnRateTicks(cfg.getInt(path + "spawnRateTicks", 200));
            sp.setSpawnRadius(cfg.getInt(path + "spawnRadius", 4));
            sp.setMaxNearbyEntities(cfg.getInt(path + "maxNearbyEntities", 6));
            sp.setPlayerActivationRadius(cfg.getInt(path + "playerActivationRadius", 16));
            sp.setSpawnCount(cfg.getInt(path + "spawnCount", 1));
            spawners.put(id.toLowerCase(), sp);
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (CustomSpawner sp : spawners.values()) {
            String path = sp.getId() + ".";
            Location loc = sp.getLocation();
            cfg.set(path + "world", loc.getWorld().getName());
            cfg.set(path + "x", loc.getX());
            cfg.set(path + "y", loc.getY());
            cfg.set(path + "z", loc.getZ());
            cfg.set(path + "spawnTarget", sp.getSpawnTarget());
            cfg.set(path + "spawnRateTicks", sp.getSpawnRateTicks());
            cfg.set(path + "spawnRadius", sp.getSpawnRadius());
            cfg.set(path + "maxNearbyEntities", sp.getMaxNearbyEntities());
            cfg.set(path + "playerActivationRadius", sp.getPlayerActivationRadius());
            cfg.set(path + "spawnCount", sp.getSpawnCount());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить spawners.yml: " + e.getMessage());
        }
    }
}
