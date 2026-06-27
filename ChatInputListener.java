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
 * Слоты:
 *  10 - имя (клик -> ввод в чат)
 *  12 - здоровье (ЛКМ +1, ПКМ -1, Shift+ЛКМ +10)
 *  14 - тип моба (ЛКМ -> следующий тип, ПКМ -> предыдущий)
 *  16 - опыт за убийство (ЛКМ +1, ПКМ -1, Shift +10)
 *  20 - оружие (положите предмет в слот / клик ПКМ чтобы очистить)
 *  21 - шлем
 *  22 - нагрудник
 *  23 - штаны
 *  24 - ботинки
 *  31 - сохранить и закрыть
 */
public class MobEditorGUI {

    public static final String TITLE_PREFIX = "MSMP: Моб ";

    public static final int SLOT_NAME = 10;
    public static final int SLOT_HEALTH = 12;
    public static final int SLOT_TYPE = 14;
    public static final int SLOT_EXP = 16;
    public static final int SLOT_WEAPON = 20;
    public static final int SLOT_HELMET = 21;
    public static final int SLOT_CHEST = 22;
    public static final int SLOT_LEGS = 23;
    public static final int SLOT_BOOTS = 24;
    public static final int SLOT_SAVE = 31;

    // ограниченный, безопасный список типов мобов для циклического переключения
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
        Inventory inv = Bukkit.createInventory(null, 36, TITLE_PREFIX + mob.getId());

        inv.setItem(SLOT_NAME, named(Material.NAME_TAG,
                "§eИмя: §f" + mob.getDisplayName(),
                "§7Нажмите, чтобы ввести новое имя в чат"));

        inv.setItem(SLOT_HEALTH, named(Material.GOLDEN_APPLE,
                "§cЗдоровье: §f" + mob.getMaxHealth(),
                "§7ЛКМ: +1  ПКМ: -1  Shift+ЛКМ: +10  Shift+ПКМ: -10"));

        inv.setItem(SLOT_TYPE, named(Material.SPAWNER,
                "§bТип моба: §f" + mob.getEntityType().name(),
                "§7ЛКМ: следующий тип  ПКМ: предыдущий тип"));

        inv.setItem(SLOT_EXP, named(Material.EXPERIENCE_BOTTLE,
                "§aОпыт за убийство: §f" + mob.getExpDrop(),
                "§7ЛКМ: +1  ПКМ: -1  Shift+ЛКМ: +10  Shift+ПКМ: -10"));

        inv.setItem(SLOT_WEAPON, equipItem(mob.getWeapon(), "Оружие"));
        inv.setItem(SLOT_HELMET, equipItem(mob.getHelmet(), "Шлем"));
        inv.setItem(SLOT_CHEST, equipItem(mob.getChestplate(), "Нагрудник"));
        inv.setItem(SLOT_LEGS, equipItem(mob.getLeggings(), "Штаны"));
        inv.setItem(SLOT_BOOTS, equipItem(mob.getBoots(), "Ботинки"));

        inv.setItem(SLOT_SAVE, named(Material.LIME_DYE, "§a§lСохранить и закрыть", "§7Все изменения уже сохраняются автоматически"));

        player.openInventory(inv);
    }

    private ItemStack equipItem(Material current, String label) {
        Material display = current == Material.AIR ? Material.BARRIER : current;
        List<String> lore = new ArrayList<>();
        lore.add("§7Положите предмет сюда, чтобы назначить");
        lore.add("§7ПКМ по слоту — очистить");
        lore.add(current == Material.AIR ? "§7Сейчас: §fничего" : "§7Сейчас: §f" + current.name());
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
