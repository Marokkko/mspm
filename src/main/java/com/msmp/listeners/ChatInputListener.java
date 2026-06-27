package com.msmp.listeners;

import com.msmp.data.CustomMob;
import com.msmp.gui.MobEditorGUI;
import com.msmp.managers.MobManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ловит сообщения игрока в чат для двух режимов:
 *  1. Ввод нового display-имени моба (после клика на NAME_TAG)
 *  2. Ввод имени кастомного моба как EntityType (Shift+ЛКМ по SLOT_TYPE)
 *     — позволяет привязать "тип" к другому кастомному мобу через имя
 *     (хранится в displayName под префиксом "custom:")
 */
public class ChatInputListener implements Listener {

    public enum InputMode { MOB_NAME, MOB_TYPE_CUSTOM }

    private final MobManager mobManager;
    private final MobEditorGUI mobEditorGUI;
    private final Map<UUID, String>     awaitingMobNameFor   = new HashMap<>();
    private final Map<UUID, String>     awaitingCustomTypeFor = new HashMap<>();

    public ChatInputListener(MobManager mobManager, MobEditorGUI mobEditorGUI) {
        this.mobManager = mobManager;
        this.mobEditorGUI = mobEditorGUI;
    }

    /** Режим 1: ожидаем имя моба */
    public void awaitMobName(Player player, String mobId) {
        awaitingCustomTypeFor.remove(player.getUniqueId());
        awaitingMobNameFor.put(player.getUniqueId(), mobId);
    }

    /** Режим 2: ожидаем ввод имени кастомного моба как типа */
    public void awaitCustomType(Player player, String mobId) {
        awaitingMobNameFor.remove(player.getUniqueId());
        awaitingCustomTypeFor.put(player.getUniqueId(), mobId);
        player.sendMessage("§eВведите §fEntityType §eмоба в чат (например: §fZOMBIE, SKELETON, CREEPER§e).");
        player.sendMessage("§eДоступные стандартные типы (часть): §fZOMBIE, SKELETON, SPIDER, CREEPER, HUSK, STRAY, BLAZE...");
    }

    public boolean isAwaiting(Player player) {
        UUID uuid = player.getUniqueId();
        return awaitingMobNameFor.containsKey(uuid) || awaitingCustomTypeFor.containsKey(uuid);
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        String raw = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();

        if (awaitingMobNameFor.containsKey(uuid)) {
            e.setCancelled(true);
            String mobId = awaitingMobNameFor.remove(uuid);
            CustomMob mob = mobManager.getMob(mobId);
            if (mob == null) return;

            mob.setDisplayName(raw);
            mobManager.save();

            e.getPlayer().getServer().getScheduler().runTask(
                    e.getPlayer().getServer().getPluginManager().getPlugin("MSMP"),
                    () -> {
                        e.getPlayer().sendMessage("§aИмя моба обновлено: §f" + raw);
                        mobEditorGUI.open(e.getPlayer(), mob);
                    }
            );
            return;
        }

        if (awaitingCustomTypeFor.containsKey(uuid)) {
            e.setCancelled(true);
            String mobId = awaitingCustomTypeFor.remove(uuid);
            CustomMob mob = mobManager.getMob(mobId);
            if (mob == null) return;

            String input = raw.toUpperCase().trim();
            e.getPlayer().getServer().getScheduler().runTask(
                    e.getPlayer().getServer().getPluginManager().getPlugin("MSMP"),
                    () -> {
                        try {
                            EntityType type = EntityType.valueOf(input);
                            mob.setEntityType(type);
                            mobManager.save();
                            e.getPlayer().sendMessage("§aТип моба установлен: §f" + type.name());
                        } catch (IllegalArgumentException ex) {
                            e.getPlayer().sendMessage("§cНеизвестный тип: §f" + input + "§c. Попробуйте снова.");
                        }
                        mobEditorGUI.open(e.getPlayer(), mob);
                    }
            );
        }
    }
}
