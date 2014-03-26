/*
 * Copyright (c) 2/16/14 1:50 PM Kevin Seiden. All rights reserved.
 *
 *  This works is licensed under the Creative Commons Attribution-NonCommercial 3.0
 *
 *  You are Free to:
 *     to Share: to copy, distribute and transmit the work
 *     to Remix: to adapt the work
 *
 *  Under the following conditions:
 *     Attribution: You must attribute the work in the manner specified by the author (but not in any way that suggests that they endorse you or your use of the work).
 *     Non-commercial: You may not use this work for commercial purposes.
 *
 *  With the understanding that:
 *     Waiver: Any of the above conditions can be waived if you get permission from the copyright holder.
 *     Public Domain: Where the work or any of its elements is in the public domain under applicable law, that status is in no way affected by the license.
 *     Other Rights: In no way are any of the following rights affected by the license:
 *         Your fair dealing or fair use rights, or other applicable copyright exceptions and limitations;
 *         The author's moral rights;
 *         Rights other persons may have either in the work itself or in how the work is used, such as publicity or privacy rights.
 *
 *  Notice: For any reuse or distribution, you must make clear to others the license terms of this work. The best way to do this is with a link to this web page.
 *  http://creativecommons.org/licenses/by-nc/3.0/
 */

package io.github.alshain01.rocketteleport;

import java.util.*;

import io.github.alshain01.rocketteleport.PluginCommand.PluginCommandType;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Plugin Main Class
 */
@SuppressWarnings("WeakerAccess") //API
public class RocketTeleport extends JavaPlugin {
    static CustomYML message;  // Static for enumeration access
	LaunchPad launchPad;
    private Warp warpController;
    MissionControl missionControl;

    final Map<UUID, PluginCommandType> commandQueue = new HashMap<UUID, PluginCommandType>();
    final Map<UUID, Rocket> rocketQueue = new HashMap<UUID, Rocket>();

    @Override
	public void onEnable() {
        this.saveDefaultConfig();
        message = new CustomYML(this, "message.yml");
        message.saveDefaultConfig();
        ConfigurationSerialization.registerClass(Rocket.class);

        missionControl = new MissionControl(getConfig().getConfigurationSection("Sound"), getConfig().getConfigurationSection("Timer"));


        // Configure the updater
        if (getConfig().getBoolean("Update.Enabled")) {
            new io.github.alshain01.rocketteleport.Updater(this);
        }
        Bukkit.getPluginManager().registerEvents(new RocketListener(this), this);
        getCommand("rocketteleport").setExecutor(new PluginCommand(this));
        new ServerEnabledTasks(this).run();
	}

    void writeData() {
        CustomYML data = new CustomYML(this, "data.yml");
        data.getConfig().createSection("LaunchPads"); //Overwrite every time
        launchPad.write(data.getConfig().getConfigurationSection("LaunchPads"));
        data.getConfig().createSection("Warps"); //Overwrite every time
        warpController.write(data.getConfig().getConfigurationSection("Warps"));
        data.saveConfig();
    }

    void reload() {
        writeData();
        this.reloadConfig();
        missionControl = new MissionControl(getConfig().getConfigurationSection("Sound"), getConfig().getConfigurationSection("Sound"));
        message.reload();
        loadData();
    }

    /*
     * Initialize the launch pads after the worlds have loaded
     */
    private void loadData() {
        // Get the number of times to attempt to find a valid block.
        CustomYML data = new CustomYML(this, "data.yml");
        if(data.getConfig().isConfigurationSection("LaunchPads")) {
            launchPad = new LaunchPad(data.getConfig().getConfigurationSection("LaunchPads"));
        } else {
            launchPad = new LaunchPad();
        }

        if(data.getConfig().isConfigurationSection("Warps")) {
            warpController = new Warp(this, data.getConfig().getConfigurationSection("Warps"));
        } else {
            warpController = new Warp(this);
        }
        CommandExecutor warpExecutor = new WarpCommand(warpController, missionControl);
        getCommand("warp").setExecutor(warpExecutor);
        getCommand("delwarp").setExecutor(warpExecutor);
        getCommand("setwarp").setExecutor(warpExecutor);
    }

    @Override
	public void onDisable() {
        writeData();
        ConfigurationSerialization.unregisterClass(Rocket.class);
    }

    @SuppressWarnings("unused") // API
    public LaunchPad getLaunchPad() { return this.launchPad; }

    @SuppressWarnings("unused") // API
    public Warp getWarps() { return warpController; }

    /*
     * Tasks that must be run only after the entire sever has loaded. Runs on
     * first server tick.
     */
    private class ServerEnabledTasks extends BukkitRunnable {
        final RocketTeleport plugin;
        ServerEnabledTasks(RocketTeleport plugin) {
            this.plugin = plugin;
        }


        @Override
        public void run() {
            plugin.loadData();

            if(plugin.getConfig().getBoolean("Metrics.Enabled")) {
                Metrics.StartMetrics(plugin);
            }
        }
    }
}
