package io.github.alshain01.rocketteleport;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.github.alshain01.rocketteleport.Updater.UpdateResult;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class RocketTeleport extends JavaPlugin {
	private LaunchPad launchPad;
    private CustomYML data;
    private Updater updater = null;

	@Override
	public void onEnable() {
        this.saveDefaultConfig();
        data = new CustomYML(this);
        ConfigurationSerialization.registerClass(Rocket.class);

        if (getConfig().getBoolean("Update.Check")) {
            new UpdateScheduler().runTaskTimer(this, 0, 1728000);
            getServer().getPluginManager().registerEvents(new UpdateListener(), this);
        }

        if(this.getConfig().getBoolean("Metrics.Enabled")) {
            try {
                new MetricsLite(this).start();
            } catch (IOException ex) {
                this.getLogger().warning("Metrics failed to start.");
            }
        }

        new ServerEnabledTasks().run();
	}

    /*
     * Initialize the launch pads after the worlds have loaded
     */
    protected void initialize() {
        // Grab the list of materials to not teleport players to
        List<?> list = this.getConfig().getList("Exclusions");
        Set<Material> exclusions = new HashSet<Material>();
        for(Object o : list) {
            exclusions.add(Material.valueOf((String)o));
        }

        // Get the number of times to attempt to find a valid block.
        int retries = this.getConfig().getInt("Retries");

        if(data.getConfig().isConfigurationSection("LaunchPads")) {
            launchPad = new LaunchPad(data.getConfig().getConfigurationSection("LaunchPads"), exclusions, retries);
        } else {
            launchPad = new LaunchPad(exclusions, retries);
        }
        this.getServer().getPluginManager().registerEvents(launchPad, this);
    }

    @Override
	public void onDisable() {
        data.getConfig().createSection("LaunchPads"); //Overwrite every time
        launchPad.write(data.getConfig().getConfigurationSection("LaunchPads"));
        data.saveConfig();
        ConfigurationSerialization.unregisterClass(Rocket.class);
    }

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!cmd.getName().equalsIgnoreCase("RocketTeleport") || args.length < 1) {
			return false;
		}

		if(!(sender instanceof Player)) {
			sender.sendMessage("RocketTeleport commands may not be used by the console.");
			return true;
		}
		UUID player = ((Player)sender).getUniqueId(); 
		
		// Is the player setting a landing zone?
		if (args[0].equalsIgnoreCase("land") && launchPad.hasPartialRocket(player)) {
			if(args.length > 1) {
				return false;
			}
			launchPad.setLandMode(player);
			sender.sendMessage("Left click the landing zone.");
			return true;
		}

		// Is the player attempting to create a cannon while one is already in the queue?
		if(!args[0].equalsIgnoreCase("cancel") && launchPad.hasPartialRocket(player)) {
			sender.sendMessage("You already have a launch pad pending, use /rt land to complete it.");
            return true;
		}
		
		// Is the player creating a random cannon?
		if(args[0].equalsIgnoreCase("random") && args.length < 2) {
			sender.sendMessage("/RocketTeleport random <Radius>");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("random")) {
			double radius;
			try {
				radius = Double.valueOf(args[1]);
			} catch(NumberFormatException ex) {
				sender.sendMessage("The radius is invalid.");
				return true;
			}
			
			launchPad.addPartialRocket(player, new Rocket(radius));
			sender.sendMessage("Right click the button or plate you wish to use as a rocket trigger." );
			return true;
		}
		
		if(args.length > 1) {
			return false;
		}
	
		if(args[0].equalsIgnoreCase("soft")) {
			launchPad.addPartialRocket(player, new Rocket(RocketType.SOFT));
			sender.sendMessage("Right click the button or plate you wish to use as a rocket trigger.");
			return true;
		} else if (args[0].equalsIgnoreCase("hard")) {
			launchPad.addPartialRocket(player, new Rocket(RocketType.HARD));
			sender.sendMessage("Right click the button or plate you wish to use as a rocket trigger.");
			return true;
		} else if (args[0].equalsIgnoreCase("cancel")) {
            if(launchPad.cancelCreation(player)) {
                sender.sendMessage("Rocket LaunchPad creation canceled.");
                return true;
            }
            sender.sendMessage("There is no pending LaunchPad creation.");
            return true;
        }
		return false;
	}

    /*
 * Tasks that must be run only after the entire sever has loaded. Runs on
 * first server tick.
 */
    private class ServerEnabledTasks extends BukkitRunnable {
        public void run() {
            ((RocketTeleport)Bukkit.getPluginManager().getPlugin("RocketTeleport")).initialize();
        }
    }

    /*
     * Contains event listeners required for plugin maintenance.
     */
    private class UpdateListener implements Listener {
        // Update listener
        @EventHandler(ignoreCancelled = true)
        private void onPlayerJoin(PlayerJoinEvent e) {
            if(updater == null) { return; }
            if (e.getPlayer().hasPermission("rocketteleport.admin.notifyupdate")) {
                if(updater.getResult() == UpdateResult.UPDATE_AVAILABLE) {
                    e.getPlayer().sendMessage(ChatColor.DARK_PURPLE
                            + "The version of RocketTeleport that this server is running is out of date. "
                            + "Please consider updating to the latest version at dev.bukkit.org/bukkit-plugins/rocketteleport/.");
                } else if(updater.getResult() == UpdateResult.SUCCESS) {
                    e.getPlayer().sendMessage("[RocketTeleport] " + ChatColor.DARK_PURPLE
                            + "An update to RocketTeleport has been downloaded and will be installed when the server is reloaded.");
                }
            }
        }
    }

    /*
     * Handles update checking and downloading
     */
    private class UpdateScheduler extends BukkitRunnable {
        @Override
        public void run() {
            // Update script
            final String key = getConfig().getString("Update.ServerModsAPIKey");
            final Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("RockeTeleport");
            updater = (getConfig().getBoolean("Update.Download"))
                    ? new Updater(plugin, 65024, getFile(), Updater.UpdateType.DEFAULT, key, true)
                    : new Updater(plugin, 65024, getFile(), Updater.UpdateType.NO_DOWNLOAD, key, false);

            if (updater.getResult() == UpdateResult.UPDATE_AVAILABLE) {
                Bukkit.getServer().getConsoleSender()
                        .sendMessage("[RocketTeleport] "	+ ChatColor.DARK_PURPLE
                                + "The version of RocketTeleport that this server is running is out of date. "
                                + "Please consider updating to the latest version at dev.bukkit.org/bukkit-plugins/rocketteleport/.");
            } else if (updater.getResult() == UpdateResult.SUCCESS) {
                Bukkit.getServer().getConsoleSender()
                        .sendMessage("[RocketTeleport] "	+ ChatColor.DARK_PURPLE
                                + "An update to RocketTeleport has been downloaded and will be installed when the server is reloaded.");
            }
        }
    }
}
