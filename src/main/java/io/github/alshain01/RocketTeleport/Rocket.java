package io.github.alshain01.rocketteleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class Rocket implements ConfigurationSerializable {
	private final RocketType type;
	private RocketLocation trigger = null;
	private RocketLocation destination = null;
	private double radius = 0;

    private class RocketLocation {
        final String world;
        final double coords[] = new double[3];

        private RocketLocation(Location location) {
            coords[0] = location.getX();
            coords[1] = location.getY();
            coords[2] = location.getZ();
            world = location.getWorld().getName();
        }

        private RocketLocation(String location) {
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

        public Location getLocation() {
            return new Location(Bukkit.getWorld(world), coords[0], coords[1], coords[2]);
        }
    }

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
        trigger = new RocketLocation((String)rocket.get("Trigger"));
        if(((String)rocket.get("Destination")).equals("null")) {
            // Upgrade from v1.1.0 and earlier where Random did not have a destination
            destination = new RocketLocation((String)rocket.get("Trigger"));
        } else {
            destination = new RocketLocation((String)rocket.get("Destination"));
        }
        radius = (Double)rocket.get("Radius");
    }

    public Map<String, Object> serialize() {
        Map<String, Object> rocket = new HashMap<String, Object>();
        rocket.put("Type", type.toString());
        rocket.put("Trigger", trigger.toString());
        rocket.put("Destination",  destination.toString());
        rocket.put("Radius", radius);
        return rocket;
    }

	RocketType getType() {
		return type;
	}
	
	Location getTrigger() { return trigger.getLocation(); }
	
	Location getDestination() {	return destination.getLocation(); }
	
	double getRadius() {
		return radius;
	}
	
	void setTrigger(Location trigger) { this.trigger = new RocketLocation(trigger); }
	
	void setDestination(Location destination) {	this.destination = new RocketLocation(destination); }
	
	/*boolean setRadius(double radius) {
		if(this.type == RocketType.RANDOM) {
			this.radius = radius;
			return true;
		}
		return false;
	}*/
}
