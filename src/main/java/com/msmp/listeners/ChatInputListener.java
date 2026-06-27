package com.msmp.listeners;

import com.msmp.data.CustomMob;
import com.msmp.gui.MobEditorGUI;
import com.msmp.managers.MobManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ловит следующее сообщение игрока в чат, если он находится в режиме
 * "ожидания ввода имени" (после клика на NAME_TAG в MobEditorGUI).
 *
 * Используется Paper-событие AsyncChatEvent. Если ваш сервер не Paper/Purpur
 * (а чистый Spigot), замените на AsyncPlayerChatEvent из org.bukkit.event.player.
 */
public class ChatInputListener implements Listener {

    private final MobManager mobManager;
    private final MobEditorGUI mobEditorGUI;
    private final Map<UUID, String> awaitingMobNameFor = new HashMap<>();

    public ChatInputListener(MobManager mobManager, MobEditorGUI mobEditorGUI) {
        this.mobManager = mobManager;
        this.mobEditorGUI = mobEditorGUI;
    }

    public void awaitMobName(Player player, String mobId) {
        awaitingMobNameFor.put(player.getUniqueId(), mobId);
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (!awaitingMobNameFor.containsKey(uuid)) return;

        e.setCancelled(true); // не показываем это сообщение всем в чат
        String mobId = awaitingMobNameFor.remove(uuid);
        String newName = PlainTextComponentSerializer.plainText().serialize(e.message());

        CustomMob mob = mobManager.getMob(mobId);
        if (mob == null) return;

        mob.setDisplayName(newName);
        mobManager.save();

        // возвращаемся в основной поток, чтобы открыть инвентарь
        e.getPlayer().getServer().getScheduler().runTask(
                e.getPlayer().getServer().getPluginManager().getPlugin("MSMP"),
                () -> {
                    e.getPlayer().sendMessage("§aИмя моба обновлено: §f" + newName);
                    mobEditorGUI.open(e.getPlayer(), mob);
                }
        );
    }
}
