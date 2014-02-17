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

import org.bukkit.Bukkit;
import org.bukkit.Location;

class RocketLocation {
    private final String world;
    private final double coords[] = new double[3];

    RocketLocation(Location location) {
        coords[0] = getMidpoint(location.getX()) ;
        coords[1] = Math.ceil(location.getY());
        coords[2] = getMidpoint(location.getZ());
        world = location.getWorld().getName();
    }

    RocketLocation(String location) {
        String[] arg = location.split(",");

        world = arg[0];
        for (int a = 0; a < 3; a++) {
            coords[a] = Double.parseDouble(arg[a+1]);
        }
    }

    @Override
    public String toString() {
        return world + "," + coords[0] + "," + coords[1] + "," + coords[2];
    }

    public String toKey() {
        return world + "," + (int)coords[0] + "," + (int)coords[1] + "," + (int)coords[2];
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(world), coords[0], coords[1], coords[2]);
    }

    private double getMidpoint(double value) {
        if (value < 0) {
            value = Math.ceil(value);
            return value - 0.5;
        } else {
            value = Math.floor(value);
            return value + 0.5;
        }
    }
}
