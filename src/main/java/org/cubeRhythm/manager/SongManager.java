package org.cubeRhythm.manager;

import lombok.Getter;
import org.cubeRhythm.chart.Chart;
import org.cubeRhythm.chart.ChartLoader;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static org.cubeRhythm.Main.instance;

/**
 * 谱面管理器
 * 负责加载、缓存和管理所有谱面
 */
public class SongManager {
    private final Logger logger;
    /**
     * -- GETTER --
     *  获取谱面文件夹路径
     *
     */
    @Getter
    private final File chartsFolder;
    private final Map<String, Chart> chartCache;
    private final List<String> availableCharts;

    public SongManager() {
        this.logger = instance.getLogger();
        this.chartsFolder = new File(instance.getDataFolder(), "charts");
        this.chartCache = new HashMap<>();
        this.availableCharts = new ArrayList<>();

        // 确保谱面文件夹存在
        if (!chartsFolder.exists()) {
            chartsFolder.mkdirs();
            logger.info("创建谱面文件夹: " + chartsFolder.getPath());
        }

        // 扫描可用谱面
        scanCharts();
    }

    /**
     * 扫描谱面文件夹，查找所有可用的谱面
     */
    public void scanCharts() {
        availableCharts.clear();
        File[] files = chartsFolder.listFiles((dir, name) -> name.endsWith(".json"));

        if (files != null) {
            for (File file : files) {
                String chartId = file.getName().replace(".json", "");
                availableCharts.add(chartId);
            }
            logger.info("找到 " + availableCharts.size() + " 个谱面");
        }
    }

    /**
     * 加载谱面
     * @param chartId 谱面 ID（不含 .json 后缀）
     * @return 加载的谱面，如果失败返回 null
     */
    public Chart loadChart(String chartId) {
        // 检查缓存
        if (chartCache.containsKey(chartId)) {
            return chartCache.get(chartId);
        }

        // 从文件加载
        File chartFile = new File(chartsFolder, chartId + ".json");
        if (!chartFile.exists()) {
            logger.warning("谱面文件不存在: " + chartId);
            return null;
        }

        try {
            Chart chart = ChartLoader.loadChart(chartFile);
            chartCache.put(chartId, chart);
            logger.info("成功加载谱面: " + chartId);
            return chart;
        } catch (IOException e) {
            logger.severe("加载谱面失败: " + chartId + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 重新加载谱面（清除缓存并重新加载）
     * @param chartId 谱面 ID
     * @return 重新加载的谱面
     */
    public Chart reloadChart(String chartId) {
        chartCache.remove(chartId);
        return loadChart(chartId);
    }

    /**
     * 获取所有可用的谱面 ID 列表
     * @return 谱面 ID 列表
     */
    public List<String> getAvailableCharts() {
        return new ArrayList<>(availableCharts);
    }

    /**
     * 检查谱面是否存在
     * @param chartId 谱面 ID
     * @return 是否存在
     */
    public boolean chartExists(String chartId) {
        return availableCharts.contains(chartId);
    }

    /**
     * 清除所有缓存
     */
    public void clearCache() {
        chartCache.clear();
        logger.info("已清除谱面缓存");
    }

    /**
     * 获取缓存的谱面数量
     * @return 缓存数量
     */
    public int getCacheSize() {
        return chartCache.size();
    }

}
