package io.github.alshain01.RocketTeleport;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class LaunchPad implements Listener {
	//Stores a list of fully created and active rockets
	private Map<Location, Rocket> launchpads = new ConcurrentHashMap<Location, Rocket>();

	//Stores a list of partially created rockets
	private Map<UUID, Rocket> partialLP = new ConcurrentHashMap<UUID, Rocket>();

	//Stores a list of players who have created a rocket and need to set it's destination
	private Set<UUID> destMode = new HashSet<UUID>();

    //Store a list of blocks that players should not be randomly teleported to.
    private Set<Material> exclusions = new HashSet<Material>();

    protected LaunchPad(ConfigurationSection config, Set<Material> exclusions) {
        Set<String> k = config.getKeys(false);
        for(String l : k) {
            launchpads.put(getLocationFromString(l), new Rocket(config.getConfigurationSection(l).getValues(false)));
        }
        this.exclusions = exclusions;
    }

    protected LaunchPad(Set<Material> exclusions) {
        this.exclusions = exclusions;
    }

    protected void write(ConfigurationSection config) {
        for(Location l : launchpads.keySet()) {
            String loc = l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
            config.set(loc, launchpads.get(l).serialize());
        }
    }

    protected boolean hasPartialRocket(UUID player) {
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

    protected void addPartialRocket(UUID player, Rocket rocket) {
        partialLP.put(player, rocket);
    }

    protected void setLandMode(UUID player) {
        destMode.add(player);
    }

    /*
     * Runnable class for handling delayed teleport
     */
 	private class Teleport extends BukkitRunnable {
		private String player;
		private Rocket rocket;
        private Location destination = null;
		
		private Teleport(String player, Rocket rocket) {
            this.player = player;
            this.rocket = rocket;
            this.destination = rocket.getDestination();
        }

        private Teleport(String player, Rocket rocket, Location destination) {
            this.player = player;
            this.rocket = rocket;
            this.destination = destination;
        }
		
		@Override
		public void run() {
			Player player = Bukkit.getPlayer(this.player);
			player.setVelocity(new Vector(0D, 0D, 0D));
			Location landing = destination;
			switch(rocket.getType()) {
				case SOFT:
                case RANDOM:
					landing = destination.add(0,1,0);
					break;
				case HARD:
					landing = destination.add(0, 75, 0);
                    break;
			}
			player.teleport(landing, TeleportCause.PLUGIN);
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

    private Location getRandomLocation(Location trigger, double radius) {
        Block landing;
        do {
            double xloc = (trigger.getX() - radius) + Math.random() * (radius*2);
            double zloc = (trigger.getZ() - radius) + Math.random() * (radius*2);
            Location loc = new Location(trigger.getWorld(), xloc, 0D, zloc);
            landing = loc.getWorld().getHighestBlockAt(loc);
        } while (!validType(landing.getType()));
        return landing.getLocation().add(0, 1, 0);
    }

    private boolean validType(Material type) {
        for(Material m : exclusions) {
            if(type.equals(m)) { return false; }
        }
        return true;
    }
	
    /*
     * Removes any partially created rocket if the player quits
     */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent e) {
		partialLP.remove(e.getPlayer().getUniqueId());
		destMode.remove(e.getPlayer().getUniqueId());
	}

    /*
     * Handles a player using a fully configured rocket
     */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onActivateRocket(PlayerInteractEvent e) {
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK 
				|| (e.getClickedBlock().getType() != Material.STONE_BUTTON
				&& e.getClickedBlock().getType() != Material.WOOD_BUTTON)) {
			return;
		}

		if(launchpads.containsKey(e.getClickedBlock().getLocation())) {
			Rocket rocket = launchpads.get(e.getClickedBlock().getLocation());
			if(e.getPlayer().getLocation().getY() < e.getPlayer().getWorld().getHighestBlockYAt(e.getPlayer().getLocation())) {
				e.getPlayer().teleport(rocket.getDestination());
				return;
			}

            Location dest;
            if(rocket.getType().equals(RocketType.RANDOM)) {
                dest = getRandomLocation(rocket.getTrigger(), rocket.getRadius());
            } else {
                dest = rocket.getDestination();
            }

			e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.EXPLODE, 20, 0);
			e.getPlayer().setVelocity(new Vector(0D, 10D, 0D));
			new Teleport(e.getPlayer().getName(), rocket, dest).runTaskLater(Bukkit.getServer().getPluginManager().getPlugin("RocketTeleport"), 20 * 2);
		}
	}

    /*
     * Handles a player creating a rocket
     */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private void onCreateRocket(PlayerInteractEvent e) {
		if(!partialLP.containsKey(e.getPlayer().getUniqueId())
				|| partialLP.get(e.getPlayer().getUniqueId()).getTrigger() != null) {
			return;
		}

		if(e.getAction() != Action.RIGHT_CLICK_BLOCK 
				|| (e.getClickedBlock().getType() != Material.STONE_BUTTON
				&& e.getClickedBlock().getType() != Material.WOOD_BUTTON)) {
			return;
		}

		// Don't need to activate the button.
		e.setCancelled(true);
		
		Rocket rocket = partialLP.get(e.getPlayer().getUniqueId());
		partialLP.remove(e.getPlayer().getUniqueId());
        rocket.setTrigger(e.getClickedBlock().getLocation());
		
		// The rocket is complete
		if(rocket.getType() == RocketType.RANDOM) {
			launchpads.put(rocket.getTrigger(), rocket);
			e.getPlayer().sendMessage("New random rocket created.");
			return;
		}
		
		// The rocket still needs a destination
		partialLP.put(e.getPlayer().getUniqueId(), rocket);
		e.getPlayer().sendMessage("Trigger location set, use /rt land to create landing zone.");
	}

    /*
     * Handles a player setting the landing zone
     */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private void onSetDestination(PlayerInteractEvent e) {
		if(e.getAction() != Action.LEFT_CLICK_BLOCK
				|| !destMode.contains(e.getPlayer().getUniqueId())
				|| partialLP.get(e.getPlayer().getUniqueId()).getTrigger() == null) {
			return;
		}

		// If the block is something that can be interacted with, don't.
		e.setCancelled(true);
		
		Rocket rocket = partialLP.get(e.getPlayer().getUniqueId());
		partialLP.remove(e.getPlayer().getUniqueId());
		destMode.remove(e.getPlayer().getUniqueId());
        rocket.setDestination(e.getClickedBlock().getLocation());
		
		launchpads.put(rocket.getTrigger(), rocket);
		e.getPlayer().sendMessage("New rocket created.");
	}

    /*
     * Handles destruction of a rocket launch pad
     */
    @EventHandler(priority = EventPriority.HIGH.MONITOR, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent e) {
        if(launchpads.containsKey(e.getBlock().getLocation())) {
            launchpads.remove(e.getBlock().getLocation());
        }
        e.getPlayer().sendMessage("RocketTeleport Launchpad Destroyed.");
    }
}
