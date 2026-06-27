package com.msmp.tasks;

import com.msmp.MSMPPlugin;
import com.msmp.data.CustomMob;
import com.msmp.data.CustomSpawner;
import com.msmp.listeners.SpawnListener;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

/**
 * Запускается один раз для каждого CustomSpawner и сам себя переплпнирует
 * с интервалом spawnRateTicks (т.к. интервал у каждого спавнера свой и может
 * меняться через GUI в любой момент).
 */
public class SpawnerTask extends BukkitRunnable {

    private final MSMPPlugin plugin;
    private final String spawnerId;
    private final Random random = new Random();

    public SpawnerTask(MSMPPlugin plugin, String spawnerId) {
        this.plugin = plugin;
        this.spawnerId = spawnerId;
    }

    @Override
    public void run() {
        CustomSpawner sp = plugin.getSpawnerManager().getSpawner(spawnerId);
        if (sp == null) {
            this.cancel();
            return; // спавнер удалён — больше не планируем
        }

        Location loc = sp.getLocation();
        if (loc.getWorld() == null) {
            scheduleNext(sp);
            return;
        }

        // активация только если рядом игрок
        boolean playerNearby = false;
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= (double) sp.getPlayerActivationRadius() * sp.getPlayerActivationRadius()) {
                playerNearby = true;
                break;
            }
        }
        if (!playerNearby) {
            scheduleNext(sp);
            return;
        }

        // проверяем лимит мобов рядом
        List<org.bukkit.entity.Entity> nearby = loc.getWorld().getNearbyEntities(loc, sp.getSpawnRadius(), sp.getSpawnRadius(), sp.getSpawnRadius())
                .stream().filter(en -> en instanceof LivingEntity && !(en instanceof Player)).toList();
        if (nearby.size() >= sp.getMaxNearbyEntities()) {
            scheduleNext(sp);
            return;
        }

        for (int i = 0; i < sp.getSpawnCount(); i++) {
            spawnOne(sp);
        }

        scheduleNext(sp);
    }

    private void spawnOne(CustomSpawner sp) {
        Location loc = sp.getLocation();
        double dx = (random.nextDouble() * 2 - 1) * sp.getSpawnRadius();
        double dz = (random.nextDouble() * 2 - 1) * sp.getSpawnRadius();
        Location spawnAt = loc.clone().add(dx, 0, dz);

        if (sp.isCustomMobTarget()) {
            CustomMob mob = plugin.getMobManager().getMob(sp.getCustomMobId());
            if (mob == null) return;
            LivingEntity entity = (LivingEntity) spawnAt.getWorld().spawnEntity(spawnAt, mob.getEntityType());
            plugin.getSpawnListener().applyCustomMob(entity, mob);
        } else {
            EntityType type;
            try {
                type = EntityType.valueOf(sp.getSpawnTarget());
            } catch (IllegalArgumentException ex) {
                type = EntityType.ZOMBIE;
            }
            spawnAt.getWorld().spawnEntity(spawnAt, type);
        }
    }

    private void scheduleNext(CustomSpawner sp) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> new SpawnerTask(plugin, spawnerId).run(), sp.getSpawnRateTicks());
    }

    /**
     * Запустить таски для всех загруженных спавнеров (вызывать при onEnable).
     */
    public static void startAll(MSMPPlugin plugin) {
        for (String id : plugin.getSpawnerManager().getAll().keySet()) {
            new SpawnerTask(plugin, id).runTask(plugin); // первый запуск — следующий тик
        }
    }
}
