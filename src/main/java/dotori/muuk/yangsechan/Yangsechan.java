package dotori.muuk.yangsechan;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class Yangsechan extends JavaPlugin {

    public static Plugin plugin;

    @Override
    public void onEnable() {
        plugin = this;
        getCommand("양세찬게임").setExecutor(new Starter());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
