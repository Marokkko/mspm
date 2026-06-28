package com.msmp.managers;

import com.msmp.data.LevelLocate;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Управляет:
 *  - картой уровень → точка телепорта (locates.yml)
 *  - состоянием inDungeon каждого игрока (dungeon_states.yml)
 */
public class LocateManager {

    private final JavaPlugin plugin;

    // level -> LevelLocate (отсортированы по level по возрастанию)
    private final TreeMap<Integer, LevelLocate> locates = new TreeMap<>();

    // UUID -> inDungeon
    private final Map<UUID, Boolean> dungeonStates = new HashMap<>();

    private final File locatesFile;
    private final File statesFile;

    public LocateManager(JavaPlugin plugin) {
        this.plugin      = plugin;
        this.locatesFile = new File(plugin.getDataFolder(), "locates.yml");
        this.statesFile  = new File(plugin.getDataFolder(), "dungeon_states.yml");
        loadLocates();
        loadStates();
    }

    // ─────────────────────────────────────────────────────────────
    //  Locates
    // ─────────────────────────────────────────────────────────────

    public void addLocate(LevelLocate loc) {
        locates.put(loc.getLevel(), loc);
        saveLocates();
    }

    public boolean removeLocate(int level) {
        if (locates.remove(level) != null) { saveLocates(); return true; }
        return false;
    }

    public LevelLocate getLocate(int level) {
        return locates.get(level);
    }

    /** Возвращает точку телепорта для текущего уровня игрока (точное совпадение). */
    public LevelLocate getExactLocate(int playerLevel) {
        return locates.get(playerLevel);
    }

    public TreeMap<Integer, LevelLocate> getAllLocates() {
        return locates;
    }

    private void loadLocates() {
        locates.clear();
        if (!locatesFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(locatesFile);
        for (String key : cfg.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                String world = cfg.getString(key + ".world", "world");
                double x = cfg.getDouble(key + ".x");
                double y = cfg.getDouble(key + ".y");
                double z = cfg.getDouble(key + ".z");
                locates.put(level, new LevelLocate(level, world, x, y, z));
            } catch (NumberFormatException ignored) {}
        }
    }

    private void saveLocates() {
        FileConfiguration cfg = new YamlConfiguration();
        for (LevelLocate loc : locates.values()) {
            String path = loc.getLevel() + ".";
            cfg.set(path + "world", loc.getWorldName());
            cfg.set(path + "x",     loc.getX());
            cfg.set(path + "y",     loc.getY());
            cfg.set(path + "z",     loc.getZ());
        }
        save(cfg, locatesFile);
    }

    // ─────────────────────────────────────────────────────────────
    //  Dungeon states
    // ─────────────────────────────────────────────────────────────

    public void setInDungeon(UUID uuid, boolean value) {
        dungeonStates.put(uuid, value);
        saveStates();
    }

    public boolean isInDungeon(UUID uuid) {
        return dungeonStates.getOrDefault(uuid, false);
    }

    public Map<UUID, Boolean> getAllStates() {
        return Collections.unmodifiableMap(dungeonStates);
    }

    private void loadStates() {
        dungeonStates.clear();
        if (!statesFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(statesFile);
        for (String key : cfg.getKeys(false)) {
            try {
                dungeonStates.put(UUID.fromString(key), cfg.getBoolean(key));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void saveStates() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Boolean> entry : dungeonStates.entrySet()) {
            cfg.set(entry.getKey().toString(), entry.getValue());
        }
        save(cfg, statesFile);
    }

    // ─────────────────────────────────────────────────────────────
    private void save(FileConfiguration cfg, File file) {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Не удалось сохранить " + file.getName() + ": " + ex.getMessage());
        }
    }
}
