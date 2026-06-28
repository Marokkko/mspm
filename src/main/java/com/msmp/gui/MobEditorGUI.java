package com.msmp.gui;

import com.msmp.data.CustomMob;
import com.msmp.managers.MobManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GUI редактора кастомного моба (45 слотов, 5 рядов).
 *
 * Схема слотов:
 *   10 — имя               12 — здоровье       14 — тип моба
 *   16 — опыт
 *   20 — оружие  21 — шлем  22 — нагрудник  23 — штаны  24 — ботинки
 *   28 — скорость           30 — шанс дропа вании
 *   34 — кастомный дроп (открывает LootEditorGUI)
 *   40 — сохранить и закрыть
 */
public class MobEditorGUI {

    public static final String TITLE_PREFIX = "MSMP: Моб ";

    public static final int SLOT_NAME   = 10;
    public static final int SLOT_HEALTH = 12;
    public static final int SLOT_TYPE   = 14;
    public static final int SLOT_EXP    = 16;
    public static final int SLOT_WEAPON = 20;
    public static final int SLOT_HELMET = 21;
    public static final int SLOT_CHEST  = 22;
    public static final int SLOT_LEGS   = 23;
    public static final int SLOT_BOOTS  = 24;
    public static final int SLOT_SPEED  = 28;
    public static final int SLOT_LOOT   = 30;
    public static final int SLOT_DROPS  = 34;
    public static final int SLOT_SAVE   = 40;

    // Список EntityType для SpawnerEditorGUI
    public static final List<EntityType> MOB_TYPES = Arrays.asList(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER,
            EntityType.ENDERMAN, EntityType.WITCH, EntityType.PILLAGER, EntityType.VINDICATOR,
            EntityType.HUSK, EntityType.STRAY, EntityType.DROWNED, EntityType.BLAZE,
            EntityType.WITHER_SKELETON, EntityType.ZOMBIFIED_PIGLIN, EntityType.PIGLIN,
            EntityType.RAVAGER, EntityType.VEX, EntityType.GUARDIAN, EntityType.PHANTOM,
            EntityType.SILVERFISH, EntityType.CAVE_SPIDER
    );

    private final MobManager mobManager;

    public MobEditorGUI(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    public void open(Player player, CustomMob mob) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE_PREFIX + mob.getId());

        inv.setItem(SLOT_NAME, named(Material.NAME_TAG,
                "§eИмя: §f" + mob.getDisplayName(),
                "§7Нажмите для ввода нового имени в чат"));

        inv.setItem(SLOT_HEALTH, named(Material.GOLDEN_APPLE,
                "§cЗдоровье: §f" + mob.getMaxHealth(),
                "§7ЛКМ: +1  ПКМ: -1  Shift: ±10"));

        inv.setItem(SLOT_TYPE, named(Material.SPAWNER,
                "§bТип моба: §f" + mob.getEntityType().name(),
                "§7Перетащи §fяйцо спавна §7сюда — установит тип.",
                "§7Shift+ЛКМ — ввести тип в чат вручную.",
                "§7ПКМ — сброс к ZOMBIE."));

        inv.setItem(SLOT_EXP, named(Material.EXPERIENCE_BOTTLE,
                "§aОпыт за убийство: §f" + mob.getExpDrop(),
                "§7ЛКМ: +1  ПКМ: -1  Shift: ±10"));

        inv.setItem(SLOT_WEAPON, equipItem(mob.getWeapon(), "Оружие"));
        inv.setItem(SLOT_HELMET, equipItem(mob.getHelmet(), "Шлем"));
        inv.setItem(SLOT_CHEST,  equipItem(mob.getChestplate(), "Нагрудник"));
        inv.setItem(SLOT_LEGS,   equipItem(mob.getLeggings(), "Штаны"));
        inv.setItem(SLOT_BOOTS,  equipItem(mob.getBoots(), "Ботинки"));

        String speedLabel = mob.getMoveSpeed() < 0.15 ? "§8медленный"
                : mob.getMoveSpeed() < 0.26 ? "§aобычный"
                : mob.getMoveSpeed() < 0.4  ? "§eбыстрый" : "§cочень быстрый";
        inv.setItem(SLOT_SPEED, named(Material.SUGAR,
                String.format("§dСкорость: §f%.2f §8(%s§8)", mob.getMoveSpeed(), speedLabel),
                "§7ЛКМ: +0.01  ПКМ: -0.01  Shift: ±0.05",
                "§8Зомби: 0.23 | Скелет: 0.25 | Крипер: 0.25"));

        String lootBar = buildBar(mob.getLootDropChance(), 100, 10);
        inv.setItem(SLOT_LOOT, named(Material.CHEST,
                "§6Шанс ванильного дропа: §f" + mob.getLootDropChance() + "%",
                "§7" + lootBar,
                "§7ЛКМ: +5%  ПКМ: -5%  Shift: ±25%",
                "§80% — ничего не дропает от ванили"));

        int dropCount = mob.getCustomDrops().size();
        inv.setItem(SLOT_DROPS, named(Material.HOPPER,
                "§e§lКастомный дроп §7[" + dropCount + " шт.]",
                "§7Нажми, чтобы открыть редактор дропа.",
                "§7Сейчас настроено: §f" + dropCount + " §7предметов."));

        inv.setItem(SLOT_SAVE, named(Material.LIME_DYE,
                "§a§lСохранить и закрыть",
                "§7Изменения сохраняются автоматически"));

        player.openInventory(inv);
    }

    private String buildBar(int value, int max, int length) {
        int filled = (int) Math.round((double) value / max * length);
        return "§a" + "█".repeat(Math.max(0, filled)) + "§8" + "█".repeat(Math.max(0, length - filled));
    }

    private ItemStack equipItem(Material current, String label) {
        boolean empty = current == null || current == Material.AIR;
        Material display = empty ? Material.BARRIER : current;
        List<String> lore = new ArrayList<>();
        lore.add("§7Перетащи предмет §fсюда§7 чтобы назначить.");
        lore.add("§7Или Shift+клик из инвентаря.");
        lore.add("§7ПКМ — очистить слот.");
        lore.add(empty ? "§7Сейчас: §fничего" : "§7Сейчас: §f" + current.name());
        return named(display, "§d" + label, lore.toArray(new String[0]));
    }

    private ItemStack named(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    public MobManager getMobManager() { return mobManager; }
}
