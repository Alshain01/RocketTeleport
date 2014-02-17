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

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Rocket implements ConfigurationSerializable {
	private final RocketType type;
	private RocketLocation trigger = null;
	private final List<RocketLocation> destination = new ArrayList<RocketLocation>();
	private double radius = 0;

	Rocket(RocketType type) {
		this.type = type;
	}
	
	Rocket(double radius) {
		this.type = RocketType.RANDOM;
		this.radius = radius;
	}

    Rocket(Map<String, Object> rocket) {
        type = RocketType.valueOf((String)rocket.get("Type"));
        trigger = new RocketLocation((String)rocket.get("Trigger"));


        if(rocket.get("Destination") instanceof List<?>) {
            List<?> list = (ArrayList<?>)rocket.get("Destination");
            List<RocketLocation> locations = new ArrayList<RocketLocation>();
            for(Object o : list) {
                locations.add(new RocketLocation((String)o));
            }
            destination.addAll(locations);
        } else {
            if(rocket.get("Destination").equals("null")) {
                // Upgrade from v1.1.0 and earlier where Random did not have a destination
                destination.add(new RocketLocation((String)rocket.get("Trigger")));
            } else {
                // Upgrade from v1.2.0 and earlier where rockets supported only one destination
                destination.add(new RocketLocation((String)rocket.get("Destination")));
            }
        }
        radius = (Double)rocket.get("Radius");
    }

    public Map<String, Object> serialize() {
        Map<String, Object> rocket = new HashMap<String, Object>();
        rocket.put("Type", type.toString());
        rocket.put("Trigger", trigger.toString());
        rocket.put("Radius", radius);

        List destinations = new ArrayList<String>();
        for(RocketLocation l : destination) {
            destinations.add(l.toString());
        }
        rocket.put("Destination",  destinations);
        return rocket;
    }

	RocketType getType() {
		return type;
	}
	
	RocketLocation getTrigger() { return trigger; }

	List<RocketLocation> getDestination() {	return destination; }

	double getRadius() {
		return radius;
	}
	
	void setTrigger(Location trigger) { this.trigger = new RocketLocation(trigger); }
	
	void addDestination(Location destination) {	this.destination.add(new RocketLocation(destination)); }
	
	/*boolean setRadius(double radius) {
		if(this.type == RocketType.RANDOM) {
			this.radius = radius;
			return true;
		}
		return false;
	}*/
}
