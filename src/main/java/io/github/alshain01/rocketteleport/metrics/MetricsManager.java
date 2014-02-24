package io.github.alshain01.rocketteleport.metrics;

import io.github.alshain01.rocketteleport.RocketTeleport;
import io.github.alshain01.rocketteleport.Rocket.RocketType;
import io.github.alshain01.rocketteleport.metrics.Metrics.Graph;

import java.io.IOException;

public class MetricsManager {
    public static void StartMetrics(final RocketTeleport plugin) {
        try {
            final Metrics metrics = new Metrics(plugin);

            /*
			 * Rocket Type Graph
			 */
            Graph graph = metrics.createGraph("Rocket Types");
            for(final RocketType r : plugin.getRocketCount().keySet())
            graph.addPlotter(new Metrics.Plotter(r.getName()) {
                @Override
                public int getValue() {
                    return plugin.getRocketCount().get(r);
                }
            });

            /*
			 * Auto Update settings
			 */
            graph = metrics.createGraph("Update Configuration");
            if (!plugin.getConfig().getBoolean("Update.Check")) {
                graph.addPlotter(new Metrics.Plotter("No Updates") {
                    @Override
                    public int getValue() {
                        return 1;
                    }
                });
            } else if (!plugin.getConfig().getBoolean("Update.Download")) {
                graph.addPlotter(new Metrics.Plotter("Check for Updates") {
                    @Override
                    public int getValue() {
                        return 1;
                    }
                });
            } else {
                graph.addPlotter(new Metrics.Plotter("Download Updates") {
                    @Override
                    public int getValue() {
                        return 1;
                    }
                });
            }

            metrics.start();
        } catch (final IOException e) {
            plugin.getLogger().info(e.getMessage());
        }
    }
}
