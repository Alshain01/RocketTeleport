/*
 * Copyright (c) 2/16/14 1:49 PM Kevin Seiden. All rights reserved.
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

import io.github.alshain01.flags.*;
import io.github.alshain01.flags.System;
import io.github.alshain01.flags.area.Area;

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
import org.bukkit.plugin.Plugin;
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

    // Flags need to be Objects to guard against cases where
    // The Flags plugin in is not installed.  We will cast them back later.
    private Object createFlag = null, landFlag = null, useFlag = null;

    private static final Set<Material> triggerTypes = new HashSet<Material>(Arrays.asList(
            Material.WOOD_BUTTON,
            Material.WOOD_PLATE,
            Material.STONE_BUTTON,
            Material.STONE_PLATE));

    private final int retries;
    private boolean easterEggTimeout = true;

    LaunchPad(ConfigurationSection config, Set<Material> exclusions, int retries) {
        Set<String> k = config.getKeys(false);
        for(String l : k) {
            Rocket r = new Rocket(config.getConfigurationSection(l).getValues(false));
            launchpads.put(r.getTrigger().getLocation(), new Rocket(config.getConfigurationSection(l).getValues(false)));
        }
        this.exclusions = exclusions;
        this.retries = retries;
        if(Bukkit.getPluginManager().isPluginEnabled("Flags")) { initFlags(); }
    }

    LaunchPad(Set<Material> exclusions, int retries) {
        this.exclusions = exclusions;
        this.retries = retries;
        if(Bukkit.getPluginManager().isPluginEnabled("Flags")) { initFlags(); }
    }

    void write(ConfigurationSection config) {
        for(Location l : launchpads.keySet()) {
            Rocket r = launchpads.get(l);
            config.set(r.getTrigger().toKey(), r.serialize());
        }
    }

    void initFlags() {
        landFlag = Flags.getRegistrar().getFlag("SetRocketLanding");
        createFlag = Flags.getRegistrar().getFlag("CreateRocket");
        useFlag = Flags.getRegistrar().getFlag("UseRocket");
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
        private Location destination = null;
		
		/*private Teleport(String player, Rocket rocket) {
            this.player = player;
            this.rocket = rocket;
            this.destination = rocket.getDestination();
        }*/

        private Teleport(String player, Location destination) {
            this.player = player;
            this.destination = destination;
        }
		
		@Override
		public void run() {
			Player player = Bukkit.getPlayer(this.player);
            player.setVelocity(new Vector(0D, 0D, 0D));
			player.teleport(destination, TeleportCause.PLUGIN);
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
	@EventHandler(priority = EventPriority.HIGHEST)
	private void onActivateRocket(PlayerInteractEvent e) {
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK
                && e.getAction() != Action.PHYSICAL
				|| !triggerTypes.contains(e.getClickedBlock().getType())) {
			return;
		}

        if(launchpads.containsKey(e.getClickedBlock().getLocation())) {
            Player player = e.getPlayer();

            // Check the flag
            if(useFlag != null) {
                Flag flag = (Flag)useFlag;
                Area area = System.getActive().getAreaAt(e.getClickedBlock().getLocation());

                if(!player.hasPermission(flag.getBypassPermission()) && !area.hasTrust(flag, player)) {
                    player.sendMessage(area.getMessage(flag, player.getName()));
                    return;
                }
            }

            e.setCancelled(false); // Undo anti-grief measures for rockets.
			Rocket rocket = launchpads.get(e.getClickedBlock().getLocation());
            List<RocketLocation> possibleDestinations = rocket.getDestination();

            // Get a random location from the list of possible locations
            Location destination = possibleDestinations.get((int)(Math.random()*(possibleDestinations.size() - 1))).getLocation();

            if(rocket.getType().equals(RocketType.ELEMENT)) {
                Plugin plugin = Bukkit.getPluginManager().getPlugin("RocketTeleport");
                if(easterEggTimeout) {
                    new EasterEgg().run(player, destination);
                    easterEggTimeout = false;
                    new BukkitRunnable() {
                        public void run() {
                            easterEggTimeout = true;
                        }
                    }.runTaskLater(plugin, plugin.getConfig().getInt("EasterEggTimeout"));
                } else {
                    double timeout = plugin.getConfig().getInt("EasterEggTimeout") / 60 / 20;
                    player.sendMessage(ChatColor.RED + "This can only be used once every " + timeout + " minutes server wide.");
                }
                return;
            }

            if(rocket.getType().equals(RocketType.RANDOM)) {
                // Get a random radius around the location
                destination = getRandomLocation(destination, rocket.getRadius());
                if(destination == null) {
                    player.sendMessage(ChatColor.RED + "Failed to locate suitable destination after " + retries +" attempts.");
                    return;
                }
            }

            if((int)player.getLocation().getY() < player.getWorld().getHighestBlockYAt(player.getLocation())) {
                player.teleport(destination);
                return;
            }

            if(rocket.getType().equals(RocketType.HARD)) {
                destination = destination.add(0D, 75D, 0D);
            }

            player.getWorld().playSound(player.getLocation(), Sound.EXPLODE, 20, 0);
            player.teleport(player.getLocation().add(0D,1D,0D)); // Prevents player from getting "stuck" on pressure plate
            player.setVelocity(new Vector(0D, 10D, 0D));
			new Teleport(player.getName(), destination).runTaskLater(Bukkit.getServer().getPluginManager().getPlugin("RocketTeleport"), 40);
		}
	}

    /*
     * Handles a player creating a rocket
     */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private void onCreateRocket(PlayerInteractEvent e) {
        Player player = e.getPlayer();

		if(!partialLP.containsKey(player.getUniqueId())
				|| partialLP.get(player.getUniqueId()).getTrigger() != null) {
			return;
		}

		if(e.getAction() != Action.RIGHT_CLICK_BLOCK 
				|| !triggerTypes.contains(e.getClickedBlock().getType())) {
			return;
		}

        // Check the flag
        if(createFlag != null) {
            Flag flag = (Flag)createFlag;
            Area area = System.getActive().getAreaAt(e.getClickedBlock().getLocation());

            if(!player.hasPermission(flag.getBypassPermission()) && !area.hasTrust(flag, player)) {
                player.sendMessage(area.getMessage(flag, player.getName()));
                return;
            }
        }

		// Don't need to activate the button.
		e.setCancelled(true);
		
		Rocket rocket = partialLP.get(player.getUniqueId());
		partialLP.remove(player.getUniqueId());
        rocket.setTrigger(e.getClickedBlock().getLocation());
		
		// The rocket still needs a destination
		partialLP.put(player.getUniqueId(), rocket);
        player.sendMessage(ChatColor.AQUA + "Trigger location set, use " + ChatColor.GOLD + "/rt land" + ChatColor.AQUA + " to create landing zone.");
	}

    /*
     * Handles a player setting the landing zone
     */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private void onSetDestination(PlayerInteractEvent e) {
        Player player = e.getPlayer();

		if(e.getAction() != Action.LEFT_CLICK_BLOCK
				|| !destinationMode.contains(player.getUniqueId())
				|| partialLP.get(player.getUniqueId()).getTrigger() == null) {
			return;
		}

        // Check the flag
        if(landFlag != null) {
            Flag flag = (Flag)landFlag;
            Area area = System.getActive().getAreaAt(e.getClickedBlock().getLocation());

            if(!player.hasPermission(flag.getBypassPermission()) && !area.hasTrust(flag, player)) {
                player.sendMessage(area.getMessage(flag, player.getName()));
                destinationMode.remove(player.getUniqueId());
                return;
            }
        }

		// If the block is something that can be interacted with, don't.
		e.setCancelled(true);
		
		Rocket rocket = partialLP.get(player.getUniqueId());
		partialLP.remove(player.getUniqueId());
		destinationMode.remove(player.getUniqueId());
        rocket.addDestination(e.getClickedBlock().getLocation());
        launchpads.put(rocket.getTrigger().getLocation(), rocket);
        player.sendMessage(ChatColor.GREEN + "New rocket created.");
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

    Map<RocketType, Integer> getRocketCount() {
        Map<RocketType, Integer> counts = new HashMap<RocketType, Integer>();
        for(RocketType r : RocketType.values()) {
            counts.put(r, 0);
        }

        for(Rocket r : launchpads.values()) {
            int count = counts.get(r.getType());
            count++;
            counts.put(r.getType(), count);
        }
        return counts;
    }
}
