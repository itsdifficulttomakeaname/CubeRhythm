package org.cubeRhythm.editor;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.cubeRhythm.coordinate.Face;
import org.cubeRhythm.coordinate.NotePosition;
import org.cubeRhythm.note.NoteType;

import java.util.UUID;

/**
 * 编辑器事件监听器 - 处理鼠标滚轮、点击、按键等交互
 *
 * @deprecated 编辑器功能已废弃，建议使用外部工具编辑铺面。详见 EDITOR_DESIGN.md
 */
@Deprecated
public class EditorListener implements Listener {
    private final EditorManager editorManager;
    private static final double JUDGMENT_DISTANCE = 4.0;  // 判定面距离

    public EditorListener() {
        this.editorManager = EditorManager.getInstance();
    }

    /**
     * 处理F键切换发光状态（通过交换手持物品事件检测）
     */
    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        // 检查是否在编辑模式
        if (!editorManager.isInEditorMode(player)) {
            return;
        }

        EditorSession session = editorManager.getSession(player);
        if (session == null) {
            return;
        }

        // 取消事件，防止实际交换物品
        event.setCancelled(true);

        // 切换发光状态
        session.toggleGlowing();

        // 播放音效
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 2.0f);

        // 显示标题
        String subtitle = session.isGlowing() ? "§7发光: §a开" : "§7发光: §c关";
        player.sendTitle("", subtitle, 0, 10, 10);
    }

    /**
     * 处理右键点击（放置音符）和左键点击（删除音符）
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // 检查是否在编辑模式
        if (!editorManager.isInEditorMode(player)) {
            return;
        }

        EditorSession session = editorManager.getSession(player);
        if (session == null) {
            return;
        }

        // 检查是否持有魔杖
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.STICK) {
            return;
        }

        Action action = event.getAction();

        // 右键放置音符
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            handleNotePlacement(player, session);
        }
        // 左键删除音符
        else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            handleNoteDeletion(player, session);
        }
    }

    /**
     * 处理音符放置
     */
    private void handleNotePlacement(Player player, EditorSession session) {
        // 使用射线追踪找到判定面上的位置
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        // 计算射线与判定面的交点
        double playerZ = eyeLocation.getZ();
        double rate = Math.abs(JUDGMENT_DISTANCE - playerZ) / direction.getZ();

        double targetX = direction.getX() * rate + eyeLocation.getX();
        double targetY;

        // 如果按住Shift，对齐到0.5格网格
        if (player.isSneaking()) {
            targetY = direction.getY() * rate + eyeLocation.getY();
            targetX = Math.round(targetX * 2) / 2.0;
            targetY = Math.round(targetY * 2) / 2.0;
        } else {
            targetY = direction.getY() * rate + eyeLocation.getY();
        }

        // 检查坐标是否在有效范围内 (-3 到 4)
        if (targetX < -3 || targetX > 4 || targetY < -3 || targetY > 4) {
            player.sendMessage("§c位置超出范围！");
            return;
        }

        // 转换为音符坐标（相对于判定面中心）
        // 游戏中音符坐标是相对于判定面的，需要转换
        double noteX = targetX;
        double noteY = targetY;

        // 显示坐标反馈
        player.sendTitle("", String.format("§7%.1f, %.1f", noteX, noteY), 0, 10, 10);

        // 创建音符
        UUID noteId = UUID.randomUUID();
        EditorNote note = new EditorNote();
        note.setId(noteId.toString());
        note.setType(session.getCurrentNoteType());
        note.setTime(session.getCurrentTime());
        note.setFace(session.getCurrentFace());
        note.setGlowing(session.isGlowing());
        note.setTag("");

        // 根据音符类型设置位置
        if (note.getType() == NoteType.FLICK) {
            // FLICK音符在中心，不需要位置
            note.setPosition(null);
            note.setTurn(session.getFlickDirection());
        } else if (note.getType() == NoteType.DOUBLE) {
            // DOUBLE音符需要两个位置（暂时只放置一个，需要用户再次点击放置第二个）
            // 这里简化处理，放置在同一位置
            note.getPositions().add(new NotePosition((int)noteX, (int)noteY));
            note.getPositions().add(new NotePosition((int)noteX + 1, (int)noteY));
        } else {
            // 普通音符
            note.setPosition(new NotePosition((int)noteX, (int)noteY));
        }

        // 添加到会话
        session.addNote(note);

        // 保存到文件
        if (session.getChartFile() != null) {
            EditorFileUtil.saveSession(session);
        }

        // 重新渲染
        EditorNoteRenderer.renderVisibleNotes(session, player);

        // 播放音效
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
    }

    /**
     * 处理音符删除
     */
    private void handleNoteDeletion(Player player, EditorSession session) {
        // 使用射线追踪找到最近的音符
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        // 计算射线与判定面的交点
        double playerZ = eyeLocation.getZ();
        double rate = Math.abs(JUDGMENT_DISTANCE - playerZ) / direction.getZ();

        double targetX = direction.getX() * rate + eyeLocation.getX();
        double targetY = direction.getY() * rate + eyeLocation.getY();

        // 查找最近的音符（在1格范围内）
        EditorNote nearestNote = null;
        double minDistance = 1.0;

        for (EditorNote note : session.getNotes().values()) {
            if (note.getPosition() != null) {
                double noteX = note.getPosition().getX();
                double noteY = note.getPosition().getY();
                double distance = Math.sqrt(Math.pow(noteX - targetX, 2) + Math.pow(noteY - targetY, 2));

                if (distance < minDistance) {
                    minDistance = distance;
                    nearestNote = note;
                }
            }
        }

        if (nearestNote != null) {
            // 删除音符
            session.removeNote(nearestNote.getId());

            // 保存到文件
            if (session.getChartFile() != null) {
                EditorFileUtil.saveSession(session);
            }

            // 重新渲染
            EditorNoteRenderer.renderVisibleNotes(session, player);

            // 播放音效
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);

            player.sendMessage("§c已删除音符");
        } else {
            player.sendMessage("§c未找到附近的音符");
        }
    }

    /**
     * 处理鼠标滚轮事件（通过切换物品栏槽位检测）
     */
    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        // 检查是否在编辑模式
        if (!editorManager.isInEditorMode(player)) {
            return;
        }

        EditorSession session = editorManager.getSession(player);
        if (session == null) {
            return;
        }

        // 取消事件，防止实际切换槽位
        event.setCancelled(true);

        int newSlot = event.getNewSlot();

        // 根据新槽位执行不同操作
        if (newSlot >= 1 && newSlot <= 3) {
            // Slots 1-3: 后退一拍
            handleBeatNavigation(player, session, false);
        } else if (newSlot >= 5 && newSlot <= 7) {
            // Slots 5-7: 前进一拍
            handleBeatNavigation(player, session, true);
        } else if (newSlot == 0) {
            // Slot 1: 循环切换音符类型
            handleNoteTypeCycle(player, session);
        } else if (newSlot == 8) {
            // Slot 8 (实际是9): 循环切换判定面
            handleFaceCycle(player, session);
        }

        // 操作完成后，切回槽位 4 (索引从0开始，所以是4)
        player.getInventory().setHeldItemSlot(4);
    }

    /**
     * 处理拍数导航（前进/后退一步）
     */
    private void handleBeatNavigation(Player player, EditorSession session, boolean forward) {
        int amount = forward ? 1 : -1;
        session.addTick(amount);  // 移动一步，而不是一整拍

        // 更新音符渲染
        EditorNoteRenderer.renderVisibleNotes(session, player);

        // 播放音效
        float pitch = forward ? 2.0f : 1.5f;
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, pitch);

        // 显示标题
        int beat = session.getCurrentBeat();
        int beatPos = session.getCurrentBeatPosition();
        int stepLength = session.getStepLength();
        double time = session.getCurrentTime();

        String arrow = forward ? "§a>>>" : "§a<<<";
        String subtitle = String.format("%s §7§o#%d §f%d§7/%d §7(%.2fs) %s",
                arrow, beat, beatPos, stepLength, time, arrow);
        player.sendTitle("", subtitle, 0, 10, 10);
    }

    /**
     * 处理音符类型循环切换
     */
    private void handleNoteTypeCycle(Player player, EditorSession session) {
        session.cycleNoteType();

        // 更新物品显示
        updateNoteTypeItem(player, session.getCurrentNoteType());

        // 播放音效
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 2.0f);

        // 显示标题
        String subtitle = getNoteTypeDisplayName(session.getCurrentNoteType(), session.getFlickDirection());
        player.sendTitle("", subtitle, 0, 10, 10);
    }

    /**
     * 处理判定面循环切换
     */
    private void handleFaceCycle(Player player, EditorSession session) {
        session.cycleFace();

        // 更新物品显示
        updateFaceItem(player, session.getCurrentFace());

        // 播放音效
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 2.0f);

        // 显示标题
        String subtitle = getFaceDisplayName(session.getCurrentFace());
        player.sendTitle("", subtitle, 0, 10, 10);
    }

    /**
     * 更新音符类型物品
     */
    private void updateNoteTypeItem(Player player, NoteType type) {
        EditorSession session = editorManager.getSession(player);
        if (session == null) return;

        Material material = switch (type) {
            case TAP -> Material.LIGHT_BLUE_WOOL;
            case DOUBLE -> Material.ORANGE_WOOL;
            case DRAG -> Material.YELLOW_WOOL;
            case HOLD -> Material.WHITE_WOOL;
            case FLICK -> session.getFlickDirection().equals("left") ? Material.MAGENTA_WOOL : Material.RED_WOOL;
            default -> Material.LIGHT_BLUE_WOOL;
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b音符类型: " + getNoteTypeDisplayName(type, session.getFlickDirection()));
        item.setItemMeta(meta);

        player.getInventory().setItem(0, item);
    }

    /**
     * 更新判定面物品
     */
    private void updateFaceItem(Player player, Face face) {
        Material material = switch (face) {
            case W -> Material.WHITE_STAINED_GLASS;
            case A -> Material.YELLOW_STAINED_GLASS;
            case S -> Material.ORANGE_STAINED_GLASS;
            case D -> Material.RED_STAINED_GLASS;
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§f判定面: " + getFaceDisplayName(face));
        item.setItemMeta(meta);

        player.getInventory().setItem(8, item);
    }

    /**
     * 获取音符类型显示名称
     */
    private String getNoteTypeDisplayName(NoteType type, String flickDirection) {
        return switch (type) {
            case TAP -> "§bTap";
            case DOUBLE -> "§6Double";
            case DRAG -> "§eDrag";
            case HOLD -> "§fHold";
            case FLICK -> flickDirection.equals("left") ? "§a← §dFlick" : "§cFlick §a→";
            default -> "§7Unknown";
        };
    }

    /**
     * 获取判定面显示名称
     */
    private String getFaceDisplayName(Face face) {
        return switch (face) {
            case W -> "§f前 (W)";
            case A -> "§e左 (A)";
            case S -> "§6后 (S)";
            case D -> "§c右 (D)";
        };
    }
}
