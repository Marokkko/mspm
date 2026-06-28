package com.msmp.listeners;

import com.msmp.MSMPPlugin;
import com.msmp.data.LevelLocate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLevelChangeEvent;

/**
 * Слушает PlayerLevelChangeEvent.
 * Если у игрока inDungeon = true и для его нового уровня
 * зарегистрирована точка телепорта — телепортируем его туда.
 */
public class LevelUpListener implements Listener {

    private final MSMPPlugin plugin;

    public LevelUpListener(MSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelChange(PlayerLevelChangeEvent e) {
        Player player = e.getPlayer();
        int newLevel  = e.getNewLevel();

        // Проверяем флаг inDungeon
        if (!plugin.getLocateManager().isInDungeon(player.getUniqueId())) return;

        // Ищем точку для нового уровня
        LevelLocate loc = plugin.getLocateManager().getExactLocate(newLevel);
        if (loc == null) return;

        // Находим мир
        World world = Bukkit.getWorld(loc.getWorldName());
        if (world == null) {
            plugin.getLogger().warning("Мир '" + loc.getWorldName()
                    + "' не найден для locate lvl=" + newLevel);
            return;
        }

        Location dest = new Location(world, loc.getX(), loc.getY(), loc.getZ(),
                player.getLocation().getYaw(), player.getLocation().getPitch());

        // Телепорт в главный поток (PlayerLevelChangeEvent уже в нём)
        player.teleport(dest);
        player.sendMessage("§6[MSMP] §eДостигнут уровень §f" + newLevel
                + "§e! Телепортация: §f"
                + loc.getWorldName() + " ("
                + (int)loc.getX() + ", "
                + (int)loc.getY() + ", "
                + (int)loc.getZ() + ")");
    }
}
