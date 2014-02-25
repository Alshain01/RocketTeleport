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

import io.github.alshain01.rocketteleport.Rocket.RocketType;

import java.util.*;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

class LaunchPad {
	//Stores a list of fully created and active rockets
	private final Map<Location, Rocket> launchpads = new HashMap<Location, Rocket>();

    LaunchPad () { }

    /*
     * Constructor used for loading existing data.
     */
    LaunchPad(ConfigurationSection config) {
        Set<String> k = config.getKeys(false);
        for(String l : k) {
            Rocket r = new Rocket(config.getConfigurationSection(l).getValues(false));
            launchpads.put(r.getTrigger().getLocation(), new Rocket(config.getConfigurationSection(l).getValues(false)));
        }
    }

    /*
     * Write the data to the provided configuration section
     */
    void write(ConfigurationSection config) {
        for(Location l : launchpads.keySet()) {
            Rocket r = launchpads.get(l);
            config.set(r.getTrigger().toKey(), r.serialize());
        }
    }

    void addRocket(Rocket rocket) {
        launchpads.put(rocket.getTrigger().getLocation(), rocket);
    }

    boolean removeRocket(Player player, Location location) {
        if(launchpads.containsKey(location)) {
            launchpads.remove(location);
            player.sendMessage(Message.TRIGGER_DESTROYED.get());
            return true;
        }
        return false;
    }

    boolean hasRocket(Location location) {
        return launchpads.containsKey(location);
    }

    Rocket getRocket(Location location) {
        return launchpads.get(location);
    }

    /**
     * Gets a map containing the rocket type and the number of rockets on the server of that type.
     *
     * @return The rocket count map.
     */
    public Map<RocketType, Integer> getRocketCount() {
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
