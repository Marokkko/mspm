package com.msmp.listeners;

import com.msmp.data.CustomMob;
import com.msmp.data.CustomSpawner;
import com.msmp.gui.LootEditorGUI;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;

import java.util.List;
import java.util.Set;

public class GUIListener implements Listener {

    private final MobManager mobManager;
    private final SpawnerManager spawnerManager;
    private final MobEditorGUI mobEditorGUI;
    private final SpawnerEditorGUI spawnerEditorGUI;
    private final LootEditorGUI lootEditorGUI;
    private final ChatInputListener chatInputListener;

    // Брони допустимые для слотов брони
    private static final Set<Material> ARMOR_OK = Set.of(
            Material.LEATHER_HELMET,     Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS,   Material.LEATHER_BOOTS,
            Material.IRON_HELMET,        Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS,      Material.IRON_BOOTS,
            Material.GOLDEN_HELMET,      Material.GOLDEN_CHESTPLATE,
            Material.GOLDEN_LEGGINGS,    Material.GOLDEN_BOOTS,
            Material.CHAINMAIL_HELMET,   Material.CHAINMAIL_CHESTPLATE,
            Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            Material.DIAMOND_HELMET,     Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS,   Material.DIAMOND_BOOTS,
            Material.NETHERITE_HELMET,   Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
            Material.TURTLE_HELMET
    );

    // Слоты экипировки (броня + оружие) — в них разрешено класть предметы
    private static final Set<Integer> EQUIP_SLOTS = Set.of(
            MobEditorGUI.SLOT_WEAPON,
            MobEditorGUI.SLOT_HELMET,
            MobEditorGUI.SLOT_CHEST,
            MobEditorGUI.SLOT_LEGS,
            MobEditorGUI.SLOT_BOOTS
    );

    public GUIListener(MobManager mobManager, SpawnerManager spawnerManager,
                       MobEditorGUI mobEditorGUI, SpawnerEditorGUI spawnerEditorGUI,
                       LootEditorGUI lootEditorGUI, ChatInputListener chatInputListener) {
        this.mobManager        = mobManager;
        this.spawnerManager    = spawnerManager;
        this.mobEditorGUI      = mobEditorGUI;
        this.spawnerEditorGUI  = spawnerEditorGUI;
        this.lootEditorGUI     = lootEditorGUI;
        this.chatInputListener = chatInputListener;
    }

    // ─────────────────────────────────────────────────────────────
    // InventoryDragEvent: блокируем ЛЮБОЕ drag в наш GUI
    // (drag — это когда тащишь и делишь стак, не путать с pickup)
    // ─────────────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        if (!isOurGUI(e.getView().getTitle())) return;
        int topSize = e.getView().getTopInventory().getSize();
        for (int slot : e.getRawSlots()) {
            if (slot < topSize) { e.setCancelled(true); return; }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // InventoryClickEvent — главный обработчик
    // ─────────────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (title.startsWith(MobEditorGUI.TITLE_PREFIX)) {
            handleMobClick(e, title.substring(MobEditorGUI.TITLE_PREFIX.length()));
        } else if (title.startsWith(SpawnerEditorGUI.TITLE_PREFIX)) {
            handleSpawnerClick(e, title.substring(SpawnerEditorGUI.TITLE_PREFIX.length()));
        } else if (title.startsWith(LootEditorGUI.TITLE_PREFIX)) {
            handleLootClick(e, title.substring(LootEditorGUI.TITLE_PREFIX.length()));
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  MOB EDITOR
    // ═════════════════════════════════════════════════════════════
    private void handleMobClick(InventoryClickEvent e, String mobId) {
        Player player  = (Player) e.getWhoClicked();
        CustomMob mob  = mobManager.getMob(mobId);
        if (mob == null) { e.setCancelled(true); player.closeInventory(); return; }

        int slot    = e.getRawSlot();
        int topSize = e.getView().getTopInventory().getSize(); // 45
        boolean shift = e.isShiftClick();
        boolean right = e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT;
        InventoryAction action = e.getAction();

        // ── Клик ПО НИЖНЕМУ инвентарю ──────────────────────────────────
        if (slot >= topSize) {
            // Разрешаем ТОЛЬКО shift-click для автоэкипировки
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);
                ItemStack item = e.getCurrentItem();
                if (item != null && item.getType() != Material.AIR) {
                    tryAutoEquip(player, mob, item);
                }
            } else {
                e.setCancelled(true);
            }
            return;
        }

        // ── СЛОТЫ ЭКИПИРОВКИ (weapon/helmet/chest/legs/boots) ──────────
        // Ключевое: НЕ отменяем событие заранее — сначала читаем курсор,
        // потом решаем как поступить.
        if (EQUIP_SLOTS.contains(slot)) {
            // ПКМ — очистить слот
            if (right) {
                e.setCancelled(true);
                applyEquip(mob, slot, Material.AIR, player);
                mobEditorGUI.open(player, mob);
                return;
            }

            // Любое действие "положить предмет в слот" — перехватываем
            // PLACE_ONE / PLACE_ALL / SWAP_WITH_CURSOR — все они несут cursor
            if (action == InventoryAction.PLACE_ALL
                    || action == InventoryAction.PLACE_ONE
                    || action == InventoryAction.PLACE_SOME
                    || action == InventoryAction.SWAP_WITH_CURSOR) {

                ItemStack cursor = e.getCursor();  // читаем ДО setCancelled!
                if (cursor != null && cursor.getType() != Material.AIR) {
                    Material mat = cursor.getType();
                    if (applyEquip(mob, slot, mat, player)) {
                        // Вручную кладём предмет в GUI-слот и убираем с курсора
                        e.setCancelled(true);
                        Inventory guiInv = e.getView().getTopInventory();
                        ItemStack old = guiInv.getItem(slot); // предмет, который был в слоте
                        guiInv.setItem(slot, cursor.clone());
                        // Если в слоте что-то лежало (SWAP) — возвращаем игроку на курсор
                        if (old != null && old.getType() != Material.AIR
                                && action == InventoryAction.SWAP_WITH_CURSOR) {
                            player.setItemOnCursor(old);
                        } else {
                            player.setItemOnCursor(new ItemStack(Material.AIR));
                        }
                        // Через тик обновляем GUI (лор иконки)
                        scheduleRefresh(player, mob);
                    } else {
                        e.setCancelled(true); // невалидный предмет — просто отменяем
                    }
                } else {
                    e.setCancelled(true);
                }
                return;
            }

            // Любые другие действия на экипировочных слотах — отменяем
            e.setCancelled(true);
            return;
        }

        // ── СЛОТ ТИПА МОБА ──────────────────────────────────────────────
        if (slot == MobEditorGUI.SLOT_TYPE) {
            // Перетащили яйцо — читаем cursor ДО setCancelled
            if (!right && (action == InventoryAction.PLACE_ALL
                    || action == InventoryAction.PLACE_ONE
                    || action == InventoryAction.SWAP_WITH_CURSOR)) {
                ItemStack cursor = e.getCursor();
                e.setCancelled(true);
                if (cursor != null && isSpawnEgg(cursor.getType())) {
                    EntityType type = spawnEggToEntityType(cursor);
                    if (type != null) {
                        mob.setEntityType(type);
                        mobManager.save();
                        player.sendMessage("§aТип моба: §f" + type.name());
                        mobEditorGUI.open(player, mob);
                    }
                }
                return;
            }
            e.setCancelled(true);
            // ПКМ — сброс
            if (right && !shift) {
                mob.setEntityType(EntityType.ZOMBIE);
                mobManager.save();
                mobEditorGUI.open(player, mob);
                return;
            }
            // Shift+ЛКМ — ввод через чат
            if (shift && !right) {
                player.closeInventory();
                chatInputListener.awaitCustomType(player, mob.getId());
            }
            return;
        }

        // ── Все остальные слоты GUI — всегда отменяем ───────────────────
        e.setCancelled(true);

        if (slot == MobEditorGUI.SLOT_NAME) {
            player.closeInventory();
            chatInputListener.awaitMobName(player, mob.getId());
            player.sendMessage("§eВведите новое имя моба в чат:");
            return;
        }
        if (slot == MobEditorGUI.SLOT_HEALTH) {
            double delta = (shift ? 10 : 1) * (right ? -1 : 1);
            mob.setMaxHealth(mob.getMaxHealth() + delta);
        } else if (slot == MobEditorGUI.SLOT_EXP) {
            int delta = (shift ? 10 : 1) * (right ? -1 : 1);
            mob.setExpDrop(mob.getExpDrop() + delta);
        } else if (slot == MobEditorGUI.SLOT_SPEED) {
            double delta = (shift ? 0.05 : 0.01) * (right ? -1 : 1);
            mob.setMoveSpeed(mob.getMoveSpeed() + delta);
        } else if (slot == MobEditorGUI.SLOT_LOOT) {
            int delta = (shift ? 25 : 5) * (right ? -1 : 1);
            mob.setLootDropChance(mob.getLootDropChance() + delta);
        } else if (slot == MobEditorGUI.SLOT_DROPS) {
            lootEditorGUI.open(player, mob);
            return;
        } else if (slot == MobEditorGUI.SLOT_SAVE) {
            mobManager.save();
            player.sendMessage("§aМоб §f" + mob.getId() + " §aсохранён.");
            player.closeInventory();
            return;
        } else {
            return; // пустой слот — ничего не делаем
        }
        mobManager.save();
        mobEditorGUI.open(player, mob);
    }

    /**
     * Shift+клик из инвентаря: автоматически определяем правильный слот
     * по типу предмета и применяем. Предмет уменьшаем на 1 у игрока.
     */
    private void tryAutoEquip(Player player, CustomMob mob, ItemStack item) {
        Material mat = item.getType();
        int target;
        if      (isHelmet(mat)) target = MobEditorGUI.SLOT_HELMET;
        else if (isChest(mat))  target = MobEditorGUI.SLOT_CHEST;
        else if (isLegs(mat))   target = MobEditorGUI.SLOT_LEGS;
        else if (isBoots(mat))  target = MobEditorGUI.SLOT_BOOTS;
        else                    target = MobEditorGUI.SLOT_WEAPON;

        if (applyEquip(mob, target, mat, player)) {
            mobManager.save();
            item.setAmount(item.getAmount() - 1); // убираем из инвентаря
            mobEditorGUI.open(player, mob);
        }
    }

    /**
     * Применяет материал к нужному полю CustomMob.
     * @return true если принято, false если невалидно (броня в неброневой слот)
     */
    private boolean applyEquip(CustomMob mob, int slot, Material mat, Player player) {
        if (mat != Material.AIR && slot != MobEditorGUI.SLOT_WEAPON && !ARMOR_OK.contains(mat)) {
            player.sendMessage("§cЭтот предмет не является бронёй.");
            return false;
        }
        switch (slot) {
            case MobEditorGUI.SLOT_WEAPON -> mob.setWeapon(mat);
            case MobEditorGUI.SLOT_HELMET -> mob.setHelmet(mat);
            case MobEditorGUI.SLOT_CHEST  -> mob.setChestplate(mat);
            case MobEditorGUI.SLOT_LEGS   -> mob.setLeggings(mat);
            case MobEditorGUI.SLOT_BOOTS  -> mob.setBoots(mat);
            default -> { return false; }
        }
        return true;
    }

    /** Переоткрываем GUI через 1 тик — чтобы лор иконок обновился */
    private void scheduleRefresh(Player player, CustomMob mob) {
        player.getServer().getScheduler().runTask(
                player.getServer().getPluginManager().getPlugin("MSMP"),
                () -> mobEditorGUI.open(player, mob)
        );
    }

    // ═════════════════════════════════════════════════════════════
    //  LOOT EDITOR
    // ═════════════════════════════════════════════════════════════
    private void handleLootClick(InventoryClickEvent e, String mobId) {
        Player player = (Player) e.getWhoClicked();
        CustomMob mob = mobManager.getMob(mobId);
        if (mob == null) { e.setCancelled(true); player.closeInventory(); return; }

        int slot    = e.getRawSlot();
        int topSize = e.getView().getTopInventory().getSize(); // 54
        boolean shift = e.isShiftClick();
        boolean right = e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT;
        InventoryAction action = e.getAction();

        // ── Клик из нижнего инвентаря ──
        if (slot >= topSize) {
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);
                ItemStack item = e.getCurrentItem();
                if (item != null && item.getType() != Material.AIR) {
                    addDrop(mob, item.getType(), player);
                    lootEditorGUI.open(player, mob);
                }
            } else {
                e.setCancelled(true);
            }
            return;
        }

        // ── Слот "добавить предмет" (SLOT_ADD): принимаем курсор ──
        if (slot == LootEditorGUI.SLOT_ADD) {
            if (action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE
                    || action == InventoryAction.SWAP_WITH_CURSOR) {
                ItemStack cursor = e.getCursor(); // читаем ДО setCancelled
                e.setCancelled(true);
                if (cursor != null && cursor.getType() != Material.AIR) {
                    addDrop(mob, cursor.getType(), player);
                    lootEditorGUI.open(player, mob);
                }
            } else {
                e.setCancelled(true);
            }
            return;
        }

        // Остальные слоты — всегда отменяем
        e.setCancelled(true);

        if (slot == LootEditorGUI.SLOT_BACK) {
            mobEditorGUI.open(player, mob);
            return;
        }
        if (slot == LootEditorGUI.SLOT_SAVE) {
            mobManager.save();
            player.sendMessage("§aДроп моба §f" + mob.getId() + " §aсохранён.");
            player.closeInventory();
            return;
        }

        // Разделитель (36-44) — игнорируем
        if (slot >= 36 && slot <= 44) return;

        // ── Слоты дропа (0-35) ──
        if (slot < LootEditorGUI.MAX_DROPS) {
            List<CustomMob.LootEntry> drops = mob.getCustomDrops();
            if (slot >= drops.size()) return; // пустой слот

            CustomMob.LootEntry entry = drops.get(slot);

            if (shift && right) {
                // Shift+ПКМ — удалить запись
                mob.removeCustomDrop(slot);
                mobManager.save();
                player.sendMessage("§cЗапись дропа удалена.");
                lootEditorGUI.open(player, mob);
                return;
            }

            if (shift) {
                // Shift+ЛКМ — minAmount +1, Shift+ПКМ — maxAmount +1
                if (!right) entry.setMinAmount(entry.getMinAmount() + 1);
                else        entry.setMaxAmount(entry.getMaxAmount() + 1);
            } else {
                // ЛКМ/ПКМ — шанс ±5%
                int delta = right ? -5 : 5;
                entry.setChance(entry.getChance() + delta);
            }

            // Средняя кнопка — уменьшить количество
            if (e.getClick() == ClickType.MIDDLE) {
                if (entry.getMinAmount() > 1) entry.setMinAmount(entry.getMinAmount() - 1);
                else if (entry.getMaxAmount() > 1) entry.setMaxAmount(entry.getMaxAmount() - 1);
            }

            mobManager.save();
            // Обновляем только эту иконку (без переоткрытия всего GUI)
            e.getView().getTopInventory().setItem(slot, LootEditorGUI.lootIcon(entry));
        }
    }

    private void addDrop(CustomMob mob, Material mat, Player player) {
        if (mob.getCustomDrops().size() >= LootEditorGUI.MAX_DROPS) {
            player.sendMessage("§cМаксимум " + LootEditorGUI.MAX_DROPS + " записей дропа.");
            return;
        }
        mob.addCustomDrop(mat, 1, 1, 100);
        mobManager.save();
        player.sendMessage("§aДобавлен дроп: §f" + mat.name() + " §7(100%, x1)");
    }

    // ═════════════════════════════════════════════════════════════
    //  SPAWNER EDITOR
    // ═════════════════════════════════════════════════════════════
    private void handleSpawnerClick(InventoryClickEvent e, String spawnerId) {
        Player player   = (Player) e.getWhoClicked();
        CustomSpawner sp = spawnerManager.getSpawner(spawnerId);
        if (sp == null) { e.setCancelled(true); player.closeInventory(); return; }

        int slot    = e.getRawSlot();
        int topSize = e.getView().getTopInventory().getSize();
        boolean shift = e.isShiftClick();
        boolean right = e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT;
        InventoryAction action = e.getAction();

        if (slot >= topSize) { e.setCancelled(true); return; }

        // Слот цели: яйцо спавна — читаем курсор ДО setCancelled
        if (slot == SpawnerEditorGUI.SLOT_TARGET) {
            if (!right && (action == InventoryAction.PLACE_ALL
                    || action == InventoryAction.PLACE_ONE
                    || action == InventoryAction.SWAP_WITH_CURSOR)) {
                ItemStack cursor = e.getCursor();
                e.setCancelled(true);
                if (cursor != null && isSpawnEgg(cursor.getType())) {
                    EntityType type = spawnEggToEntityType(cursor);
                    if (type != null) {
                        sp.setSpawnTarget(type.name());
                        player.sendMessage("§aЦель спавнера: §f" + type.name());
                    }
                    spawnerManager.save();
                    spawnerEditorGUI.open(player, sp);
                    return;
                }
            }
        }

        e.setCancelled(true);

        if (slot == SpawnerEditorGUI.SLOT_TARGET) {
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
                    player.sendMessage("§cНет кастомных мобов.");
                }
            } else {
                List<EntityType> types = MobEditorGUI.MOB_TYPES;
                EntityType cur = sp.isCustomMobTarget() ? types.get(0) : safeType(sp.getSpawnTarget());
                int idx = types.indexOf(cur);
                if (idx < 0) idx = -1;
                sp.setSpawnTarget(types.get((idx + 1) % types.size()).name());
            }
        } else if (slot == SpawnerEditorGUI.SLOT_RATE) {
            int delta = (shift ? 100 : 20) * (right ? -1 : 1);
            sp.setSpawnRateTicks(Math.max(20, sp.getSpawnRateTicks() + delta));
        } else if (slot == SpawnerEditorGUI.SLOT_RADIUS) {
            sp.setSpawnRadius(Math.max(1, sp.getSpawnRadius() + (right ? -1 : 1)));
        } else if (slot == SpawnerEditorGUI.SLOT_MAX_NEARBY) {
            sp.setMaxNearbyEntities(Math.max(1, sp.getMaxNearbyEntities() + (right ? -1 : 1)));
        } else if (slot == SpawnerEditorGUI.SLOT_ACTIVATION) {
            sp.setPlayerActivationRadius(Math.max(0, sp.getPlayerActivationRadius() + (right ? -4 : 4)));
        } else if (slot == SpawnerEditorGUI.SLOT_COUNT) {
            sp.setSpawnCount(Math.max(1, sp.getSpawnCount() + (right ? -1 : 1)));
        } else if (slot == SpawnerEditorGUI.SLOT_SAVE) {
            spawnerManager.save();
            player.sendMessage("§aСпавнер §f" + sp.getId() + " §aсохранён.");
            player.closeInventory();
            return;
        }

        spawnerManager.save();
        spawnerEditorGUI.open(player, sp);
    }

    // ─────────────────────────────────────────────────────────────
    //  Хелперы
    // ─────────────────────────────────────────────────────────────
    private boolean isOurGUI(String title) {
        return title.startsWith(MobEditorGUI.TITLE_PREFIX)
            || title.startsWith(SpawnerEditorGUI.TITLE_PREFIX)
            || title.startsWith(LootEditorGUI.TITLE_PREFIX);
    }

    private boolean isSpawnEgg(Material mat)  { return mat.name().endsWith("_SPAWN_EGG"); }

    private EntityType spawnEggToEntityType(ItemStack item) {
        if (item.getItemMeta() instanceof SpawnEggMeta meta) return meta.getSpawnedType();
        try { return EntityType.valueOf(item.getType().name().replace("_SPAWN_EGG", "")); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private boolean isHelmet(Material m) { return m.name().endsWith("_HELMET") || m == Material.TURTLE_HELMET; }
    private boolean isChest(Material m)  { return m.name().endsWith("_CHESTPLATE"); }
    private boolean isLegs(Material m)   { return m.name().endsWith("_LEGGINGS"); }
    private boolean isBoots(Material m)  { return m.name().endsWith("_BOOTS"); }

    private EntityType safeType(String n) {
        try { return EntityType.valueOf(n); } catch (IllegalArgumentException e) { return EntityType.ZOMBIE; }
    }
}
