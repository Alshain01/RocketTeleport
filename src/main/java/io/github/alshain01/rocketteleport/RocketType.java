package io.github.alshain01.rocketteleport;

public enum RocketType {
	RANDOM, SOFT, HARD;

    public String getName() {
        // Make it look nice (Capital first, lower rest)
        return this.toString().substring(0, 1).toUpperCase() + this.toString().substring(1).toLowerCase();
    }
}
