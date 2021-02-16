package ru.armagidon.dokitux;

import org.bukkit.plugin.java.JavaPlugin;
import ru.armagidon.dokitux.commands.PluginCommand;
import ru.armagidon.dokitux.commands.DoasCommand;
import ru.armagidon.dokitux.pluginmanagement.PluginFileManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Dokitux extends JavaPlugin {

    public static final ExecutorService PLUGIN_THREAD_POOL = Executors.newCachedThreadPool();

    private PluginFileManager fileManager;

    @Override
    public void onEnable() {
        getLogger().info("Creating data folder: "+getDataFolder().mkdirs());
        new PluginCommand(fileManager);
        new DoasCommand();

    }

    @Override
    public void onDisable() {}

    @Override
    public void onLoad() {
        fileManager = new PluginFileManager(this);
    }
}
