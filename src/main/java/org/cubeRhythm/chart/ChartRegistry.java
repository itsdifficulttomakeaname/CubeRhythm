package org.cubeRhythm.chart;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static org.cubeRhythm.Main.instance;

public class ChartRegistry {
    private final Map<String, Chart> charts = new ConcurrentHashMap<>();
    private final File chartsDirectory;

    public ChartRegistry() {
        this.chartsDirectory = new File(instance.getDataFolder(), "charts");

        if (!chartsDirectory.exists()) {
            chartsDirectory.mkdirs();
        }
    }

    /**
     * 从谱面目录加载所有谱面
     */
    public void loadAllCharts() {
        charts.clear();

        File[] files = chartsDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            instance.getLogger().info("No chart files found in " + chartsDirectory.getPath());
            return;
        }

        int loaded = 0;
        for (File file : files) {
            try {
                Chart chart = ChartLoader.loadChart(file);
                charts.put(chart.getMetadata().getId(), chart);
                loaded++;
                instance.getLogger().info("Loaded chart: " + chart.getMetadata().getTitle());
            } catch (IOException e) {
                instance.getLogger().log(Level.WARNING, "Failed to load chart: " + file.getName(), e);
            }
        }

        instance.getLogger().info("Loaded " + loaded + " charts");
    }

    /**
     * 根据 ID 获取谱面
     */
    public Chart getChart(String id) {
        return charts.get(id);
    }

    /**
     * 获取所有已加载的谱面
     */
    public Collection<Chart> getAllCharts() {
        return charts.values();
    }

    /**
     * 获取所有谱面 ID
     */
    public Set<String> getChartIds() {
        return charts.keySet();
    }

    /**
     * 检查谱面是否存在
     */
    public boolean hasChart(String id) {
        return charts.containsKey(id);
    }

    /**
     * 重新加载特定谱面
     */
    public boolean reloadChart(String id) {
        File file = new File(chartsDirectory, id + ".json");
        if (!file.exists()) {
            return false;
        }

        try {
            Chart chart = ChartLoader.loadChart(file);
            charts.put(chart.getMetadata().getId(), chart);
            instance.getLogger().info("Reloaded chart: " + chart.getMetadata().getTitle());
            return true;
        } catch (IOException e) {
            instance.getLogger().log(Level.WARNING, "Failed to reload chart: " + id, e);
            return false;
        }
    }

    /**
     * 获取谱面数量
     */
    public int getChartCount() {
        return charts.size();
    }
}
