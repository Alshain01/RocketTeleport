package io.github.alshain01.rocketteleport;

import java.util.*;

import io.github.alshain01.flags.Flags;
import io.github.alshain01.flags.ModuleYML;

import io.github.alshain01.rocketteleport.metrics.MetricsManager;
import io.github.alshain01.rocketteleport.update.UpdateScheduler;
import io.github.alshain01.rocketteleport.update.UpdateListener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class RocketTeleport extends JavaPlugin {
    static CustomYML message;  // Static for enumeration access
	LaunchPad launchPad;

    @Override
	public void onEnable() {
        this.saveDefaultConfig();
        message = new CustomYML(this, "message.yml");
        message.saveDefaultConfig();
        ConfigurationSerialization.registerClass(Rocket.class);

        PluginManager pm = Bukkit.getPluginManager();
        ConfigurationSection updateConfig = getConfig().getConfigurationSection("Update");
        if (updateConfig.getBoolean("Check")) {
            UpdateScheduler updater = new UpdateScheduler(getFile(), updateConfig);
            updater.run();
            updater.runTaskTimer(this, 0, 1728000);
            pm.registerEvents(new UpdateListener(updater), this);
        }

        if(this.getConfig().getBoolean("Metrics.Enabled")) {
            MetricsManager.StartMetrics(this);
        }

        // Register Player Flags
        if (getServer().getPluginManager().isPluginEnabled("Flags")) {
            getLogger().info("Enabling Flags Integration");

            // Connect to the data file and register the flags
            Flags.getRegistrar().register(new ModuleYML(this, "flags.yml"), this.getName());
        }

        getCommand("rocketteleport").setExecutor(new PluginCommand(this));
        new ServerEnabledTasks().run();
	}

    public void writeData() {
        CustomYML data = new CustomYML(this, "data.yml");
        data.getConfig().createSection("LaunchPads"); //Overwrite every time
        launchPad.write(data.getConfig().getConfigurationSection("LaunchPads"));
        data.saveConfig();
    }

    public void reload() {
        writeData();
        this.reloadConfig();
        message.reload();
        loadData();
    }

    /*
     * Initialize the launch pads after the worlds have loaded
     */
    private void loadData() {
        // Grab the list of materials to not teleport players to
        List<?> list = this.getConfig().getList("Exclusions");
        Set<Material> exclusions = new HashSet<Material>();
        for(Object o : list) {
            exclusions.add(Material.valueOf((String)o));
        }

        // Get the number of times to attempt to find a valid block.
        int retries = this.getConfig().getInt("Retries");
        CustomYML data = new CustomYML(this, "data.yml");
        if(data.getConfig().isConfigurationSection("LaunchPads")) {
            launchPad = new LaunchPad(data.getConfig().getConfigurationSection("LaunchPads"), exclusions, retries);
        } else {
            launchPad = new LaunchPad(exclusions, retries);
        }
        this.getServer().getPluginManager().registerEvents(launchPad, this);
    }

    @Override
	public void onDisable() {
        writeData();
        ConfigurationSerialization.unregisterClass(Rocket.class);
    }

    // Public pass-through
    public Map<RocketType, Integer> getRocketCount() {
        return launchPad.getRocketCount();
    }

    /*
     * Tasks that must be run only after the entire sever has loaded. Runs on
     * first server tick.
     */
    private class ServerEnabledTasks extends BukkitRunnable {
        public void run() {
            ((RocketTeleport)Bukkit.getPluginManager().getPlugin("RocketTeleport")).loadData();
        }
    }
}
