package io.github.alshain01.RocketTeleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

public class Rocket implements ConfigurationSerializable {
	private final RocketType type;
	private Location trigger = null;
	private Location destination = null;
	private double radius = 0;
	
	protected Rocket(RocketType type) {
		this.type = type;
	}
	
	protected Rocket(RocketType type, Location destination) {
		this.type = type;
		this.destination = destination;
	}
	
	protected Rocket(double radius) {
		this.type = RocketType.RANDOM;
		this.radius = radius;
	}

    protected Rocket(Map<String, Object> rocket) {
        type = RocketType.valueOf((String)rocket.get("Type"));
        trigger = getLocationFromString((String)rocket.get("Trigger"));
        if(!((String)rocket.get("Destination")).equals("null")) {
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
            rocket.put("Destination", "null");
        }
        rocket.put("Radius", radius);
        return rocket;
    }

	protected RocketType getType() {
		return type;
	}
	
	protected Location getTrigger() {
		return trigger;
	}
	
	protected Location getDestination() {
		return destination;
	}
	
	protected double getRadius() {
		return radius;
	}
	
	protected void setTrigger(Location trigger) {
		this.trigger = trigger;
	}
	
	protected boolean setDestination(Location destination) {
		if(this.type != RocketType.RANDOM) {
			this.destination = destination;
			return true;
		}
		return false;
	}
	
	protected boolean setRadius(double radius) {
		if(this.type == RocketType.RANDOM) {
			this.radius = radius;
			return true;
		}
		return false;
	}
}
