package dotori.muuk.yangsechan.discord;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

// LinkCommand.java (새 파일)
public class LinkCommand implements CommandExecutor {
    private final LinkManager linkManager;

    public LinkCommand(LinkManager linkManager) {
        this.linkManager = linkManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }

        String authCode = linkManager.generateAuthCode(player.getUniqueId());
        
        player.sendMessage(Component.text("--- 디스코드 연동 ---", NamedTextColor.AQUA));
        player.sendMessage(Component.text("디스코드에서 아래 명령어를 입력해주세요:", NamedTextColor.GRAY));
        
        Component commandMessage = Component.text("/연동 코드:" + authCode, NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.copyToClipboard("/연동 코드:" + authCode))
                .hoverEvent(HoverEvent.showText(Component.text("클릭하여 복사")));
        
        player.sendMessage(commandMessage);
        player.sendMessage(Component.text("--------------------", NamedTextColor.AQUA));
        
        return true;
    }
}