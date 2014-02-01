package io.github.alshain01.rocketteleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

class Rocket implements ConfigurationSerializable {
	private final RocketType type;
	private Location trigger = null;
	private Location destination = null;
	private double radius = 0;
	
	Rocket(RocketType type) {
		this.type = type;
	}
	
	/*Rocket(RocketType type, Location destination) {
		this.type = type;
		this.destination = destination;
	}*/
	
	Rocket(double radius) {
		this.type = RocketType.RANDOM;
		this.radius = radius;
	}

    Rocket(Map<String, Object> rocket) {
        type = RocketType.valueOf((String)rocket.get("Type"));
        trigger = getLocationFromString((String)rocket.get("Trigger"));
        if(((String)rocket.get("Destination")).equals("null")) {
            // Upgrade from v1.1.0 and earlier where Random did not have a destination
            destination = getLocationFromString((String)rocket.get("Trigger"));
        } else {
            destination = getLocationFromString((String)rocket.get("Destination"));
        }
        radius = (Double)rocket.get("Radius");
    }

    private Location getLocationFromString(String s) {
        String[] arg = s.split(",");
        double[] parsed = new double[3];

        for (int a = 0; a < 3; a++) {
            parsed[a] = Double.parseDouble(arg[a+1]);
        }
        return new Location (Bukkit.getWorld(arg[0]), parsed[0], parsed[1], parsed[2]);
    }

    public Map<String, Object> serialize() {
        Map<String, Object> rocket = new HashMap<String, Object>();
        rocket.put("Type", type.toString());
        rocket.put("Trigger", trigger.getWorld().getName() + "," + trigger.getX() + "," + trigger.getY() + "," + trigger.getZ());
        if(destination != null) {
            rocket.put("Destination",  destination.getWorld().getName() + "," + destination.getX() + "," + destination.getY() + "," + destination.getZ());
        } else {
            // Upgrade from v1.1.0 and earlier where Random did not have a destination
            rocket.put("Destination",  trigger.getWorld().getName() + "," + trigger.getX() + "," + trigger.getY() + "," + trigger.getZ());
        }
        rocket.put("Radius", radius);
        return rocket;
    }

	RocketType getType() {
		return type;
	}
	
	Location getTrigger() {
		return trigger;
	}
	
	Location getDestination() {
		return destination;
	}
	
	double getRadius() {
		return radius;
	}
	
	void setTrigger(Location trigger) {
		this.trigger = trigger;
	}
	
	void setDestination(Location destination) {	this.destination = destination; }
	
	/*boolean setRadius(double radius) {
		if(this.type == RocketType.RANDOM) {
			this.radius = radius;
			return true;
		}
		return false;
	}*/
}
