package io.github.alshain01.RocketTeleport;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

class RocketTeleport extends JavaPlugin {
	private LaunchPad launchPad;
    private final CustomYML data = new CustomYML(this);

	@Override
	public void onEnable() {
        this.saveDefaultConfig();
        ConfigurationSerialization.registerClass(Rocket.class);
        List<?> list = this.getConfig().getList("Exclusions");
        Set<Material> exclusions = new HashSet<Material>();
        int retries = data.getConfig().getInt("Retries");

        for(Object o : list) {
            exclusions.add(Material.valueOf((String)o));
        }

        if(data.getConfig().isConfigurationSection("LaunchPads")) {
            launchPad = new LaunchPad(data.getConfig().getConfigurationSection("LaunchPads"), exclusions, retries);
        } else {
            launchPad = new LaunchPad(exclusions, retries);
        }
		this.getServer().getPluginManager().registerEvents(launchPad, this);

        if(this.getConfig().getBoolean("Metrics.Enabled")) {
            try {
                new MetricsLite(this).start();
            } catch (IOException ex) {
                this.getLogger().warning("Metrics failed to start.");
            }
        }
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
}
