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
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

public class GUIListener implements Listener {

    private final MobManager mobManager;
    private final SpawnerManager spawnerManager;
    private final MobEditorGUI mobEditorGUI;
    private final SpawnerEditorGUI spawnerEditorGUI;
    private final ChatInputListener chatInputListener;

    // материалы, которые разрешено класть как "оружие"/"броню" (можно расширить)
    private static final Set<Material> ARMOR_OK = Set.of(
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
            Material.TURTLE_HELMET
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

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (title.startsWith(MobEditorGUI.TITLE_PREFIX)) {
            handleMobClick(e, title.substring(MobEditorGUI.TITLE_PREFIX.length()));
        } else if (title.startsWith(SpawnerEditorGUI.TITLE_PREFIX)) {
            handleSpawnerClick(e, title.substring(SpawnerEditorGUI.TITLE_PREFIX.length()));
        }
    }

    private void handleMobClick(InventoryClickEvent e, String mobId) {
        Player player = (Player) e.getWhoClicked();
        CustomMob mob = mobManager.getMob(mobId);
        if (mob == null) {
            player.closeInventory();
            return;
        }

        int slot = e.getRawSlot();
        boolean shift = e.isShiftClick();
        boolean right = e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT;

        // Разрешаем класть предметы прямо в слоты экипировки
        if (slot == MobEditorGUI.SLOT_WEAPON || slot == MobEditorGUI.SLOT_HELMET
                || slot == MobEditorGUI.SLOT_CHEST || slot == MobEditorGUI.SLOT_LEGS
                || slot == MobEditorGUI.SLOT_BOOTS) {

            ItemStack cursor = e.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR && !right) {
                // игрок кладёт предмет своим курсором — разрешаем это (не отменяем событие)
                Material mat = cursor.getType();
                applyEquip(mob, slot, mat);
                // событие не отменяем, чтобы предмет визуально лёг в слот;
                // но дальше нужно обновить лор — переоткрываем меню через тик
                schedule(player, mob);
                return;
            }
            if (right) {
                e.setCancelled(true);
                applyEquip(mob, slot, Material.AIR);
                mobEditorGUI.open(player, mob);
                return;
            }
            e.setCancelled(true);
            return;
        }

        e.setCancelled(true);

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
        } else if (slot == MobEditorGUI.SLOT_TYPE) {
            List<EntityType> types = MobEditorGUI.MOB_TYPES;
            int idx = types.indexOf(mob.getEntityType());
            if (idx < 0) idx = 0;
            idx = right ? (idx - 1 + types.size()) % types.size() : (idx + 1) % types.size();
            mob.setEntityType(types.get(idx));
        } else if (slot == MobEditorGUI.SLOT_EXP) {
            int delta = shift ? 10 : 1;
            if (right) delta = -delta;
            mob.setExpDrop(Math.max(0, mob.getExpDrop() + delta));
        } else if (slot == MobEditorGUI.SLOT_SAVE) {
            mobManager.save();
            player.sendMessage("§aМоб §f" + mob.getId() + " §aсохранён.");
            player.closeInventory();
            return;
        }

        mobManager.save();
        mobEditorGUI.open(player, mob); // перерисовываем меню с новыми значениями
    }

    private void applyEquip(CustomMob mob, int slot, Material mat) {
        if (mat != Material.AIR && slot != MobEditorGUI.SLOT_WEAPON && !ARMOR_OK.contains(mat)) {
            return; // не валидный предмет брони — игнорируем
        }
        if (slot == MobEditorGUI.SLOT_WEAPON) mob.setWeapon(mat);
        else if (slot == MobEditorGUI.SLOT_HELMET) mob.setHelmet(mat);
        else if (slot == MobEditorGUI.SLOT_CHEST) mob.setChestplate(mat);
        else if (slot == MobEditorGUI.SLOT_LEGS) mob.setLeggings(mat);
        else if (slot == MobEditorGUI.SLOT_BOOTS) mob.setBoots(mat);
        mobManager.save();
    }

    private void schedule(Player player, CustomMob mob) {
        // переоткрываем меню через тик, чтобы предмет успел визуально встать в слот
        player.getServer().getScheduler().runTask(
                player.getServer().getPluginManager().getPlugin("MSMP"),
                () -> mobEditorGUI.open(player, mob)
        );
    }

    private void handleSpawnerClick(InventoryClickEvent e, String spawnerId) {
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        CustomSpawner sp = spawnerManager.getSpawner(spawnerId);
        if (sp == null) {
            player.closeInventory();
            return;
        }

        int slot = e.getRawSlot();
        boolean shift = e.isShiftClick();
        boolean right = e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT;

        if (slot == SpawnerEditorGUI.SLOT_TARGET) {
            if (shift && !right) {
                sp.setSpawnTarget("ZOMBIE");
            } else if (right) {
                // циклически переключаем по кастомным мобам
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
            if (right) delta = -delta; else delta = -delta; // ЛКМ уменьшает, ПКМ увеличивает (см. лор)
            if (e.getClick() == ClickType.LEFT || e.getClick() == ClickType.SHIFT_LEFT) delta = -Math.abs(delta);
            else delta = Math.abs(delta);
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
        try {
            return EntityType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return EntityType.ZOMBIE;
        }
    }
}
