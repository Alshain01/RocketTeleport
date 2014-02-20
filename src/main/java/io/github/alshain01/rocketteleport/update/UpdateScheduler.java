package io.github.alshain01.rocketteleport.update;

import io.github.alshain01.rocketteleport.Message;
import io.github.alshain01.rocketteleport.update.Updater.UpdateResult;
import io.github.alshain01.rocketteleport.update.Updater.UpdateType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public class UpdateScheduler extends BukkitRunnable {
    private final Updater.UpdateType type;
    private final File file, dataFolder;
    private final String version, updateFolder, key;
    private final List<String> authors;
    private final Logger logger;

    private Updater updater = null;

    public UpdateScheduler(Plugin plugin, File file, ConfigurationSection config) {
        // Move everything out of bukkit so we can run async
        this.file = file;
        this.type = config.getBoolean("Download") ? UpdateType.DEFAULT : UpdateType.NO_DOWNLOAD;
        this.key = config.getString("ServerModsAPIKey");
        this.dataFolder = plugin.getDataFolder();
        this.version = plugin.getDescription().getVersion();
        this.updateFolder = plugin.getServer().getUpdateFolder();
        this.authors = plugin.getDescription().getAuthors();
        this.logger = plugin.getLogger();
    }

    UpdateResult getResult() {
        return updater.getResult();
    }

    @Override
    public void run() {
        final int PLUGIN_ID = 70281;
        updater = new Updater(authors, dataFolder, updateFolder, version, logger, PLUGIN_ID, file, type, key, true);

        if (updater.getResult() == UpdateResult.UPDATE_AVAILABLE) {
            logger.info("The version of RocketTeleport that this server is running is out of date. "
                    + "Please consider updating to the latest version at dev.bukkit.org/bukkit-plugins/rocketteleport/.");
        } else if (updater.getResult() == UpdateResult.SUCCESS) {
            logger.info("An update to RocketTeleport has been downloaded and will be installed when the server is reloaded.");
        }
    }
}
