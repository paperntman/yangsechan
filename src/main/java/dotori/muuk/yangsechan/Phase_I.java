package dotori.muuk.yangsechan;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Phase_I implements Listener {

    private static List<Player> players = null;
    private static List<Player> recipients = null;
    private static final Queue<Player> targets  = new ConcurrentLinkedQueue<>();
    private static Player target = null;
    private static String word = null;
    private static final VoteScoreboardManager scoreboardManager = new VoteScoreboardManager();
    private static final Map<UUID, NodStatus> playerStatuses = new ConcurrentHashMap<>();
    private static int wordSelectorCode = 0;
    private static final Map<Player, String > playerNames = new ConcurrentHashMap<>();

    public static void start(List<Player> players) {
        Phase_I.players = players;
        targets.clear();
        targets.addAll(players);
        nextPlayer();


    }

    @EventHandler
    private void onChat(AsyncChatEvent e) {
        if (target == null) return;
        if (!players.contains(e.getPlayer())) return;
        if (e.getPlayer().equals(target)) return;
        if (!(e.message() instanceof TextComponent component)) return;

        synchronized (this) {
            if (word != null) return;
            word = component.content();
        }

        Bukkit.getScheduler().runTask(Yangsechan.plugin, () -> {
            recipients.forEach(player -> player.showTitle(Title.title(
                    Component.text(word),
                    Component.text("끄덕이거나 ").append(
                            Component.text("고개를 저어서 "),
                            Component.text("투표하세요!")
                    ))));

            scoreboardManager.createScoreboard(recipients);
            recipients.forEach(player -> playerStatuses.put(player.getUniqueId(), NodStatus.NORMAL));
        });
    }

    @EventHandler
    private void onNod(NodEvent e){
        if (!recipients.contains(e.getPlayer()) || word == null) {
            return;
        }
        Bukkit.getScheduler().runTask(Yangsechan.plugin, () -> updatePlayerStatus(e.getPlayer(), NodStatus.NOD));
    }

    @EventHandler
    private void onShake(ShakeEvent e){
        if (!recipients.contains(e.getPlayer()) || word == null) {
            return;
        }
        Bukkit.getScheduler().runTask(Yangsechan.plugin, () -> updatePlayerStatus(e.getPlayer(), NodStatus.SHAKE));
    }

    private static void end() {
        GameManager.Phase_I_End(playerNames);
        playerNames.clear();
    }

    private static void nextPlayer(){
        target = targets.poll();
        recipients = new ArrayList<>(players);
        recipients.remove(target);
        word = null;
        scoreboardManager.cleanup();
        playerStatuses.clear();
        if (target == null) end();
        else {
            sendMessage(Component.text("다음 플레이어는 ")
                    .append(Component.text(target.getName()))
                    .append(Component.text(" 입니다! 단어를 정해주세요!"))
                    ,recipients);
        }
    }

    private static void updatePlayerStatus(Player player, NodStatus newStatus) {
        if (playerStatuses.get(player.getUniqueId()) == newStatus) return;

        playerStatuses.put(player.getUniqueId(), newStatus);
        scoreboardManager.updateScoreboard(playerStatuses);

        Collection<NodStatus> values = playerStatuses.values();

        long nodCount = values.stream().filter(status -> status.equals(NodStatus.NOD)).count();
        long shakeCount = values.stream().filter(status -> status.equals(NodStatus.SHAKE)).count();
        long size = values.size();

        /*
            자동 확정: 도리도리 상태의 플레이어가 한 명도 없고, 끄덕임 상태의 플레이어가 과반수를 넘으면, 5초의 카운트다운 후 단어가 자동으로 확정된다.
            집행 유예: 도리도리 상태의 플레이어가 한 명이라도 나타나면, 자동 확정 카운트다운은 즉시 중단된다.
            기각: 도리도리 상태의 플레이어가 과반수를 넘으면, 제안된 단어는 자동으로 기각된다.
         */
        if (nodCount == size){
            sendMessage(Component.text("만장일치로 ")
                    .append(Component.text(target.getName()))
                    .append(Component.text(" 의 단어가 "))
                    .append(Component.text(word))
                    .append(Component.text("로 선정되었습니다!"))
                    ,recipients);
            playerNames.put(target, word);
            recipients.forEach(Player::resetTitle);
            nextPlayer();
            if (wordSelectorCode != 0) {
                Bukkit.getScheduler().cancelTask(wordSelectorCode);
                wordSelectorCode = 0;
            }
        }else if (shakeCount > size/2){
            sendMessage(Component.text("과반수의 반대로 기각되었습니다!")
                    ,recipients);
            word = null;
            recipients.forEach(Player::resetTitle);
            if (wordSelectorCode != 0) {
                Bukkit.getScheduler().cancelTask(wordSelectorCode);
                wordSelectorCode = 0;
            }
        }else if (nodCount > size/2){
            if (shakeCount == 0 && wordSelectorCode == 0){
                sendMessage(Component.text("5초 동안 반대표가 없다면, 해당 단어로 확정됩니다!"), recipients);
                wordSelectorCode = Bukkit.getScheduler().runTaskLater(Yangsechan.plugin,
                        () -> {
                            sendMessage(Component.text(target.getName())
                                            .append(Component.text(" 의 단어가 "))
                                            .append(Component.text(word))
                                            .append(Component.text("로 선정되었습니다!"))
                                    , recipients);
                            playerNames.put(target, word);
                            recipients.forEach(Player::resetTitle);
                            nextPlayer();
                        }, 100).getTaskId();
            }
        }else if (wordSelectorCode != 0) {
            sendMessage(Component.text("반대표의 존재로 인해 자동 확정이 유예됩니다!"), recipients);
            if (wordSelectorCode != 0) {
                Bukkit.getScheduler().cancelTask(wordSelectorCode);
                wordSelectorCode = 0;
            }
        }
    }

    private static void sendMessage(Component text, List<Player> target){
        for (Player player : target) {
            player.sendMessage(text);
        }
    }
}
