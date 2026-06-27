package com.msmp.listeners;

import com.msmp.data.CustomMob;
import com.msmp.data.CustomSpawner;
import com.msmp.gui.MobEditorGUI;
import com.msmp.gui.SpawnerEditorGUI;
import com.msmp.managers.MobManager;
import com.msmp.managers.SpawnerManager;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;

import java.util.List;
import java.util.Set;

public class GUIListener implements Listener {

    private final MobManager mobManager;
    private final SpawnerManager spawnerManager;
    private final MobEditorGUI mobEditorGUI;
    private final SpawnerEditorGUI spawnerEditorGUI;
    private final ChatInputListener chatInputListener;

    // Разрешённые материалы для слотов брони
    private static final Set<Material> ARMOR_OK = Set.of(
            Material.LEATHER_HELMET,    Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS,  Material.LEATHER_BOOTS,
            Material.IRON_HELMET,       Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS,     Material.IRON_BOOTS,
            Material.GOLDEN_HELMET,     Material.GOLDEN_CHESTPLATE,
            Material.GOLDEN_LEGGINGS,   Material.GOLDEN_BOOTS,
            Material.CHAINMAIL_HELMET,  Material.CHAINMAIL_CHESTPLATE,
            Material.CHAINMAIL_LEGGINGS,Material.CHAINMAIL_BOOTS,
            Material.DIAMOND_HELMET,    Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS,  Material.DIAMOND_BOOTS,
            Material.NETHERITE_HELMET,  Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS,Material.NETHERITE_BOOTS,
            Material.TURTLE_HELMET
    );

    // Слоты брони/оружия (в них разрешено класть предметы)
    private static final Set<Integer> EQUIP_SLOTS = Set.of(
            MobEditorGUI.SLOT_WEAPON,
            MobEditorGUI.SLOT_HELMET,
            MobEditorGUI.SLOT_CHEST,
            MobEditorGUI.SLOT_LEGS,
            MobEditorGUI.SLOT_BOOTS
    );

    public GUIListener(MobManager mobManager, SpawnerManager spawnerManager,
                       MobEditorGUI mobEditorGUI, SpawnerEditorGUI spawnerEditorGUI,
                       ChatInputListener chatInputListener) {
        this.mobManager = mobManager;
        this.spawnerManager = spawnerManager;
        this.mobEditorGUI = mobEditorGUI;
        this.spawnerEditorGUI = spawnerEditorGUI;
        this.chatInputListener = chatInputListener;
    }

    // =========================================================
    //  Блокируем DRAG (перетаскивание с делением стака) во весь GUI
    // =========================================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (!title.startsWith(MobEditorGUI.TITLE_PREFIX) && !title.startsWith(SpawnerEditorGUI.TITLE_PREFIX)) return;

        // Если хотя бы один из слотов drag'а — в верхнем инвентаре GUI — отменяем
        int topSize = e.getView().getTopInventory().getSize();
        for (int slot : e.getRawSlots()) {
            if (slot < topSize) {
                e.setCancelled(true);
                return;
            }
        }
    }

    // =========================================================
    //  Основной обработчик кликов
    // =========================================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();

        if (title.startsWith(MobEditorGUI.TITLE_PREFIX)) {
            handleMobClick(e, title.substring(MobEditorGUI.TITLE_PREFIX.length()));
        } else if (title.startsWith(SpawnerEditorGUI.TITLE_PREFIX)) {
            handleSpawnerClick(e, title.substring(SpawnerEditorGUI.TITLE_PREFIX.length()));
        }
    }

    // =========================================================
    //  MobEditor
    // =========================================================
    private void handleMobClick(InventoryClickEvent e, String mobId) {
        Player player = (Player) e.getWhoClicked();
        CustomMob mob = mobManager.getMob(mobId);
        if (mob == null) { player.closeInventory(); return; }

        int slot      = e.getRawSlot();
        int topSize   = e.getView().getTopInventory().getSize();
        boolean shift = e.isShiftClick();
        boolean right = e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT;

        // --- Клик по нижнему инвентарю игрока ---
        if (slot >= topSize) {
            // Shift+клик из инвентаря игрока -> пытаемся определить, куда положить предмет
            if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);
                ItemStack item = e.getCurrentItem();
                if (item == null || item.getType() == Material.AIR) return;
                tryAutoEquip(player, mob, item);
            } else {
                e.setCancelled(true);
            }
            return;
        }

        // --- Слоты экипировки: разрешаем класть предмет курсором ---
        if (EQUIP_SLOTS.contains(slot)) {
            ItemStack cursor = e.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR && !right) {
                // Кладём предмет в слот
                Material mat = cursor.getType();
                if (applyEquip(mob, slot, mat, player)) {
                    schedule(player, mob); // переоткрываем после тика
                } else {
                    e.setCancelled(true);
                }
                return;
            }
            if (right) {
                e.setCancelled(true);
                applyEquip(mob, slot, Material.AIR, player);
                mobEditorGUI.open(player, mob);
                return;
            }
            e.setCancelled(true);
            return;
        }

        // --- Слот типа моба (SLOT_TYPE) ---
        if (slot == MobEditorGUI.SLOT_TYPE) {
            ItemStack cursor = e.getCursor();

            // Перетащили яйцо спавна — читаем EntityType из него
            if (cursor != null && cursor.getType() != Material.AIR && isSpawnEgg(cursor.getType()) && !right) {
                EntityType type = spawnEggToEntityType(cursor);
                if (type != null) {
                    mob.setEntityType(type);
                    mobManager.save();
                    player.sendMessage("§aТип моба установлен по яйцу: §f" + type.name());
                }
                schedule(player, mob);
                return;
            }

            // ПКМ — сброс к ZOMBIE
            if (right && !shift) {
                e.setCancelled(true);
                mob.setEntityType(EntityType.ZOMBIE);
                mobManager.save();
                mobEditorGUI.open(player, mob);
                return;
            }

            // Shift+ЛКМ — ввод типа через чат
            if (shift && !right) {
                e.setCancelled(true);
                player.closeInventory();
                chatInputListener.awaitCustomType(player, mob.getId());
                return;
            }

            e.setCancelled(true);
            return;
        }

        // --- Все остальные слоты GUI — отменяем любое взаимодействие ---
        e.setCancelled(true);

        // --- Функциональные слоты ---
        if (slot == MobEditorGUI.SLOT_NAME) {
            player.closeInventory();
            chatInputListener.awaitMobName(player, mob.getId());
            player.sendMessage("§eВведите новое имя моба в чат:");
            return;
        }

        if (slot == MobEditorGUI.SLOT_HEALTH) {
            double delta = shift ? 10 : 1;
            if (right) delta = -delta;
            mob.setMaxHealth(Math.max(1, mob.getMaxHealth() + delta));

        } else if (slot == MobEditorGUI.SLOT_EXP) {
            int delta = shift ? 10 : 1;
            if (right) delta = -delta;
            mob.setExpDrop(Math.max(0, mob.getExpDrop() + delta));

        } else if (slot == MobEditorGUI.SLOT_SPEED) {
            double delta = shift ? 0.05 : 0.01;
            if (right) delta = -delta;
            mob.setMoveSpeed(mob.getMoveSpeed() + delta);

        } else if (slot == MobEditorGUI.SLOT_LOOT) {
            int delta = shift ? 25 : 5;
            if (right) delta = -delta;
            mob.setLootDropChance(mob.getLootDropChance() + delta);

        } else if (slot == MobEditorGUI.SLOT_SAVE) {
            mobManager.save();
            player.sendMessage("§aМоб §f" + mob.getId() + " §aсохранён.");
            player.closeInventory();
            return;
        } else {
            return; // клик по пустому слоту — ничего не делаем
        }

        mobManager.save();
        mobEditorGUI.open(player, mob);
    }

    /**
     * Пробует автоматически определить правильный слот для предмета при Shift+клике из инвентаря.
     */
    private void tryAutoEquip(Player player, CustomMob mob, ItemStack item) {
        Material mat = item.getType();
        if (isHelmet(mat))     { applyEquip(mob, MobEditorGUI.SLOT_HELMET, mat, player); }
        else if (isChest(mat)) { applyEquip(mob, MobEditorGUI.SLOT_CHEST,  mat, player); }
        else if (isLegs(mat))  { applyEquip(mob, MobEditorGUI.SLOT_LEGS,   mat, player); }
        else if (isBoots(mat)) { applyEquip(mob, MobEditorGUI.SLOT_BOOTS,  mat, player); }
        else                   { applyEquip(mob, MobEditorGUI.SLOT_WEAPON, mat, player); }
        mobManager.save();
        mobEditorGUI.open(player, mob);
    }

    /** @return true если предмет принят */
    private boolean applyEquip(CustomMob mob, int slot, Material mat, Player player) {
        if (mat != Material.AIR && slot != MobEditorGUI.SLOT_WEAPON && !ARMOR_OK.contains(mat)) {
            player.sendMessage("§cЭтот предмет нельзя использовать как броню.");
            return false;
        }
        if      (slot == MobEditorGUI.SLOT_WEAPON) mob.setWeapon(mat);
        else if (slot == MobEditorGUI.SLOT_HELMET) mob.setHelmet(mat);
        else if (slot == MobEditorGUI.SLOT_CHEST)  mob.setChestplate(mat);
        else if (slot == MobEditorGUI.SLOT_LEGS)   mob.setLeggings(mat);
        else if (slot == MobEditorGUI.SLOT_BOOTS)  mob.setBoots(mat);
        mobManager.save();
        return true;
    }

    private void schedule(Player player, CustomMob mob) {
        player.getServer().getScheduler().runTask(
                player.getServer().getPluginManager().getPlugin("MSMP"),
                () -> mobEditorGUI.open(player, mob)
        );
    }

    // =========================================================
    //  Spawn egg helpers
    // =========================================================
    private boolean isSpawnEgg(Material mat) {
        return mat.name().endsWith("_SPAWN_EGG");
    }

    private EntityType spawnEggToEntityType(ItemStack item) {
        // Paper API: SpawnEggMeta#getSpawnedType()
        if (item.getItemMeta() instanceof SpawnEggMeta meta) {
            return meta.getSpawnedType();
        }
        // Fallback: вырезаем тип из имени материала
        String name = item.getType().name().replace("_SPAWN_EGG", "");
        try { return EntityType.valueOf(name); } catch (IllegalArgumentException ignored) { return null; }
    }

    // =========================================================
    //  Броня — вспомогательные методы
    // =========================================================
    private boolean isHelmet(Material m) {
        String n = m.name();
        return n.endsWith("_HELMET") || m == Material.TURTLE_HELMET;
    }
    private boolean isChest(Material m)  { return m.name().endsWith("_CHESTPLATE"); }
    private boolean isLegs(Material m)   { return m.name().endsWith("_LEGGINGS"); }
    private boolean isBoots(Material m)  { return m.name().endsWith("_BOOTS"); }

    // =========================================================
    //  SpawnerEditor (без изменений логики, только блокировка drag)
    // =========================================================
    private void handleSpawnerClick(InventoryClickEvent e, String spawnerId) {
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        CustomSpawner sp = spawnerManager.getSpawner(spawnerId);
        if (sp == null) { player.closeInventory(); return; }

        int slot  = e.getRawSlot();
        int topSize = e.getView().getTopInventory().getSize();
        if (slot >= topSize) return; // клик по инвентарю игрока — просто отменяем

        boolean shift = e.isShiftClick();
        boolean right = e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT;

        if (slot == SpawnerEditorGUI.SLOT_TARGET) {
            // Перетащили яйцо спавна — устанавливаем тип спавнера
            ItemStack cursor = e.getCursor();
            if (cursor != null && isSpawnEgg(cursor.getType()) && !right) {
                EntityType type = spawnEggToEntityType(cursor);
                if (type != null) {
                    sp.setSpawnTarget(type.name());
                    player.sendMessage("§aЦель спавнера: §f" + type.name());
                }
                spawnerManager.save();
                spawnerEditorGUI.open(player, sp);
                return;
            }
            if (shift && !right) {
                sp.setSpawnTarget("ZOMBIE");
            } else if (right) {
                List<String> ids = mobManager.getAll().keySet().stream().toList();
                if (!ids.isEmpty()) {
                    String current = sp.isCustomMobTarget() ? sp.getCustomMobId() : null;
                    int idx = current == null ? -1 : ids.indexOf(current);
                    idx = (idx + 1) % ids.size();
                    sp.setSpawnTarget("custom:" + ids.get(idx));
                } else {
                    player.sendMessage("§cНет ни одного кастомного моба. Создайте через /msmp create mob <id>.");
                }
            } else {
                List<EntityType> types = MobEditorGUI.MOB_TYPES;
                EntityType currentVanilla = sp.isCustomMobTarget() ? types.get(0) : safeType(sp.getSpawnTarget());
                int idx = types.indexOf(currentVanilla);
                if (idx < 0) idx = -1;
                idx = (idx + 1) % types.size();
                sp.setSpawnTarget(types.get(idx).name());
            }
        } else if (slot == SpawnerEditorGUI.SLOT_RATE) {
            int delta = shift ? 100 : 20;
            if (e.getClick() == ClickType.LEFT || e.getClick() == ClickType.SHIFT_LEFT) delta = Math.abs(delta);
            else delta = -Math.abs(delta);
            sp.setSpawnRateTicks(Math.max(20, sp.getSpawnRateTicks() + delta));
        } else if (slot == SpawnerEditorGUI.SLOT_RADIUS) {
            int delta = right ? -1 : 1;
            sp.setSpawnRadius(Math.max(1, sp.getSpawnRadius() + delta));
        } else if (slot == SpawnerEditorGUI.SLOT_MAX_NEARBY) {
            int delta = right ? -1 : 1;
            sp.setMaxNearbyEntities(Math.max(1, sp.getMaxNearbyEntities() + delta));
        } else if (slot == SpawnerEditorGUI.SLOT_ACTIVATION) {
            int delta = right ? -4 : 4;
            sp.setPlayerActivationRadius(Math.max(0, sp.getPlayerActivationRadius() + delta));
        } else if (slot == SpawnerEditorGUI.SLOT_COUNT) {
            int delta = right ? -1 : 1;
            sp.setSpawnCount(Math.max(1, sp.getSpawnCount() + delta));
        } else if (slot == SpawnerEditorGUI.SLOT_SAVE) {
            spawnerManager.save();
            player.sendMessage("§aСпавнер §f" + sp.getId() + " §aсохранён.");
            player.closeInventory();
            return;
        }

        spawnerManager.save();
        spawnerEditorGUI.open(player, sp);
    }

    private EntityType safeType(String name) {
        try { return EntityType.valueOf(name); } catch (IllegalArgumentException e) { return EntityType.ZOMBIE; }
    }
}
