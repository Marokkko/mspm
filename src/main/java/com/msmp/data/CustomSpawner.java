package com.msmp.data;

import org.bukkit.Location;

/**
 * Модель кастомного спавнера.
 * spawnTarget — либо имя vanilla EntityType (например "ZOMBIE"),
 * либо id кастомного моба из MobManager (например "custom:my_boss").
 * Чтобы отличать, кастомные id всегда хранятся с префиксом "custom:".
 */
public class CustomSpawner {

    private final String id;
    private Location location;
    private String spawnTarget = "ZOMBIE"; // vanilla type или "custom:<mobId>"
    private int spawnRateTicks = 200;      // раз в сколько тиков пытаться спавнить (20 тиков = 1 сек)
    private int spawnRadius = 4;           // радиус спавна вокруг точки
    private int maxNearbyEntities = 6;     // лимит мобов в радиусе, после которого спавнер не работает
    private int playerActivationRadius = 16; // спавнер активен только если рядом есть игрок
    private int spawnCount = 1;            // сколько мобов спавнить за раз

    public CustomSpawner(String id, Location location) {
        this.id = id;
        this.location = location;
    }

    public String getId() { return id; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public String getSpawnTarget() { return spawnTarget; }
    public void setSpawnTarget(String spawnTarget) { this.spawnTarget = spawnTarget; }

    public int getSpawnRateTicks() { return spawnRateTicks; }
    public void setSpawnRateTicks(int spawnRateTicks) { this.spawnRateTicks = spawnRateTicks; }

    public int getSpawnRadius() { return spawnRadius; }
    public void setSpawnRadius(int spawnRadius) { this.spawnRadius = spawnRadius; }

    public int getMaxNearbyEntities() { return maxNearbyEntities; }
    public void setMaxNearbyEntities(int maxNearbyEntities) { this.maxNearbyEntities = maxNearbyEntities; }

    public int getPlayerActivationRadius() { return playerActivationRadius; }
    public void setPlayerActivationRadius(int playerActivationRadius) { this.playerActivationRadius = playerActivationRadius; }

    public int getSpawnCount() { return spawnCount; }
    public void setSpawnCount(int spawnCount) { this.spawnCount = spawnCount; }

    public boolean isCustomMobTarget() {
        return spawnTarget != null && spawnTarget.startsWith("custom:");
    }

    public String getCustomMobId() {
        return isCustomMobTarget() ? spawnTarget.substring("custom:".length()) : null;
    }
}
