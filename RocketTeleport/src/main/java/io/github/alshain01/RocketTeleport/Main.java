package io.github.alshain01.RocketTeleport;

import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	LaunchPad launchPad = new LaunchPad();
	
	@Override
	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(launchPad, this);		
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!cmd.getName().equals("rocketteleport") || args.length < 1) {
			return false;
		}

		if(!(sender instanceof Player)) {
			sender.sendMessage("RocketTeleport commands may not be used by the conosle.");
			return true;
		}
		UUID player = ((Player)sender).getUniqueId(); 
		
		// Is the player setting a landing zone?
		if (args[1].equalsIgnoreCase("land") && launchPad.hasPartialCannon(player)) {
			if(args.length > 1) {
				return false;
			}
			launchPad.setLandMode(player);
			sender.sendMessage("Right click the landing zone.");
			return true;
		}

		// Is the player attempting to create a cannon while one is already in the queue?
		if(launchPad.hasPartialCannon(player)) {
			sender.sendMessage("You already have a launch pad pending, use /rt land to complete it.");
		}
		
		// Is the player creating a random cannon?
		if(args[1].equalsIgnoreCase("random") && args.length < 2) {
			sender.sendMessage("/rocketteleport random <Radius>");
			return true;
		}
		
		if(args[1].equalsIgnoreCase("random")) {
			double radius;
			try {
				radius = Double.valueOf(args[1]);
			} catch(NumberFormatException ex) {
				sender.sendMessage("The radius is invalid.");
				return true;
			}
			
			launchPad.addPartialCannon(player, new Rocket(radius));
			sender.sendMessage("Right click the button you wish to use as a rocket trigger." );
			return true;
		}
		
		if(args.length > 1) {
			return false;
		}
	
		if(args[1].equalsIgnoreCase("soft")) {
			launchPad.addPartialCannon(player, new Rocket(RocketType.SOFT));
			sender.sendMessage("Right click the button you wish to use as a rocket trigger.");
			return true;
		} else if (args[1].equalsIgnoreCase("hard")) {
			launchPad.addPartialCannon(player, new Rocket(RocketType.HARD));
			sender.sendMessage("Right click the button you wish to use as a rocket trigger.");
			return true;
		}
		
		return false;
	}
}
