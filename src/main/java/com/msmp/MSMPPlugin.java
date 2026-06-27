package com.msmp;

import com.msmp.commands.MSMPCommand;
import com.msmp.gui.MobEditorGUI;
import com.msmp.gui.SpawnerEditorGUI;
import com.msmp.listeners.ChatInputListener;
import com.msmp.listeners.GUIListener;
import com.msmp.listeners.SpawnListener;
import com.msmp.managers.MobManager;
import com.msmp.managers.SpawnerManager;
import com.msmp.tasks.SpawnerTask;
import org.bukkit.plugin.java.JavaPlugin;

public class MSMPPlugin extends JavaPlugin {

    private MobManager mobManager;
    private SpawnerManager spawnerManager;
    private MobEditorGUI mobEditorGUI;
    private SpawnerEditorGUI spawnerEditorGUI;
    private SpawnListener spawnListener;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        this.mobManager = new MobManager(this);
        this.spawnerManager = new SpawnerManager(this);
        this.mobEditorGUI = new MobEditorGUI(mobManager);
        this.spawnerEditorGUI = new SpawnerEditorGUI(mobManager);
        this.spawnListener = new SpawnListener(this);

        ChatInputListener chatInputListener = new ChatInputListener(mobManager, mobEditorGUI);

        getServer().getPluginManager().registerEvents(chatInputListener, this);
        getServer().getPluginManager().registerEvents(spawnListener, this);
        getServer().getPluginManager().registerEvents(
                new GUIListener(mobManager, spawnerManager, mobEditorGUI, spawnerEditorGUI, chatInputListener),
                this
        );

        MSMPCommand cmd = new MSMPCommand(this);
        getCommand("msmp").setExecutor(cmd);

        // запускаем фоновые задачи спавна для всех ранее сохранённых спавнеров
        SpawnerTask.startAll(this);

        getLogger().info("MSMP включен. Мобов загружено: " + mobManager.getAll().size()
                + ", спавнеров загружено: " + spawnerManager.getAll().size());
    }

    @Override
    public void onDisable() {
        if (mobManager != null) mobManager.save();
        if (spawnerManager != null) spawnerManager.save();
        getLogger().info("MSMP выключен, данные сохранены.");
    }

    public MobManager getMobManager() { return mobManager; }
    public SpawnerManager getSpawnerManager() { return spawnerManager; }
    public MobEditorGUI getMobEditorGUI() { return mobEditorGUI; }
    public SpawnerEditorGUI getSpawnerEditorGUI() { return spawnerEditorGUI; }
    public SpawnListener getSpawnListener() { return spawnListener; }
}
