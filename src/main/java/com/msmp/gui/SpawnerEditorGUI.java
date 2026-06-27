package com.msmp.gui;

import com.msmp.data.CustomSpawner;
import com.msmp.managers.MobManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * GUI-меню редактирования спавнера.
 * Слоты:
 *  10 - цель спавна (ЛКМ -> следующий vanilla тип, ПКМ -> следующий кастомный моб; Shift+ЛКМ сброс на ZOMBIE)
 *  12 - скорость спавна (тики) (ЛКМ -20, ПКМ +20, Shift +- 100)
 *  14 - радиус спавна (ЛКМ +1, ПКМ -1)
 *  16 - максимум мобов рядом (ЛКМ +1, ПКМ -1)
 *  20 - радиус активации по игроку (ЛКМ +4, ПКМ -4)
 *  22 - количество мобов за раз (ЛКМ +1, ПКМ -1)
 *  31 - сохранить и закрыть
 */
public class SpawnerEditorGUI {

    public static final String TITLE_PREFIX = "MSMP: Спавнер ";

    public static final int SLOT_TARGET = 10;
    public static final int SLOT_RATE = 12;
    public static final int SLOT_RADIUS = 14;
    public static final int SLOT_MAX_NEARBY = 16;
    public static final int SLOT_ACTIVATION = 20;
    public static final int SLOT_COUNT = 22;
    public static final int SLOT_SAVE = 31;

    private final MobManager mobManager;

    public SpawnerEditorGUI(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    public void open(Player player, CustomSpawner sp) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE_PREFIX + sp.getId());

        inv.setItem(SLOT_TARGET, named(Material.ZOMBIE_SPAWN_EGG,
                "§eКого спавнить: §f" + sp.getSpawnTarget(),
                "§7ЛКМ: следующий обычный моб Minecraft",
                "§7ПКМ: следующий кастомный моб из /msmp",
                "§7Shift+ЛКМ: сброс на ZOMBIE"));

        inv.setItem(SLOT_RATE, named(Material.CLOCK,
                "§bЧастота спавна: §f" + sp.getSpawnRateTicks() + " тиков (~" + (sp.getSpawnRateTicks() / 20) + " сек)",
                "§7ЛКМ: -20 тиков  ПКМ: +20 тиков  Shift: ±100"));

        inv.setItem(SLOT_RADIUS, named(Material.STICK,
                "§dРадиус спавна: §f" + sp.getSpawnRadius() + " блоков",
                "§7ЛКМ: +1  ПКМ: -1"));

        inv.setItem(SLOT_MAX_NEARBY, named(Material.IRON_BARS,
                "§cМакс. мобов рядом: §f" + sp.getMaxNearbyEntities(),
                "§7Если рядом уже столько мобов — спавнер не сработает",
                "§7ЛКМ: +1  ПКМ: -1"));

        inv.setItem(SLOT_ACTIVATION, named(Material.ENDER_EYE,
                "§aРадиус активации (игрок): §f" + sp.getPlayerActivationRadius(),
                "§7Спавнер работает только если игрок ближе этого радиуса",
                "§7ЛКМ: +4  ПКМ: -4"));

        inv.setItem(SLOT_COUNT, named(Material.EGG,
                "§6Мобов за раз: §f" + sp.getSpawnCount(),
                "§7ЛКМ: +1  ПКМ: -1"));

        inv.setItem(SLOT_SAVE, named(Material.LIME_DYE, "§a§lСохранить и закрыть", "§7Все изменения уже сохраняются автоматически"));

        player.openInventory(inv);
    }

    private ItemStack named(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    public MobManager getMobManager() {
        return mobManager;
    }
}
