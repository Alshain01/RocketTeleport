package io.github.alshain01.rocketteleport;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class TeleportLocation {
    private final String world;
    private final double coords[] = new double[3];
    private final float view[] = new float[2];

    /**
     * Creates a new Teleport location that records all parameters with no adjustments.
     *
     * @param location The location to create a TeleportLocation from
     * @throws IllegalArgumentException - if location is null
     */
    public TeleportLocation(Location location) {
        this(location, false, false, true);
    }

    /**
     * Creates a new TeleportLocation
     *
     * @param location The location to create a TeleportLocation from
     * @param normalize X and Z coordinates will be adjusted to n.5
     * @param adjustY Y coordinate will be adjusted n + 1 (for PlayerInteractEvent locations)
     * @param storeRotation Yaw and Pitch will be recorded, recorded as 0 if false.
     * @throws IllegalArgumentException - if location is null
     */
    public TeleportLocation(Location location, boolean normalize, boolean adjustY, boolean storeRotation) {
        Validate.notNull(location);
        world = location.getWorld().getName();

        coords[0] = normalize ? normalize(location.getBlockX()) : location.getBlockX();
        coords[1] = location.getBlockY() + (adjustY ? 1 : 0);
        coords[2] = normalize ? normalize(location.getBlockZ()) : location.getBlockZ();

        view[0] = storeRotation ? location.getYaw() : 0F;
        view[1] = storeRotation ? location.getPitch() : 0F;
    }

    /**
     * Creates a TeleportLocation from it's string representation
     *
     * @param location The location to create a TeleportLocation from
     * @throws IllegalArgumentException - if the string is not properly formatted or location is null.
     * @throws NumberFormatException - if the coordinates could not be parsed.
     */
    TeleportLocation(String location) {
        Validate.notNull(location);
        String[] arg = location.split(",");
        if(arg.length != 4 && arg.length != 6) {
            throw new IllegalArgumentException("Malformed TeleportLocation string.");
        }

        world = arg[0];

        try {
            for (int a = 0; a < 3; a++) {
                coords[a] = Double.parseDouble(arg[a + 1]);
            }

            if (arg.length > 4) {
                view[0] = Float.parseFloat(arg[4]);
                view[1] = Float.parseFloat(arg[5]);
            } else {
                view[0] = 0F;
                view[1] = 0F;
            }
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("String does not contain valid teleport coordinates.");
        }
    }

    private double normalize(float coord) {
        // Adjust coordinates to center block.
        // Use absolute value to produce 1 or -1 in order to
        // add 0.5 if the coord is positive, subtract if the coord is negative
        // cast to int to truncate the decimal and impose our own.
        return (coord / Math.abs(coord)) * 0.5 + (int)coord;
    }

    /**
     * Gets a string representation of the TeleportLocation
     *
     * @return A string representation of the TeleportLocation.
     */
    @Override
    public String toString() {
        return world + "," + coords[0] + "," + coords[1] + "," + coords[2] + "," + view[0] + "," + view[1];
    }

    /**
     * Returns world name with a string of coordinates as integers.
     * Formatted especially for storing as a YAML key.
     * Not to be used as coordinates, use toString() instead.
     *
     * @return A uniquely formatted string.
     */
    String toKey() {
        return world + "," + (int)coords[0] + "," + (int)coords[1] + "," + (int)coords[2];
    }

    /**
     * Gets a Bukkit location for this TeleportLocation
     *
     * @return The bukkit location that represented by the TeleportLocation.
     */
    public Location getLocation() {
        return new Location(Bukkit.getWorld(world), coords[0], coords[1], coords[2], view[0], view[1]);
    }
}
