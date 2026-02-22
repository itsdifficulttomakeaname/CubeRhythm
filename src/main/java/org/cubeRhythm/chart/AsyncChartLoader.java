package org.cubeRhythm.chart;

import cn.jason31416.planetlib.PlanetLib;
import org.cubeRhythm.Main;
import org.cubeRhythm.note.Note;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Asynchronous chart loader for improved performance
 * Loads and sorts charts in background threads to avoid blocking the main thread
 */
public class AsyncChartLoader {

    /**
     * Load a chart asynchronously
     */
    public static CompletableFuture<Chart> loadChartAsync(File chartFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ChartLoader.loadChart(chartFile);
            } catch (IOException e) {
                Main.instance.getLogger().severe("Failed to load chart asynchronously: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Load a chart asynchronously with callback
     */
    public static void loadChartAsync(File chartFile, Consumer<Chart> onSuccess, Consumer<Exception> onError) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return ChartLoader.loadChart(chartFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).thenAcceptAsync(chart -> {
            // Run callback on main thread
            PlanetLib.getScheduler().runNextTick(wrappedTask -> onSuccess.accept(chart));
        }).exceptionally(throwable -> {
            // Run error callback on main thread
            PlanetLib.getScheduler().runNextTick(wrappedTask -> onError.accept((Exception) throwable.getCause()));
            return null;
        });
    }

    /**
     * Sort notes asynchronously by time
     */
    public static CompletableFuture<List<Note>> sortNotesAsync(List<Note> notes) {
        return CompletableFuture.supplyAsync(() -> {
            Collections.sort(notes, (a, b) -> Double.compare(a.getTime(), b.getTime()));
            return notes;
        });
    }

    /**
     * Preload multiple charts asynchronously
     */
    public static CompletableFuture<Void> preloadChartsAsync(List<File> chartFiles, Consumer<Chart> onEachLoaded) {
        CompletableFuture<?>[] futures = chartFiles.stream()
                .map(file -> loadChartAsync(file).thenAccept(chart -> {
                    if (chart != null) {
                        PlanetLib.getScheduler().runNextTick(wrappedTask -> onEachLoaded.accept(chart));
                    }
                }))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }
}