package ru.hubber;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.hubber.book.BookManager;
import ru.hubber.collision.CollisionManager;
import ru.hubber.commands.BackCommand;
import ru.hubber.commands.HubCommand;
import ru.hubber.commands.HubSpawnCommand;
import ru.hubber.commands.HubberCommand;
import ru.hubber.commands.JoinServerCommand;
import ru.hubber.commands.SetTriggerCommand;
import ru.hubber.commands.VerificationCommand;
import ru.hubber.config.ConfigManager;
import ru.hubber.listeners.BookListener;
import ru.hubber.listeners.NpcListener;
import ru.hubber.listeners.PlayerJoinListener;
import ru.hubber.listeners.ProtectionListener;
import ru.hubber.listeners.TriggerListener;
import ru.hubber.npc.NpcManager;
import ru.hubber.server.ServerMonitor;
import ru.hubber.spawn.SpawnManager;
import ru.hubber.trigger.TriggerManager;

public class HubberPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private SpawnManager spawnManager;
    private CollisionManager collisionManager;
    private BookManager bookManager;
    private TriggerManager triggerManager;
    private NpcManager npcManager;
    private ServerMonitor serverMonitor;

    @Override
    public void onEnable() {
        // 1. Initialize configuration
        this.configManager = new ConfigManager(this);

        // 2. Initialize managers
        this.spawnManager = new SpawnManager(this, configManager);
        this.collisionManager = new CollisionManager(this, configManager);
        this.bookManager = new BookManager(this, configManager);
        this.triggerManager = new TriggerManager(this);
        this.npcManager = new NpcManager(this);
        this.serverMonitor = new ServerMonitor(this, configManager);

        // 3. Register commands
        registerCommands();

        // 4. Register listeners
        registerListeners();

        // 5. Register BungeeCord plugin messaging channel (for Velocity / BungeeCord proxy)
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        getLogger().info("=========================================");
        getLogger().info(" Hubber v" + getPluginMeta().getVersion() + " successfully enabled on Paper 26.3!");
        getLogger().info(" Developed by kilkazens for Initium Magnum Terra.");
        getLogger().info(" Protection, spawn, triggers, NPCs, book, server monitor, and gamemode active.");
        getLogger().info("=========================================");
    }

    @Override
    public void onDisable() {
        // Unregister BungeeCord messaging channel
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");

        if (serverMonitor != null) {
            serverMonitor.stop();
        }

        if (collisionManager != null) {
            collisionManager.disable();
        }
        getLogger().info("Hubber successfully disabled.");
    }

    private void registerCommands() {
        PluginCommand hubCmd = getCommand("hub");
        if (hubCmd != null) {
            HubCommand hubHandler = new HubCommand(configManager, spawnManager, npcManager);
            hubCmd.setExecutor(hubHandler);
            hubCmd.setTabCompleter(hubHandler);
        }

        PluginCommand joinserverCmd = getCommand("joinserver");
        if (joinserverCmd != null) {
            joinserverCmd.setExecutor(new JoinServerCommand(this, configManager));
        }

        PluginCommand hubspawnCmd = getCommand("hubspawn");
        if (hubspawnCmd != null) {
            hubspawnCmd.setExecutor(new HubSpawnCommand(configManager, spawnManager));
        }

        PluginCommand backCmd = getCommand("back");
        if (backCmd != null) {
            backCmd.setExecutor(new BackCommand(configManager, spawnManager));
        }

        PluginCommand verificationCmd = getCommand("verification");
        if (verificationCmd != null) {
            verificationCmd.setExecutor(new VerificationCommand(configManager));
        }

        PluginCommand settriggerCmd = getCommand("settrigger");
        if (settriggerCmd != null) {
            SetTriggerCommand triggerHandler = new SetTriggerCommand(configManager, triggerManager);
            settriggerCmd.setExecutor(triggerHandler);
            settriggerCmd.setTabCompleter(triggerHandler);
        }

        PluginCommand hubberCmd = getCommand("hubber");
        if (hubberCmd != null) {
            HubberCommand handler = new HubberCommand(this, configManager, spawnManager, collisionManager, bookManager, triggerManager, npcManager);
            hubberCmd.setExecutor(handler);
            hubberCmd.setTabCompleter(handler);
        }
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerJoinListener(this, configManager, spawnManager, collisionManager, bookManager), this);
        pm.registerEvents(new ProtectionListener(configManager, spawnManager), this);
        pm.registerEvents(new BookListener(configManager, bookManager), this);
        pm.registerEvents(new TriggerListener(configManager, spawnManager, triggerManager), this);
        pm.registerEvents(new NpcListener(npcManager), this);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public CollisionManager getCollisionManager() {
        return collisionManager;
    }

    public BookManager getBookManager() {
        return bookManager;
    }

    public TriggerManager getTriggerManager() {
        return triggerManager;
    }

    public NpcManager getNpcManager() {
        return npcManager;
    }

    public ServerMonitor getServerMonitor() {
        return serverMonitor;
    }
}
