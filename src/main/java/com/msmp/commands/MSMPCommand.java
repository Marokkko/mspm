package com.msmp.commands;

import com.msmp.MSMPPlugin;
import com.msmp.data.CustomMob;
import com.msmp.data.CustomSpawner;
import com.msmp.tasks.SpawnerTask;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MSMPCommand implements CommandExecutor {

    private final MSMPPlugin plugin;

    public MSMPCommand(MSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("msmp.admin")) {
            sender.sendMessage("§cУ вас нет прав на эту команду.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cЭту команду можно выполнять только в игре.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "edit" -> handleEdit(player, args);
            case "delete" -> handleDelete(player, args);
            case "list" -> handleList(player, args);
            default -> sendUsage(player);
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cИспользование: /msmp create <mob|spawner> <id>");
            return;
        }
        String type = args[1].toLowerCase();
        String id = args[2];

        if (type.equals("mob")) {
            if (plugin.getMobManager().exists(id)) {
                player.sendMessage("§cМоб с id '" + id + "' уже существует. Используйте /msmp edit mob " + id);
                return;
            }
            CustomMob mob = plugin.getMobManager().createMob(id);
            player.sendMessage("§aСоздан моб §f" + id + "§a. Открываю редактор...");
            plugin.getMobEditorGUI().open(player, mob);

        } else if (type.equals("spawner")) {
            if (plugin.getSpawnerManager().exists(id)) {
                player.sendMessage("§cСпавнер с id '" + id + "' уже существует. Используйте /msmp edit spawner " + id);
                return;
            }
            CustomSpawner sp = plugin.getSpawnerManager().createSpawner(id, player.getLocation());
            player.sendMessage("§aСпавнер §f" + id + " §aсоздан в вашей текущей позиции. Открываю редактор...");
            plugin.getSpawnerEditorGUI().open(player, sp);
            new SpawnerTask(plugin, id).runTask(plugin);

        } else {
            player.sendMessage("§cНеизвестный тип: " + type + ". Используйте 'mob' или 'spawner'.");
        }
    }

    private void handleEdit(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cИспользование: /msmp edit <mob|spawner> <id>");
            return;
        }
        String type = args[1].toLowerCase();
        String id = args[2];

        if (type.equals("mob")) {
            CustomMob mob = plugin.getMobManager().getMob(id);
            if (mob == null) {
                player.sendMessage("§cМоб '" + id + "' не найден.");
                return;
            }
            plugin.getMobEditorGUI().open(player, mob);
        } else if (type.equals("spawner")) {
            CustomSpawner sp = plugin.getSpawnerManager().getSpawner(id);
            if (sp == null) {
                player.sendMessage("§cСпавнер '" + id + "' не найден.");
                return;
            }
            plugin.getSpawnerEditorGUI().open(player, sp);
        } else {
            player.sendMessage("§cНеизвестный тип: " + type + ". Используйте 'mob' или 'spawner'.");
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cИспользование: /msmp delete <mob|spawner> <id>");
            return;
        }
        String type = args[1].toLowerCase();
        String id = args[2];
        if (type.equals("mob")) {
            plugin.getMobManager().delete(id);
            player.sendMessage("§aМоб '" + id + "' удалён.");
        } else if (type.equals("spawner")) {
            plugin.getSpawnerManager().delete(id);
            player.sendMessage("§aСпавнер '" + id + "' удалён (активный таск остановится сам на следующем цикле).");
        }
    }

    private void handleList(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cИспользование: /msmp list <mob|spawner>");
            return;
        }
        String type = args[1].toLowerCase();
        if (type.equals("mob")) {
            player.sendMessage("§eКастомные мобы: §f" + String.join(", ", plugin.getMobManager().getAll().keySet()));
        } else if (type.equals("spawner")) {
            player.sendMessage("§eСпавнеры: §f" + String.join(", ", plugin.getSpawnerManager().getAll().keySet()));
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage("§6--- MSMP ---");
        player.sendMessage("§e/msmp create mob <id> §7- создать кастомного моба");
        player.sendMessage("§e/msmp create spawner <id> §7- создать спавнер в вашей позиции");
        player.sendMessage("§e/msmp edit mob <id> §7- открыть редактор моба");
        player.sendMessage("§e/msmp edit spawner <id> §7- открыть редактор спавнера");
        player.sendMessage("§e/msmp delete mob|spawner <id> §7- удалить");
        player.sendMessage("§e/msmp list mob|spawner §7- список");
    }
}
