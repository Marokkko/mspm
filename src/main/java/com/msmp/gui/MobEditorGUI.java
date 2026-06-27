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
 * GUI-меню редактирования кастомного моба.
 *
 * Слоты (инвентарь 45 = 5 рядов):
 *  10 - имя (клик -> ввод в чат)
 *  12 - здоровье (ЛКМ +1, ПКМ -1, Shift+ЛКМ +10)
 *  14 - тип моба (перетащи яйцо спавна сюда; для кастомного моба — ввод имени в чат)
 *  16 - опыт за убийство (ЛКМ +1, ПКМ -1, Shift +10)
 *  20 - оружие     (перетащи предмет в слот)
 *  21 - шлем       (перетащи броню в слот)
 *  22 - нагрудник  (перетащи броню в слот)
 *  23 - штаны      (перетащи броню в слот)
 *  24 - ботинки    (перетащи броню в слот)
 *  28 - скорость передвижения (ЛКМ +0.01, ПКМ -0.01, Shift ±0.05)
 *  30 - шанс дропа лута % (ЛКМ +5, ПКМ -5, Shift ±25)
 *  40 - сохранить и закрыть
 */
public class MobEditorGUI {

    public static final String TITLE_PREFIX = "MSMP: Моб ";

    public static final int SLOT_NAME       = 10;
    public static final int SLOT_HEALTH     = 12;
    public static final int SLOT_TYPE       = 14;
    public static final int SLOT_EXP        = 16;
    public static final int SLOT_WEAPON     = 20;
    public static final int SLOT_HELMET     = 21;
    public static final int SLOT_CHEST      = 22;
    public static final int SLOT_LEGS       = 23;
    public static final int SLOT_BOOTS      = 24;
    public static final int SLOT_SPEED      = 28;
    public static final int SLOT_LOOT       = 30;
    public static final int SLOT_SAVE       = 40;

    // Ограниченный список типов для циклического переключения (используется в SpawnerEditorGUI)
    public static final List<EntityType> MOB_TYPES = Arrays.asList(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER,
            EntityType.ENDERMAN, EntityType.WITCH, EntityType.PILLAGER, EntityType.VINDICATOR,
            EntityType.HUSK, EntityType.STRAY, EntityType.DROWNED, EntityType.BLAZE,
            EntityType.WITHER_SKELETON, EntityType.ZOMBIFIED_PIGLIN, EntityType.PIGLIN,
            EntityType.RAVAGER, EntityType.VEX, EntityType.GUARDIAN, EntityType.PHANTOM,
            EntityType.SILVERFISH, EntityType.CAVE_SPIDER
    );

    // Все слоты, в которых разрешено «класть» предметы (экипировка + тип)
    public static final List<Integer> EQUIP_SLOTS = List.of(
            SLOT_WEAPON, SLOT_HELMET, SLOT_CHEST, SLOT_LEGS, SLOT_BOOTS, SLOT_TYPE
    );

    private final MobManager mobManager;

    public MobEditorGUI(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    public void open(Player player, CustomMob mob) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE_PREFIX + mob.getId());

        inv.setItem(SLOT_NAME, named(Material.NAME_TAG,
                "§eИмя: §f" + mob.getDisplayName(),
                "§7Нажмите, чтобы ввести новое имя в чат"));

        inv.setItem(SLOT_HEALTH, named(Material.GOLDEN_APPLE,
                "§cЗдоровье: §f" + mob.getMaxHealth(),
                "§7ЛКМ: +1  ПКМ: -1  Shift+ЛКМ: +10  Shift+ПКМ: -10"));

        // Слот типа моба — перетаскивание яйца спавна
        inv.setItem(SLOT_TYPE, named(Material.SPAWNER,
                "§bТип моба: §f" + mob.getEntityType().name(),
                "§7Перетащи §fяйцо спавна §7сюда, чтобы сменить тип.",
                "§7Для кастомного моба: введи имя в чат (Shift+ЛКМ).",
                "§7ПКМ — сбросить к ZOMBIE"));

        inv.setItem(SLOT_EXP, named(Material.EXPERIENCE_BOTTLE,
                "§aОпыт за убийство: §f" + mob.getExpDrop(),
                "§7ЛКМ: +1  ПКМ: -1  Shift+ЛКМ: +10  Shift+ПКМ: -10"));

        inv.setItem(SLOT_WEAPON, equipItem(mob.getWeapon(), "Оружие", false));
        inv.setItem(SLOT_HELMET, equipItem(mob.getHelmet(), "Шлем", true));
        inv.setItem(SLOT_CHEST, equipItem(mob.getChestplate(), "Нагрудник", true));
        inv.setItem(SLOT_LEGS, equipItem(mob.getLeggings(), "Штаны", true));
        inv.setItem(SLOT_BOOTS, equipItem(mob.getBoots(), "Ботинки", true));

        String speedDisplay = String.format("%.2f", mob.getMoveSpeed());
        String speedLabel = mob.getMoveSpeed() < 0.15 ? "§8(медленный)" :
                            mob.getMoveSpeed() < 0.25 ? "§a(обычный)" :
                            mob.getMoveSpeed() < 0.4  ? "§e(быстрый)" : "§c(очень быстрый)";
        inv.setItem(SLOT_SPEED, named(Material.SUGAR,
                "§dСкорость: §f" + speedDisplay + " " + speedLabel,
                "§7ЛКМ: +0.01  ПКМ: -0.01",
                "§7Shift+ЛКМ: +0.05  Shift+ПКМ: -0.05",
                "§8Стандарт зомби: 0.23, скелет: 0.25"));

        String lootBar = buildBar(mob.getLootDropChance(), 100, 10);
        inv.setItem(SLOT_LOOT, named(Material.CHEST,
                "§6Шанс дропа лута: §f" + mob.getLootDropChance() + "%",
                "§7" + lootBar,
                "§7ЛКМ: +5%  ПКМ: -5%  Shift: ±25%",
                "§8При 0% — моб ничего не дропает"));

        inv.setItem(SLOT_SAVE, named(Material.LIME_DYE,
                "§a§lСохранить и закрыть",
                "§7Все изменения уже сохраняются автоматически"));

        player.openInventory(inv);
    }

    /** Простая полоска прогресса */
    private String buildBar(int value, int max, int length) {
        int filled = (int) Math.round((double) value / max * length);
        return "§a" + "█".repeat(filled) + "§8" + "█".repeat(length - filled);
    }

    private ItemStack equipItem(Material current, String label, boolean isArmor) {
        Material display = (current == null || current == Material.AIR) ? Material.BARRIER : current;
        List<String> lore = new ArrayList<>();
        lore.add("§7Перетащи предмет сюда, чтобы назначить");
        lore.add("§7ПКМ по слоту — очистить");
        lore.add((current == null || current == Material.AIR)
                ? "§7Сейчас: §fничего"
                : "§7Сейчас: §f" + current.name());
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

    public MobManager getMobManager() {
        return mobManager;
    }
}
