package org.cubeRhythm.editor;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.cubeRhythm.Main;

import java.util.Arrays;

/**
 * 编辑器命令 - /editor
 *
 * @deprecated 编辑器功能已废弃，建议使用外部工具编辑铺面。详见 EDITOR_DESIGN.md
 */
@Deprecated
public class EditorCommand implements CommandExecutor {
    private final EditorManager editorManager;

    public EditorCommand() {
        this.editorManager = EditorManager.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令");
            return true;
        }

        // 无参数 - 切换编辑模式
        if (args.length == 0) {
            if (editorManager.isInEditorMode(player)) {
                exitEditorMode(player);
            } else {
                enterEditorMode(player);
            }
            return true;
        }

        // 带参数的子命令
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                showHelp(player);
                break;

            case "new":
                if (args.length < 2) {
                    player.sendMessage("§c用法: /editor new <谱面ID>");
                    return true;
                }
                createNewChart(player, args[1]);
                break;

            case "load":
                if (args.length < 2) {
                    player.sendMessage("§c用法: /editor load <谱面ID>");
                    return true;
                }
                loadChart(player, args[1]);
                break;

            case "save":
                saveChart(player);
                break;

            case "bpm":
                if (args.length < 2) {
                    player.sendMessage("§c用法: /editor bpm <数值>");
                    return true;
                }
                setBpm(player, args[1]);
                break;

            case "pretime":
                if (args.length < 2) {
                    player.sendMessage("§c用法: /editor pretime <秒数>");
                    return true;
                }
                setPreTime(player, args[1]);
                break;

            case "speed":
                if (args.length < 2) {
                    player.sendMessage("§c用法: /editor speed <倍速>");
                    return true;
                }
                setSpeed(player, args[1]);
                break;

            default:
                player.sendMessage("§c未知子命令: " + subCommand);
                player.sendMessage("§7使用 §f/editor help §7查看帮助");
                break;
        }

        return true;
    }

    /**
     * 进入编辑模式
     */
    private void enterEditorMode(Player player) {
        EditorSession session = editorManager.createSession(player);

        player.sendMessage("§8§m一一一一一一一一一一一一一一一一一一一一一一一一一一一一一一一一");
        player.sendMessage("");
        player.sendMessage("§a进入编辑模式");
        player.sendMessage("");
        player.sendMessage("§6基本命令:");
        player.sendMessage("  §f/editor help §7- 查看完整帮助");
        player.sendMessage("  §f/editor new <ID> §7- 创建新谱面");
        player.sendMessage("  §f/editor load <ID> §7- 加载现有谱面");
        player.sendMessage("  §f/editor save §7- 保存当前谱面");
        player.sendMessage("  §f/editor §7- 退出编辑模式");
        player.sendMessage("");
        player.sendMessage("§7请先使用 §f/editor new <ID> §7创建新谱面");
        player.sendMessage("§7或使用 §f/editor load <ID> §7加载现有谱面");
        player.sendMessage("");
        player.sendMessage("§8§m一一一一一一一一一一一一一一一一一一一一一一一一一一一一一一一一");

        // 给予编辑工具
        giveEditorTools(player);
    }

    /**
     * 退出编辑模式
     */
    private void exitEditorMode(Player player) {
        EditorSession session = editorManager.getSession(player);
        if (session != null && session.getChartId() != null) {
            // 自动保存
            EditorFileUtil.saveSession(session);
        }

        editorManager.endSession(player);
        player.getInventory().clear();
        player.sendMessage("§c离开编辑模式");
    }

    /**
     * 创建新谱面
     */
    private void createNewChart(Player player, String chartId) {
        if (!editorManager.isInEditorMode(player)) {
            player.sendMessage("§c请先进入编辑模式");
            return;
        }

        // 检查ID是否已存在
        if (EditorFileUtil.chartExists(chartId)) {
            player.sendMessage("§c谱面ID已存在: " + chartId);
            player.sendMessage("§7使用 §f/editor load " + chartId + " §7加载现有谱面");
            return;
        }

        EditorSession session = editorManager.getSession(player);
        session.setChartId(chartId);

        // 立即创建文件
        EditorFileUtil.saveSession(session);

        player.sendMessage("§a已创建新谱面: " + chartId);
        player.sendMessage("§7谱面将自动保存到文件");
    }

    /**
     * 加载谱面
     */
    private void loadChart(Player player, String chartId) {
        if (!editorManager.isInEditorMode(player)) {
            player.sendMessage("§c请先进入编辑模式");
            return;
        }

        EditorSession session = editorManager.getSession(player);
        if (EditorFileUtil.loadChartToEditor(session, chartId)) {
            player.sendMessage("§a已加载谱面: " + chartId + " (共 " + session.getNotes().size() + " 个音符)");
            player.sendMessage("§7BPM: " + session.getBpm());
            player.sendMessage("§7PreTime: " + session.getPreTime() + "s");
        } else {
            player.sendMessage("§c加载谱面失败: " + chartId);
        }
    }

    /**
     * 保存谱面
     */
    private void saveChart(Player player) {
        if (!editorManager.isInEditorMode(player)) {
            player.sendMessage("§c你不在编辑模式");
            return;
        }

        EditorSession session = editorManager.getSession(player);
        if (session.getChartId() == null) {
            player.sendMessage("§c请先使用 /editor new <ID> 创建谱面");
            return;
        }

        if (EditorFileUtil.saveSession(session)) {
            player.sendMessage("§a谱面已保存: " + session.getChartId());
        } else {
            player.sendMessage("§c保存失败");
        }
    }

    /**
     * 设置BPM
     */
    private void setBpm(Player player, String value) {
        if (!editorManager.isInEditorMode(player)) {
            player.sendMessage("§c你不在编辑模式");
            return;
        }

        try {
            double bpm = Double.parseDouble(value);
            if (bpm <= 0) {
                player.sendMessage("§cBPM必须大于0");
                return;
            }

            EditorSession session = editorManager.getSession(player);
            session.setBpm(bpm);
            player.sendMessage("§aBPM已设置为: " + bpm);

            // 自动保存
            if (session.getChartId() != null) {
                EditorFileUtil.saveSession(session);
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的数值: " + value);
        }
    }

    /**
     * 设置PreTime
     */
    private void setPreTime(Player player, String value) {
        if (!editorManager.isInEditorMode(player)) {
            player.sendMessage("§c你不在编辑模式");
            return;
        }

        try {
            double preTime = Double.parseDouble(value);
            EditorSession session = editorManager.getSession(player);
            session.setPreTime(preTime);
            player.sendMessage("§aPreTime已设置为: " + preTime + "s");

            // 自动保存
            if (session.getChartId() != null) {
                EditorFileUtil.saveSession(session);
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的数值: " + value);
        }
    }

    /**
     * 设置流速
     */
    private void setSpeed(Player player, String value) {
        if (!editorManager.isInEditorMode(player)) {
            player.sendMessage("§c你不在编辑模式");
            return;
        }

        try {
            double speed = Double.parseDouble(value);
            if (speed <= 0) {
                player.sendMessage("§c流速必须大于0");
                return;
            }

            EditorSession session = editorManager.getSession(player);
            session.setSpeed(speed);
            player.sendMessage("§a流速已设置为: " + speed);
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的数值: " + value);
        }
    }

    /**
     * 显示帮助信息
     */
    private void showHelp(Player player) {
        player.sendMessage("§8§m一一一一一一一一一一一一一一一一一一一一一一一一一一一一一一一一");
        player.sendMessage("");
        player.sendMessage("§6CubeRhythm 编辑器帮助");
        player.sendMessage("");
        player.sendMessage("§6文件管理:");
        player.sendMessage("  §f/editor new <ID> §7- 创建新谱面");
        player.sendMessage("  §f/editor load <ID> §7- 加载现有谱面");
        player.sendMessage("  §f/editor save §7- 手动保存谱面");
        player.sendMessage("");
        player.sendMessage("§6参数设置:");
        player.sendMessage("  §f/editor bpm <数值> §7- 设置BPM");
        player.sendMessage("  §f/editor pretime <秒数> §7- 设置第一拍前的时间");
        player.sendMessage("  §f/editor speed <倍速> §7- 设置流速");
        player.sendMessage("");
        player.sendMessage("§6时间轴操作:");
        player.sendMessage("  §f/step <整数> §7- 设置步长 (如 /step 4 表示1/4拍)");
        player.sendMessage("  §f/b <拍数> §7- 跳转到指定拍");
        player.sendMessage("  §3[鼠标滚轮] §7- 前进/后退一拍");
        player.sendMessage("");
        player.sendMessage("§6音符操作:");
        player.sendMessage("  §7[滚轮到1键] §7- 切换音符类型");
        player.sendMessage("  §7[滚轮到9键] §7- 切换判定面");
        player.sendMessage("  §7[滚轮到2-4] §7- 后退一拍");
        player.sendMessage("  §7[滚轮到6-8] §7- 前进一拍");
        player.sendMessage("  §7[F键] §7- 切换发光效果");
        player.sendMessage("  §7[右键魔杖] §7- 放置音符");
        player.sendMessage("  §7[左键魔杖] §7- 删除音符");
        player.sendMessage("  §7[Shift+右键] §7- 对齐到网格");
        player.sendMessage("");
        player.sendMessage("§7谱面会在每次修改后自动保存");
        player.sendMessage("");
        player.sendMessage("§8§m一一一一一一一一一一一一一一一一一一一一一一一一一一一一一一一一");
    }

    /**
     * 给予编辑工具
     */
    private void giveEditorTools(Player player) {
        player.getInventory().clear();

        // 音符类型选择器 (slot 0)
        ItemStack noteTypeItem = new ItemStack(Material.LIGHT_BLUE_WOOL);
        ItemMeta meta1 = noteTypeItem.getItemMeta();
        meta1.setDisplayName("§b音符类型: TAP");
        noteTypeItem.setItemMeta(meta1);
        player.getInventory().setItem(0, noteTypeItem);

        // 魔杖 (slot 4)
        ItemStack wand = new ItemStack(Material.STICK);
        ItemMeta meta2 = wand.getItemMeta();
        meta2.setDisplayName("§d§l魔杖");
        meta2.setLore(Arrays.asList("§e右键放置音符", "§e左键删除音符", "§eShift+右键对齐网格"));
        wand.setItemMeta(meta2);
        player.getInventory().setItem(4, wand);

        // 判定面选择器 (slot 8)
        ItemStack faceItem = new ItemStack(Material.WHITE_STAINED_GLASS);
        ItemMeta meta3 = faceItem.getItemMeta();
        meta3.setDisplayName("§f判定面: 前 (W)");
        faceItem.setItemMeta(meta3);
        player.getInventory().setItem(8, faceItem);

        player.getInventory().setHeldItemSlot(4);
    }
}
