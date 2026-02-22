package org.cubeRhythm.manager;

import org.bukkit.entity.Player;
import org.cubeRhythm.chart.Chart;
import org.cubeRhythm.game.GameSession;
import org.cubeRhythm.game.PlayerSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.cubeRhythm.Main.instance;

/**
 * 游戏管理器
 * 管理所有玩家的游戏会话
 */
public class GameManager {
    private final Logger logger;
    private final Map<UUID, GameSession> activeSessions;
    private final SongManager songManager;

    public GameManager(SongManager songManager) {
        this.logger = instance.getLogger();
        this.activeSessions = new HashMap<>();
        this.songManager = songManager;
    }

    /**
     * 为玩家创建并启动游戏会话
     * @param player 玩家
     * @param chartId 谱面 ID
     * @param settings 玩家设置
     * @return 是否成功创建
     */
    public boolean startGame(Player player, String chartId, PlayerSettings settings) {
        UUID playerId = player.getUniqueId();

        // 检查玩家是否已经在游戏中
        if (activeSessions.containsKey(playerId)) {
            player.sendMessage("§c你已经在游戏中了！");
            return false;
        }

        // 加载谱面
        Chart chart = songManager.loadChart(chartId);
        if (chart == null) {
            player.sendMessage("§c无法加载谱面: " + chartId);
            return false;
        }

        // 创建游戏会话
        GameSession session = new GameSession(player, chart, settings);
        activeSessions.put(playerId, session);

        // 启动游戏
        session.start();
        logger.info("玩家 " + player.getName() + " 开始游戏: " + chartId);

        return true;
    }

    /**
     * 结束玩家的游戏会话
     * @param player 玩家
     */
    public void endGame(Player player) {
        UUID playerId = player.getUniqueId();
        GameSession session = activeSessions.get(playerId);

        if (session != null) {
            session.end();
            activeSessions.remove(playerId);
            logger.info("玩家 " + player.getName() + " 结束游戏");
        }
    }

    /**
     * 停止玩家的游戏会话（不显示结果）
     * @param player 玩家
     */
    public void stopGame(Player player) {
        UUID playerId = player.getUniqueId();
        GameSession session = activeSessions.get(playerId);

        if (session != null) {
            session.stop();
            activeSessions.remove(playerId);
            logger.info("玩家 " + player.getName() + " 停止游戏");
        }
    }

    /**
     * 暂停玩家的游戏
     * @param player 玩家
     */
    public void pauseGame(Player player) {
        GameSession session = getSession(player);
        if (session != null) {
            session.pause();
            player.sendMessage("§e游戏已暂停");
        }
    }

    /**
     * 恢复玩家的游戏
     * @param player 玩家
     */
    public void resumeGame(Player player) {
        GameSession session = getSession(player);
        if (session != null) {
            session.resume();
            player.sendMessage("§a游戏已恢复");
        }
    }

    /**
     * 获取玩家的游戏会话
     * @param player 玩家
     * @return 游戏会话，如果不存在返回 null
     */
    public GameSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    /**
     * 检查玩家是否在游戏中
     * @param player 玩家
     * @return 是否在游戏中
     */
    public boolean isInGame(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    /**
     * 获取当前活跃的游戏会话数量
     * @return 会话数量
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * 清理所有游戏会话（插件卸载时调用）
     */
    public void cleanup() {
        for (GameSession session : activeSessions.values()) {
            session.stop();
        }
        activeSessions.clear();
        logger.info("已清理所有游戏会话");
    }
}
