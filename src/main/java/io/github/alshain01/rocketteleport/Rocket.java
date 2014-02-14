package io.github.alshain01.rocketteleport;

import org.bukkit.Bukkit;
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

    class RocketLocation {
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

        public String toKey() {
            return world + "," + (int)coords[0] + "," + (int)coords[1] + "," + (int)coords[2];
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


        if(rocket.get("Destination") instanceof ArrayList<?>) {
            List<?> list = (ArrayList<?>)rocket.get("Destination");
            List<RocketLocation> locations = new ArrayList<RocketLocation>();
            for(Object o : list) {
                locations.add((RocketLocation)o);
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
        rocket.put("Destination",  destination.toString());
        rocket.put("Radius", radius);
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
