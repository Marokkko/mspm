package com.msmp;

import com.msmp.commands.MSMPCommand;
import com.msmp.gui.LootEditorGUI;
import com.msmp.gui.MobEditorGUI;
import com.msmp.gui.SpawnerEditorGUI;
import com.msmp.listeners.ChatInputListener;
import com.msmp.listeners.GUIListener;
import com.msmp.listeners.LevelUpListener;
import com.msmp.listeners.SpawnListener;
import com.msmp.managers.LocateManager;
import com.msmp.managers.MobManager;
import com.msmp.managers.SpawnerManager;
import com.msmp.tasks.SpawnerTask;
import org.bukkit.plugin.java.JavaPlugin;

public class MSMPPlugin extends JavaPlugin {

    private MobManager mobManager;
    private SpawnerManager spawnerManager;
    private LocateManager locateManager;
    private MobEditorGUI mobEditorGUI;
    private SpawnerEditorGUI spawnerEditorGUI;
    private LootEditorGUI lootEditorGUI;
    private SpawnListener spawnListener;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        this.mobManager      = new MobManager(this);
        this.spawnerManager  = new SpawnerManager(this);
        this.locateManager   = new LocateManager(this);
        this.mobEditorGUI    = new MobEditorGUI(mobManager);
        this.spawnerEditorGUI = new SpawnerEditorGUI(mobManager);
        this.lootEditorGUI   = new LootEditorGUI(mobManager);
        this.spawnListener   = new SpawnListener(this);

        ChatInputListener chatInputListener = new ChatInputListener(mobManager, mobEditorGUI);

        getServer().getPluginManager().registerEvents(chatInputListener, this);
        getServer().getPluginManager().registerEvents(spawnListener, this);
        getServer().getPluginManager().registerEvents(new LevelUpListener(this), this);
        getServer().getPluginManager().registerEvents(
                new GUIListener(mobManager, spawnerManager, mobEditorGUI,
                        spawnerEditorGUI, lootEditorGUI, chatInputListener),
                this
        );

        MSMPCommand cmd = new MSMPCommand(this);
        getCommand("msmp").setExecutor(cmd);
        getCommand("msmp").setTabCompleter(cmd);

        SpawnerTask.startAll(this);

        getLogger().info("MSMP включен. Мобов: " + mobManager.getAll().size()
                + ", спавнеров: " + spawnerManager.getAll().size()
                + ", locate-точек: " + locateManager.getAllLocates().size());
    }

    @Override
    public void onDisable() {
        if (mobManager != null)   mobManager.save();
        if (spawnerManager != null) spawnerManager.save();
        getLogger().info("MSMP выключен, данные сохранены.");
    }

    public MobManager       getMobManager()       { return mobManager; }
    public SpawnerManager   getSpawnerManager()   { return spawnerManager; }
    public LocateManager    getLocateManager()    { return locateManager; }
    public MobEditorGUI     getMobEditorGUI()     { return mobEditorGUI; }
    public SpawnerEditorGUI getSpawnerEditorGUI() { return spawnerEditorGUI; }
    public LootEditorGUI    getLootEditorGUI()    { return lootEditorGUI; }
    public SpawnListener    getSpawnListener()    { return spawnListener; }
}
