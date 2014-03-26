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

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines a Rocket
 */
public class Rocket implements ConfigurationSerializable {
    /**
     * Enumeration for the determining the type of rocket
     */
    public enum RocketType {
        RANDOM, SOFT, HARD, ELEMENT;

        /**
         * Returns a nice name for the RocketType
         *
         * @return The name of the rocket type.
         */
        public String getName() {
            // Make it look nice (Capital first, lower rest)
            if(this == ELEMENT) {
                return "Element Animation";
            }
            return this.toString().substring(0, 1).toUpperCase() + this.toString().substring(1).toLowerCase();
        }
    }

    /**
     * Exception thrown when the action can not be performed on the specified RocketType
     */
    public class IllegalRocketTypeException extends IllegalArgumentException {
        IllegalRocketTypeException() { super("The Rocket Type was not valid for the action performed."); }
    }

	private RocketType type;
	private TeleportLocation trigger = null;
	private final List<TeleportLocation> destination = new ArrayList<TeleportLocation>();
	private double radius = 0;

	Rocket(RocketType type) {
		this.type = type;
	}
	
	Rocket(double radius) {
		this.type = RocketType.RANDOM;
		this.radius = radius;
	}

    /**
     * Loads and existing rocket from the serialized map
     */
    Rocket(Map<String, Object> rocket) {
        type = RocketType.valueOf((String)rocket.get("Type"));
        trigger = new TeleportLocation((String)rocket.get("Trigger"));


        if(rocket.get("Destination") instanceof List<?>) {
            List<?> list = (ArrayList<?>)rocket.get("Destination");
            List<TeleportLocation> locations = new ArrayList<TeleportLocation>();
            for(Object o : list) {
                locations.add(new TeleportLocation((String)o));
            }
            destination.addAll(locations);
        } else {
            if(rocket.get("Destination").equals("null")) {
                // Upgrade from v1.1.0 and earlier where Random did not have a destination
                destination.add(new TeleportLocation((String)rocket.get("Trigger")));
            } else {
                // Upgrade from v1.2.0 and earlier where rockets supported only one destination
                destination.add(new TeleportLocation((String)rocket.get("Destination")));
            }
        }
        radius = (Double)rocket.get("Radius");
    }

    /**
     * Serialize the rocket for storage.
     *
     * @return A serialized map of the rocket
     */
    public Map<String, Object> serialize() {
        Map<String, Object> rocket = new HashMap<String, Object>();
        rocket.put("Type", type.toString());
        rocket.put("Trigger", trigger.toString());
        rocket.put("Radius", radius);

        List<String> destinations = new ArrayList<String>();
        for(TeleportLocation l : destination) {
            destinations.add(l.toString());
        }
        rocket.put("Destination",  destinations);
        return rocket;
    }

    /**
     * Gets the type of the rocket.
     *
     * @return The rocket type
     */
	public RocketType getType() {
		return type;
	}

    /**
     * Gets the trigger location.
     *
     * @return The trigger location for the rocket.
     */
    public TeleportLocation getTrigger() {
        return trigger;
    }

    /**
     * Gets a list of possible destinations for the rocket.
     *
     * @return The destination locations
     */
	public List<TeleportLocation> getDestination() {
        return destination;
    }

    /**
     * Gets the radius of the Random type rocket.
     * This is a square radius, not a true radius.
     *
     * @return The square radius rocket or 0 if not random.
     */
	public double getRadius() {
		return radius;
	}

    /**
     * Sets a the trigger location of the rocket.
     *
     * @param trigger The new trigger location of the rocket.
     * @return The modified rocket.
     * @throws IllegalArgumentException - if location is null
     */
	public Rocket setTrigger(Location trigger) {
        Validate.notNull(trigger);
        this.trigger = new TeleportLocation(trigger, false, false, false);
        return this;
    }

    /**
     * Adds a new destination to the list of possible destinations.
     * For all rocket types, this should be the final destination.
     * Do not add to Y to create a HARD RocketType.
     *
     * @param destination The destination to add.
     * @return The modified rocket.
     * @throws IllegalArgumentException - if destination is null
     */
    public Rocket addDestination(Location destination) {
        Validate.notNull(destination);
        this.destination.add(new TeleportLocation(destination, true, true, false));
        return this;
    }

    /**
     * Removes a destination from the possible list of destinations.
     *
     * @param destination The destination to remove
     * @return The modified rocket.
     * @throws IllegalArgumentException - if destination is null
     */
    @SuppressWarnings("unused")//API
    public Rocket removeDestination(TeleportLocation destination) {
        Validate.notNull(destination);
        this.destination.remove(destination);
        return this;
    }

    /**
     * Sets the radius for a random rocket. Ignored if the RocketType is not RANDOM.
     *
     * @param radius The radius to set
     * @return The modified rocket.
     * @throws IllegalRocketTypeException - if not RocketType.RANDOM
     */
    @SuppressWarnings("unused") //API
    public Rocket setRadius(double radius) {
        if(this.type == RocketType.RANDOM) {
            throw new IllegalRocketTypeException();
        }
        this.radius = radius;
        return this;
    }

    /**
     * Sets the rocket's type.
     *
     * @param type The new Rocket Type
     * @return The modified rocket
     * @throws IllegalArgumentException - if type is null
     */
    @SuppressWarnings("unused") //API
    public Rocket setType(RocketType type) {
        Validate.notNull(type);
        this.type = type;
        return this;
    }
}
