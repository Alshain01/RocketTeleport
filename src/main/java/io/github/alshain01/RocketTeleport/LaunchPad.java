package io.github.alshain01.rocketteleport;

import java.util.*;

import org.bukkit.*;
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

class LaunchPad implements Listener {
	//Stores a list of fully created and active rockets
	private final Map<Location, Rocket> launchpads = new HashMap<Location, Rocket>();

	//Stores a list of partially created rockets
	private final Map<UUID, Rocket> partialLP = new HashMap<UUID, Rocket>();

	//Stores a list of players who have created a rocket and need to set it's destination
	private final Set<UUID> destinationMode = new HashSet<UUID>();

    //Store a list of blocks that players should not be randomly teleported to.
    private final Set<Material> exclusions;

    private static final Set<Material> triggerTypes = new HashSet<Material>(Arrays.asList(
            Material.WOOD_BUTTON,
            Material.WOOD_PLATE,
            Material.STONE_BUTTON,
            Material.STONE_PLATE));

    private final int retries;

    LaunchPad(ConfigurationSection config, Set<Material> exclusions, int retries) {
        Set<String> k = config.getKeys(false);
        for(String l : k) {
            Rocket r = new Rocket(config.getConfigurationSection(l).getValues(false));
            launchpads.put(r.getTrigger().getLocation(), new Rocket(config.getConfigurationSection(l).getValues(false)));
        }
        this.exclusions = exclusions;
        this.retries = retries;
    }

    LaunchPad(Set<Material> exclusions, int retries) {
        this.exclusions = exclusions;
        this.retries = retries;
    }

    void write(ConfigurationSection config) {
        for(Location l : launchpads.keySet()) {
            Rocket r = launchpads.get(l);
            config.set(r.getTrigger().toKey(), r.serialize());
        }
    }

    boolean hasPartialRocket(UUID player) {
        return partialLP.containsKey(player);
    }

    boolean cancelCreation(UUID player) {
        boolean cancelled = false;
        if(partialLP.containsKey(player)) {
            partialLP.remove(player);
            cancelled = true;
        }

        if(destinationMode.contains(player)) {
            destinationMode.remove(player);
            cancelled = true;
        }
        return cancelled;
    }

    void addPartialRocket(UUID player, Rocket rocket) {
        partialLP.put(player, rocket);
    }

    void setLandMode(UUID player) {
        destinationMode.add(player);
    }

    /*
     * Runnable class for handling delayed teleport
     */
 	private class Teleport extends BukkitRunnable {
		private final String player;
		private final Rocket rocket;
        private Location destination = null;
		
		/*private Teleport(String player, Rocket rocket) {
            this.player = player;
            this.rocket = rocket;
            this.destination = rocket.getDestination();
        }*/

        private Teleport(String player, Rocket rocket, Location destination) {
            this.player = player;
            this.rocket = rocket;
            this.destination = destination;
        }
		
		@Override
		public void run() {
			Player player = Bukkit.getPlayer(this.player);
			Location landing = destination;
			if(rocket.getType() == RocketType.HARD) {
                landing = destination.add(0, 75, 0);
			}
			player.teleport(landing, TeleportCause.PLUGIN);
            player.setVelocity(new Vector(0D, 0D, 0D));
		}
	}

    private Location getRandomLocation(Location landingArea, double radius) {
        int count = 0;
        Block landing;
        do {
            double x = (landingArea.getX() - radius) + Math.random() * (radius*2);
            double z = (landingArea.getZ() - radius) + Math.random() * (radius*2);
            Location loc = new Location(landingArea.getWorld(), x, 0D, z);
            landing = loc.getWorld().getHighestBlockAt(loc);
            count ++;
        } while (invalidType(landing.getType()) && count <= retries);
        if(count > retries) { return null; }
        return landing.getLocation().add(0, 1, 0);
    }

    private boolean invalidType(Material type) {
        for(Material m : exclusions) {
            if(type.equals(m)) { return true; }
        }
        return false;
    }
	
    /*
     * Removes any partially created rocket if the player quits
     */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent e) {
		partialLP.remove(e.getPlayer().getUniqueId());
		destinationMode.remove(e.getPlayer().getUniqueId());
	}

    /*
     * Handles a player using a fully configured rocket
     */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onActivateRocket(PlayerInteractEvent e) {
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK
                && e.getAction() != Action.PHYSICAL
				|| !triggerTypes.contains(e.getClickedBlock().getType())) {
			return;
		}

		if(launchpads.containsKey(e.getClickedBlock().getLocation())) {
			Rocket rocket = launchpads.get(e.getClickedBlock().getLocation());
            Player player = e.getPlayer();

			if((int)player.getLocation().getY() < player.getWorld().getHighestBlockYAt(player.getLocation())) {
                player.teleport(rocket.getDestination().getLocation());
				return;
			}

            Location destination;
            if(rocket.getType().equals(RocketType.RANDOM)) {
                destination = getRandomLocation(rocket.getDestination().getLocation(), rocket.getRadius());
                if(destination == null) {
                    player.sendMessage(ChatColor.RED + "Failed to locate suitable destination after " + retries +" attempts.");
                    return;
                }
            } else {
                destination = rocket.getDestination().getLocation();
            }

            player.getWorld().playSound(player.getLocation(), Sound.EXPLODE, 20, 0);
            player.teleport(player.getLocation().add(0,1,0)); // Prevents player from getting "stuck" on pressure plate
            player.setVelocity(new Vector(0D, 10D, 0D));
			new Teleport(player.getName(), rocket, destination).runTaskLater(Bukkit.getServer().getPluginManager().getPlugin("RocketTeleport"), 40);
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
				|| !triggerTypes.contains(e.getClickedBlock().getType())) {
			return;
		}

		// Don't need to activate the button.
		e.setCancelled(true);
		
		Rocket rocket = partialLP.get(e.getPlayer().getUniqueId());
		partialLP.remove(e.getPlayer().getUniqueId());
        rocket.setTrigger(e.getClickedBlock().getLocation());
		
		// The rocket still needs a destination
		partialLP.put(e.getPlayer().getUniqueId(), rocket);
		e.getPlayer().sendMessage(ChatColor.AQUA + "Trigger location set, use " + ChatColor.GOLD + "/rt land" + ChatColor.AQUA + " to create landing zone.");
	}

    /*
     * Handles a player setting the landing zone
     */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private void onSetDestination(PlayerInteractEvent e) {
		if(e.getAction() != Action.LEFT_CLICK_BLOCK
				|| !destinationMode.contains(e.getPlayer().getUniqueId())
				|| partialLP.get(e.getPlayer().getUniqueId()).getTrigger() == null) {
			return;
		}

		// If the block is something that can be interacted with, don't.
		e.setCancelled(true);
		
		Rocket rocket = partialLP.get(e.getPlayer().getUniqueId());
		partialLP.remove(e.getPlayer().getUniqueId());
		destinationMode.remove(e.getPlayer().getUniqueId());
        rocket.setDestination(e.getClickedBlock().getLocation());
        launchpads.put(rocket.getTrigger().getLocation(), rocket);
        e.getPlayer().sendMessage(ChatColor.GREEN + "New rocket created.");
	}

    /*
     * Handles destruction of a rocket launch pad
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent e) {
        if(launchpads.containsKey(e.getBlock().getLocation())) {
            launchpads.remove(e.getBlock().getLocation());
            e.getPlayer().sendMessage(ChatColor.DARK_RED + "RocketTeleport Launchpad Destroyed.");
        }
    }
}
