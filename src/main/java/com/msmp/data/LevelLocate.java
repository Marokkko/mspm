package com.msmp.data;

/**
 * Привязка уровня XP → координаты телепорта.
 * Мир берётся из строки worldName (сохраняется в yml).
 */
public class LevelLocate {

    private final int level;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;

    public LevelLocate(int level, String worldName, double x, double y, double z) {
        this.level     = level;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int    getLevel()     { return level; }
    public String getWorldName() { return worldName; }
    public double getX()         { return x; }
    public double getY()         { return y; }
    public double getZ()         { return z; }

    @Override
    public String toString() {
        return "lvl=" + level + " -> " + worldName + " (" + x + ", " + y + ", " + z + ")";
    }
}
