package com.msmp.gui;

import com.msmp.data.CustomMob;
import com.msmp.managers.MobManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GUI кастомного дропа моба.
 *
 * Инвентарь 54 (6 рядов):
 *  Ряды 0-3 (слоты 0–35): до 36 слотов для дроп-предметов.
 *    Каждый занятый слот показывает иконку предмета + лор с шансом/количеством.
 *    Клик ЛКМ — цикличный +5% шанса, ПКМ — −5%, Shift — ±25%.
 *    Средняя кнопка / Q-дроп — удалить запись.
 *  Слот 45: добавить предмет (перетащи сюда предмет)
 *  Слот 47: изменить minAmount выбранного (← выбери слот сначала)
 *  Слот 51: изменить maxAmount выбранного
 *  Слот 49: назад (в MobEditorGUI)
 *  Слот 53: сохранить и закрыть
 */
public class LootEditorGUI {

    public static final String TITLE_PREFIX = "MSMP: Дроп ";

    // Служебные слоты (нижний ряд + ряд перед ним)
    public static final int SLOT_ADD    = 45; // перетащи предмет
    public static final int SLOT_BACK   = 49;
    public static final int SLOT_SAVE   = 53;
    // Ряд слотов 36-44 — граница, всегда пустые (разделитель)

    // Максимум 36 записей дропа (слоты 0..35)
    public static final int MAX_DROPS = 36;

    private final MobManager mobManager;

    public LootEditorGUI(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    public void open(Player player, CustomMob mob) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX + mob.getId());

        // Заполняем слоты 0-35 записями дропа
        List<CustomMob.LootEntry> drops = mob.getCustomDrops();
        for (int i = 0; i < Math.min(drops.size(), MAX_DROPS); i++) {
            inv.setItem(i, lootIcon(drops.get(i)));
        }

        // Разделитель (слоты 36-44) — серое стекло
        for (int s = 36; s <= 44; s++) {
            inv.setItem(s, pane());
        }

        // Слот добавления
        inv.setItem(SLOT_ADD, named(Material.LIME_STAINED_GLASS_PANE,
                "§a§l+ Добавить предмет в дроп",
                "§7Перетащи любой предмет §fсюда§7.",
                "§7Он добавится с шансом 100%, кол-во 1.",
                "§7Потом нажимай ЛКМ/ПКМ на нём чтобы",
                "§7изменить шанс, Shift±25%.",
                "§7ПКМ+Shift — удалить запись."));

        inv.setItem(SLOT_BACK, named(Material.ARROW,
                "§e← Назад к мобу",
                "§7Вернуться в редактор моба"));

        inv.setItem(SLOT_SAVE, named(Material.LIME_DYE,
                "§a§lСохранить и закрыть",
                "§7Изменения уже автоматически сохраняются"));

        player.openInventory(inv);
    }

    /** Иконка одной записи дропа */
    public static ItemStack lootIcon(CustomMob.LootEntry entry) {
        ItemStack item = new ItemStack(entry.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName("§f" + entry.getMaterial().name());

        String bar = buildBar(entry.getChance(), 100, 10);
        List<String> lore = new ArrayList<>();
        lore.add("§7Шанс: §e" + entry.getChance() + "% §8" + bar);
        lore.add("§7Количество: §f" + entry.getMinAmount()
                + (entry.getMaxAmount() > entry.getMinAmount() ? "§7–§f" + entry.getMaxAmount() : ""));
        lore.add("");
        lore.add("§7ЛКМ: шанс §a+5%   §7ПКМ: шанс §c-5%");
        lore.add("§7Shift+ЛКМ: §a+25%   §7Shift+ПКМ: §c-25%");
        lore.add("§7ЛКМ+кол-во min — §fсм. ниже");
        lore.add("§cShift+ПКМ — удалить этот дроп");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String buildBar(int value, int max, int length) {
        int filled = (int) Math.round((double) value / max * length);
        return "§a" + "█".repeat(filled) + "§8" + "█".repeat(length - filled);
    }

    private ItemStack pane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
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
