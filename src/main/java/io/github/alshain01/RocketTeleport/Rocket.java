package io.github.alshain01.RocketTeleport;

import org.bukkit.Location;

public class Rocket {
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
