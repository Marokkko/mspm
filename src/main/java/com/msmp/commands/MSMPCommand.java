package com.msmp.commands;

import com.msmp.MSMPPlugin;
import com.msmp.data.CustomMob;
import com.msmp.data.CustomSpawner;
import com.msmp.data.LevelLocate;
import com.msmp.tasks.SpawnerTask;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class MSMPCommand implements CommandExecutor, TabCompleter {

    private final MSMPPlugin plugin;

    public MSMPCommand(MSMPPlugin plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────
    //  onCommand
    // ─────────────────────────────────────────────────────────────
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("msmp.admin")) {
            sender.sendMessage("§cУ вас нет прав на эту команду.");
            return true;
        }

        if (args.length == 0) { sendUsage(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(sender, args);
            case "edit"   -> handleEdit(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "list"   -> handleList(sender, args);
            case "state"  -> handleState(sender, args);
            default       -> sendUsage(sender);
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────
    //  create
    // ─────────────────────────────────────────────────────────────
    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cИспользование: /msmp create <mob|spawner|locate> ...");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "mob"     -> createMob(sender, args);
            case "spawner" -> createSpawner(sender, args);
            case "locate"  -> createLocate(sender, args);
            default        -> sender.sendMessage("§cНеизвестный тип: " + args[1]
                    + ". Доступно: mob, spawner, locate");
        }
    }

    private void createMob(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько игроки могут создавать мобов (нужен GUI)."); return;
        }
        if (args.length < 3) {
            player.sendMessage("§cИспользование: /msmp create mob <id>"); return;
        }
        String id = args[2];
        if (plugin.getMobManager().exists(id)) {
            player.sendMessage("§cМоб '"+id+"' уже существует. Используйте /msmp edit mob "+id); return;
        }
        CustomMob mob = plugin.getMobManager().createMob(id);
        player.sendMessage("§aМоб §f" + id + " §aсоздан. Открываю редактор...");
        plugin.getMobEditorGUI().open(player, mob);
    }

    private void createSpawner(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько игроки могут создавать спавнеры (нужна позиция)."); return;
        }
        if (args.length < 3) {
            player.sendMessage("§cИспользование: /msmp create spawner <id>"); return;
        }
        String id = args[2];
        if (plugin.getSpawnerManager().exists(id)) {
            player.sendMessage("§cСпавнер '"+id+"' уже существует."); return;
        }
        CustomSpawner sp = plugin.getSpawnerManager().createSpawner(id, player.getLocation());
        player.sendMessage("§aСпавнер §f" + id + " §aсоздан. Открываю редактор...");
        plugin.getSpawnerEditorGUI().open(player, sp);
        new SpawnerTask(plugin, id).runTask(plugin);
    }

    /**
     * /msmp create locate <lvl> [x] [y] [z]
     *
     * Если координаты не указаны — берём позицию игрока.
     * Если указаны — парсим как числа (поддерживаем как целые, так и дробные).
     * Мир всегда берётся из текущего мира игрока (или мира по умолчанию для консоли).
     */
    private void createLocate(CommandSender sender, String[] args) {
        // args: create locate <lvl> [x y z]
        if (args.length < 3) {
            sender.sendMessage("§cИспользование: /msmp create locate <lvl> [x] [y] [z]"); return;
        }

        int level;
        try { level = Integer.parseInt(args[2]); }
        catch (NumberFormatException e) {
            sender.sendMessage("§cУровень должен быть целым числом."); return;
        }
        if (level < 0) {
            sender.sendMessage("§cУровень не может быть отрицательным."); return;
        }

        double x, y, z;
        String worldName;

        if (args.length >= 6) {
            // Координаты указаны вручную
            try {
                x = Double.parseDouble(args[3]);
                y = Double.parseDouble(args[4]);
                z = Double.parseDouble(args[5]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cКоординаты должны быть числами. Пример: /msmp create locate 10 100 64 200"); return;
            }
            // Мир берём у игрока, или "world" для консоли
            worldName = (sender instanceof Player p) ? p.getWorld().getName() : "world";
        } else {
            // Без координат — нужен игрок
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cКонсоль должна указать координаты: /msmp create locate <lvl> <x> <y> <z>"); return;
            }
            x = player.getLocation().getX();
            y = player.getLocation().getY();
            z = player.getLocation().getZ();
            worldName = player.getWorld().getName();
        }

        boolean overwrite = plugin.getLocateManager().getLocate(level) != null;
        plugin.getLocateManager().addLocate(new LevelLocate(level, worldName, x, y, z));

        String msg = overwrite
                ? "§eТочка для lvl §f" + level + " §eобновлена: §f" + worldName
                    + " (" + fmt(x) + ", " + fmt(y) + ", " + fmt(z) + ")"
                : "§aТочка телепорта для lvl §f" + level + " §aсоздана: §f" + worldName
                    + " (" + fmt(x) + ", " + fmt(y) + ", " + fmt(z) + ")";
        sender.sendMessage(msg);
    }

    // ─────────────────────────────────────────────────────────────
    //  state
    // ─────────────────────────────────────────────────────────────
    /**
     * /msmp state <player> <true|false>
     *
     * Устанавливает флаг inDungeon у игрока.
     * Работает и из консоли, и в игре.
     */
    private void handleState(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cИспользование: /msmp state <player> <true|false>"); return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            // Пробуем найти по UUID (для оффлайн-записи)
            sender.sendMessage("§cИгрок §f" + args[1] + " §cне в сети. Состояние обновить можно только онлайн-игроку.");
            return;
        }

        boolean state;
        switch (args[2].toLowerCase()) {
            case "true",  "on",  "1", "yes" -> state = true;
            case "false", "off", "0", "no"  -> state = false;
            default -> {
                sender.sendMessage("§cЗначение должно быть §ftrue §cили §ffalse§c."); return;
            }
        }

        plugin.getLocateManager().setInDungeon(target.getUniqueId(), state);

        String stateStr = state ? "§aвключён §7(teleport при lvl-up)" : "§cвыключен";
        sender.sendMessage("§e[MSMP] §fInDungeon §7для §f" + target.getName() + "§7: " + stateStr);
        target.sendMessage("§e[MSMP] §7Ваш режим §fInDungeon §7установлен: " + stateStr);
    }

    // ─────────────────────────────────────────────────────────────
    //  edit / delete / list
    // ─────────────────────────────────────────────────────────────
    private void handleEdit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("§cТолько в игре."); return; }
        if (args.length < 3) { player.sendMessage("§cИспользование: /msmp edit <mob|spawner> <id>"); return; }
        switch (args[1].toLowerCase()) {
            case "mob" -> {
                CustomMob mob = plugin.getMobManager().getMob(args[2]);
                if (mob == null) { player.sendMessage("§cМоб '"+args[2]+"' не найден."); return; }
                plugin.getMobEditorGUI().open(player, mob);
            }
            case "spawner" -> {
                CustomSpawner sp = plugin.getSpawnerManager().getSpawner(args[2]);
                if (sp == null) { player.sendMessage("§cСпавнер '"+args[2]+"' не найден."); return; }
                plugin.getSpawnerEditorGUI().open(player, sp);
            }
            default -> player.sendMessage("§cНеизвестный тип: " + args[1]);
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage("§cИспользование: /msmp delete <mob|spawner|locate> <id/lvl>"); return; }
        switch (args[1].toLowerCase()) {
            case "mob" -> {
                plugin.getMobManager().delete(args[2]);
                sender.sendMessage("§aМоб '"+args[2]+"' удалён.");
            }
            case "spawner" -> {
                plugin.getSpawnerManager().delete(args[2]);
                sender.sendMessage("§aСпавнер '"+args[2]+"' удалён.");
            }
            case "locate" -> {
                try {
                    int lvl = Integer.parseInt(args[2]);
                    boolean ok = plugin.getLocateManager().removeLocate(lvl);
                    sender.sendMessage(ok ? "§aТочка для lvl "+lvl+" удалена." : "§cТочка для lvl "+lvl+" не найдена.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cУровень должен быть числом.");
                }
            }
            default -> sender.sendMessage("§cНеизвестный тип: " + args[1]);
        }
    }

    private void handleList(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("§cИспользование: /msmp list <mob|spawner|locate|states>"); return; }
        switch (args[1].toLowerCase()) {
            case "mob" ->
                sender.sendMessage("§eМобы: §f" + String.join(", ", plugin.getMobManager().getAll().keySet()));
            case "spawner" ->
                sender.sendMessage("§eСпавнеры: §f" + String.join(", ", plugin.getSpawnerManager().getAll().keySet()));
            case "locate" -> {
                if (plugin.getLocateManager().getAllLocates().isEmpty()) {
                    sender.sendMessage("§7Нет ни одной точки телепорта."); return;
                }
                sender.sendMessage("§e--- Точки телепорта ---");
                plugin.getLocateManager().getAllLocates().forEach((lvl, loc) ->
                        sender.sendMessage("§f  Lvl " + lvl + " §7→ §f"
                                + loc.getWorldName() + " §8("
                                + fmt(loc.getX()) + ", "
                                + fmt(loc.getY()) + ", "
                                + fmt(loc.getZ()) + "§8)"));
            }
            case "states" -> {
                sender.sendMessage("§e--- inDungeon состояния ---");
                Map<UUID, Boolean> states = plugin.getLocateManager().getAllStates();
                if (states.isEmpty()) { sender.sendMessage("§7(пусто)"); return; }
                states.forEach((uuid, val) -> {
                    Player p = Bukkit.getPlayer(uuid);
                    String name = p != null ? p.getName() : uuid.toString().substring(0, 8) + "...";
                    sender.sendMessage("  §f" + name + " §7→ " + (val ? "§aтrue" : "§cfalse"));
                });
            }
            default -> sender.sendMessage("§cНеизвестный тип: " + args[1]);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Tab Completion
    // ─────────────────────────────────────────────────────────────
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("msmp.admin")) return List.of();

        if (args.length == 1) return filter(args[0], "create","edit","delete","list","state");

        return switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length == 2) yield filter(args[1], "mob","spawner","locate");
                if (args.length == 3 && args[1].equalsIgnoreCase("locate")) yield List.of("<level>");
                yield List.of();
            }
            case "edit" -> {
                if (args.length == 2) yield filter(args[1], "mob","spawner");
                if (args.length == 3) {
                    if (args[1].equalsIgnoreCase("mob"))
                        yield filter(args[2], plugin.getMobManager().getAll().keySet().toArray(new String[0]));
                    if (args[1].equalsIgnoreCase("spawner"))
                        yield filter(args[2], plugin.getSpawnerManager().getAll().keySet().toArray(new String[0]));
                }
                yield List.of();
            }
            case "delete" -> {
                if (args.length == 2) yield filter(args[1], "mob","spawner","locate");
                if (args.length == 3) {
                    if (args[1].equalsIgnoreCase("mob"))
                        yield filter(args[2], plugin.getMobManager().getAll().keySet().toArray(new String[0]));
                    if (args[1].equalsIgnoreCase("spawner"))
                        yield filter(args[2], plugin.getSpawnerManager().getAll().keySet().toArray(new String[0]));
                    if (args[1].equalsIgnoreCase("locate"))
                        yield plugin.getLocateManager().getAllLocates().keySet()
                                .stream().map(String::valueOf).collect(Collectors.toList());
                }
                yield List.of();
            }
            case "list" -> {
                if (args.length == 2) yield filter(args[1], "mob","spawner","locate","states");
                yield List.of();
            }
            case "state" -> {
                if (args.length == 2) {
                    yield Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 3) yield filter(args[2], "true","false");
                yield List.of();
            }
            default -> List.of();
        };
    }

    // ─────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────
    private List<String> filter(String prefix, String... options) {
        List<String> result = new ArrayList<>();
        for (String o : options) if (o.toLowerCase().startsWith(prefix.toLowerCase())) result.add(o);
        return result;
    }

    private String fmt(double v) { return String.valueOf(Math.round(v * 10) / 10.0); }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§6§l--- MSMP ---");
        sender.sendMessage("§e/msmp create mob <id> §8- §7создать кастомного моба");
        sender.sendMessage("§e/msmp create spawner <id> §8- §7создать спавнер в вашей позиции");
        sender.sendMessage("§e/msmp create locate <lvl> §8[x y z] §8- §7задать точку телепорта для уровня XP");
        sender.sendMessage("§e/msmp edit mob|spawner <id> §8- §7открыть редактор");
        sender.sendMessage("§e/msmp delete mob|spawner|locate <id/lvl> §8- §7удалить");
        sender.sendMessage("§e/msmp list mob|spawner|locate|states §8- §7список");
        sender.sendMessage("§e/msmp state <player> <true|false> §8- §7включить/выключить inDungeon");
    }
}
