package dotori.muuk.yangsechan;

import dotori.muuk.yangsechan.command.AdminCommand;
import dotori.muuk.yangsechan.command.AnswerCommand;
import dotori.muuk.yangsechan.command.DoriDoriCommand;
import dotori.muuk.yangsechan.command.Starter;
import dotori.muuk.yangsechan.main.GameEventListener;
import dotori.muuk.yangsechan.main.GameManager;
import dotori.muuk.yangsechan.util.HeadMovementTask;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    public static JavaPlugin plugin;
    public GameManager gameManager;
    private HeadMovementTask headMovementTask;

    @Override
    public void onEnable() {
        plugin = this;
        gameManager = new GameManager(this);
        getCommand("양세찬게임").setExecutor(new Starter(this.gameManager));
        getCommand("정답").setExecutor(new AnswerCommand(this.gameManager));
        AdminCommand adminCommand = new AdminCommand(this.gameManager);
        getCommand("yadmin").setExecutor(adminCommand);
        getCommand("yadmin").setTabCompleter(adminCommand);
        getCommand("doridori").setExecutor(new DoriDoriCommand());


        this.headMovementTask = new HeadMovementTask(this);
        this.headMovementTask.runTaskTimer(this, 0L, 1L);

        getServer().getPluginManager().registerEvents(new GameEventListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new DoriDoriCommand(), this);



    }

    @Override
    public void onDisable() {
        if (this.headMovementTask != null) {
            this.headMovementTask.cancel();
        }
    }
}
