package org.cubeRhythm;

import cn.jason31416.planetlib.PlanetLib;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeRhythm.chart.ChartRegistry;
import org.cubeRhythm.command.ExitCommand;
import org.cubeRhythm.command.GUICommand;
import org.cubeRhythm.command.PlayCommand;
import org.cubeRhythm.game.GameSession;
import org.cubeRhythm.game.MovementRestriction;
import org.cubeRhythm.gui.GUIListener;
import org.cubeRhythm.command.ReloadCommand;
import org.cubeRhythm.manager.ConfigManager;
import org.cubeRhythm.manager.OffsetConfig;
import org.cubeRhythm.manager.PlayerSettingsManager;

import java.util.Objects;

@Getter public final class Main extends JavaPlugin {
    public static Main instance;
    private ChartRegistry chartRegistry;
    private PlayerSettingsManager playerSettingsManager;
    private GUIListener guiListener;
    private ConfigManager configManager;

    @Setter @Getter private GameSession currentSession;

    @Override public void onEnable() {
        instance = this;
        PlanetLib.initialize(instance);

        // Initialize config manager
        configManager = new ConfigManager();
        OffsetConfig.init();

        // Initialize player settings manager
        playerSettingsManager = new PlayerSettingsManager(this);

        // Initialize chart registry
        chartRegistry = new ChartRegistry();
        chartRegistry.loadAllCharts();

        // Initialize and register GUI listener
        guiListener = new GUIListener(this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        // Register movement restriction listener
        getServer().getPluginManager().registerEvents(new MovementRestriction(), this);

        // DEPRECATED: Editor functionality has been disabled due to technical limitations
        // See EDITOR_DESIGN.md for details and alternative approaches
        // Register editor listener
        // getServer().getPluginManager().registerEvents(new org.cubeRhythm.editor.EditorListener(), this);

        // Start editor update task (preview cursor and action bar)
        // org.cubeRhythm.editor.EditorUpdateTask.start(org.cubeRhythm.editor.EditorManager.getInstance());

        // Register commands
        Objects.requireNonNull(getCommand("play")).setExecutor(new PlayCommand(chartRegistry));
        Objects.requireNonNull(getCommand("exit")).setExecutor(new ExitCommand());
        Objects.requireNonNull(getCommand("gui")).setExecutor(new GUICommand());
        Objects.requireNonNull(getCommand("creload")).setExecutor(new ReloadCommand());

        // DEPRECATED: Editor commands disabled
        // Objects.requireNonNull(getCommand("editor")).setExecutor(new org.cubeRhythm.editor.EditorCommand());
        // Objects.requireNonNull(getCommand("step")).setExecutor(new org.cubeRhythm.editor.StepCommand());
        // Objects.requireNonNull(getCommand("b")).setExecutor(new org.cubeRhythm.editor.BeatCommand());

        getLogger().info("CubeRhythm has been enabled!");
        getLogger().info("Loaded " + chartRegistry.getChartCount() + " charts");
    }

    @Override public void onDisable() {
        // 清理当前会话
        if (currentSession != null) {
            currentSession.stop();
            currentSession = null;
        }

        // DEPRECATED: Editor cleanup disabled
        // 清理编辑器会话
        // org.cubeRhythm.editor.EditorManager.getInstance().cleanup();

        getLogger().info("CubeRhythm has been disabled!");
    }
}
