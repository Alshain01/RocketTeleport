package io.github.alshain01.RocketTeleport;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class LaunchPad implements Listener {
	//Stores a list of fully created and active cannons
	private Map<Location, Rocket> launchpads = new ConcurrentHashMap<Location, Rocket>();

	//Stores a list of partially created cannons
	private Map<UUID, Rocket> partialLP = new ConcurrentHashMap<UUID, Rocket>();

	//Stores a list of players who have created a cannon and need to set it's destination
	private Set<UUID> destMode = new HashSet<UUID>();

    public LaunchPad(ConfigurationSection config) {
        Set<String> k = config.getKeys(false);
        for(String l : k) {
            launchpads.put(getLocationFromString(l), new Rocket(config.getConfigurationSection(l).getValues(false)));
        }
    }

    public LaunchPad() {

    }

    public void write(ConfigurationSection config) {
        for(Location l : launchpads.keySet()) {
            String loc = l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
            config.set(loc, launchpads.get(l).serialize());
        }
    }

    private Location getLocationFromString(String s) {
        String[] arg = s.split(",");
        int[] parsed = new int[3];

        for (int a = 0; a < 3; a++) {
            parsed[a] = Integer.parseInt(arg[a + 1]);
        }

        return new Location (Bukkit.getWorld(arg[0]), parsed[0], parsed[1], parsed[2]);
    }

 	private class Teleport extends BukkitRunnable {
		private String player;
		private Rocket rocket;
		
		private Teleport(String player, Rocket rocket) {
			this.player = player;
			this.rocket = rocket;
		}
		
		@Override
		public void run() {
			Player player = Bukkit.getPlayer(this.player);
			player.setVelocity(new Vector(0D, 0D, 0D));
			Location landing;
			switch(rocket.getType()) {
				case SOFT: 
					landing = rocket.getDestination().add(0,1,0);
					break;
				case HARD:
					landing = rocket.getDestination().add(0, 75, 0);
                    break;
				case RANDOM:
					double xloc = (rocket.getTrigger().getX() - rocket.getRadius()) + Math.random() * (rocket.getRadius()*2);
					double zloc = (rocket.getTrigger().getZ() - rocket.getRadius()) + Math.random() * (rocket.getRadius()*2);
					Location loc = new Location(player.getWorld(), xloc, 0D, zloc);
					landing = loc.getWorld().getHighestBlockAt(loc).getLocation().add(0, 1, 0);
					break;
				default:
					landing = rocket.getDestination().getWorld().getSpawnLocation();
			}
			player.teleport(landing, TeleportCause.PLUGIN);
		}
	}
	
	protected boolean hasPartialCannon(UUID player) {
		return partialLP.containsKey(player);
	}

    protected boolean cancelCreation(UUID player) {
        boolean cancelled = false;
        if(partialLP.containsKey(player)) {
            partialLP.remove(player);
            cancelled = true;
        }
          
        if(destMode.contains(player)) {
            destMode.remove(player);
            cancelled = true;
        }
        return cancelled;
    }

	protected void addPartialCannon(UUID player, Rocket cannon) {
		partialLP.put(player, cannon);
	}
	
	protected void setLandMode(UUID player) {
		destMode.add(player);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent e) {
		partialLP.remove(e.getPlayer().getUniqueId());
		destMode.remove(e.getPlayer().getUniqueId());
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onActivateCannon(PlayerInteractEvent e) {
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK 
				|| (e.getClickedBlock().getType() != Material.STONE_BUTTON
				&& e.getClickedBlock().getType() != Material.WOOD_BUTTON)) {
			return;
		}

        RocketTeleport.Debug("Player interacted with button.");

		if(launchpads.containsKey(e.getClickedBlock().getLocation())) {
            RocketTeleport.Debug("LaunchPad found.");
			Rocket rocket = launchpads.get(e.getClickedBlock().getLocation()); 
			if(e.getPlayer().getLocation().getY() < e.getPlayer().getWorld().getHighestBlockYAt(e.getPlayer().getLocation())) {
				e.getPlayer().teleport(rocket.getDestination());
				return;
			}
			
			e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.EXPLODE, 20, 0);
			e.getPlayer().setVelocity(new Vector(0D, 10D, 0D));
			new Teleport(e.getPlayer().getName(), rocket).runTaskLater(Bukkit.getServer().getPluginManager().getPlugin("RocketTeleport"), 20 * 2);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCreateCannon(PlayerInteractEvent e) {
		if(!partialLP.containsKey(e.getPlayer().getUniqueId())
				|| partialLP.get(e.getPlayer().getUniqueId()).getTrigger() != null) {
			return;
		}

		if(e.getAction() != Action.RIGHT_CLICK_BLOCK 
				|| (e.getClickedBlock().getType() != Material.STONE_BUTTON
				&& e.getClickedBlock().getType() != Material.WOOD_BUTTON)) {
			return;
		}

        RocketTeleport.Debug("Player interacted with button. Partial LaunchPad found.");

		// Don't need to activate the button.
		e.setCancelled(true);
		
		Rocket cannon = partialLP.get(e.getPlayer().getUniqueId()); 
		partialLP.remove(e.getPlayer().getUniqueId());
		cannon.setTrigger(e.getClickedBlock().getLocation());
		
		// The cannon is complete
		if(cannon.getType() == RocketType.RANDOM) {
			launchpads.put(cannon.getTrigger(), cannon);
			e.getPlayer().sendMessage("New random cannon created.");
			return;
		}
		
		// The cannon still needs a destination
		partialLP.put(e.getPlayer().getUniqueId(), cannon);
		e.getPlayer().sendMessage("Trigger location set, use /rt land to create landing zone.");
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onSetDestination(PlayerInteractEvent e) {
		if(e.getAction() != Action.LEFT_CLICK_BLOCK
				|| !destMode.contains(e.getPlayer().getUniqueId())
				|| partialLP.get(e.getPlayer().getUniqueId()).getTrigger() == null) {
			return;
		}

		// If the block is something that can be interacted with, don't.
		e.setCancelled(true);
		
		Rocket cannon = partialLP.get(e.getPlayer().getUniqueId());
		partialLP.remove(e.getPlayer().getUniqueId());
		destMode.remove(e.getPlayer().getUniqueId());
		cannon.setDestination(e.getClickedBlock().getLocation());
		
		launchpads.put(cannon.getTrigger(), cannon);
		e.getPlayer().sendMessage("New cannon created.");
	}
}
