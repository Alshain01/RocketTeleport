package io.github.alshain01.rocketteleport;

import com.earth2me.essentials.Essentials;
import net.ess3.api.InvalidWorldException;
import com.earth2me.essentials.commands.WarpNotFoundException;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Warp {
    final private Map<String, TeleportLocation> warps = new HashMap<String, TeleportLocation>();
    final private RocketTeleport plugin;

    Warp(RocketTeleport plugin) {
        this.plugin = plugin;
        importEssentials();
    }

    Warp(RocketTeleport plugin, ConfigurationSection data) {
        this.plugin = plugin;
        for(String s : data.getKeys(false)) {
            this.warps.put(s, new TeleportLocation(data.getString(s)));
        }

        importEssentials();
    }

    /**
     * Exception thrown when the warp name provided can not be used.
     */
    public class IllegalWarpNameException extends IllegalArgumentException {
        IllegalWarpNameException(String message) { super(message); }
    }

    private void importEssentials() {
        if(plugin.getServer().getPluginManager().isPluginEnabled("Essentials")) {
            Essentials essentials = (Essentials)plugin.getServer().getPluginManager().getPlugin("Essentials");
            for(String w : essentials.getWarps().getList()) {
                if(!warps.containsKey(w)) {
                    try {
                        warps.put(w, new TeleportLocation(essentials.getWarps().getWarp(w)));
                    } catch (WarpNotFoundException ex) {
                        plugin.getLogger().info("Failed to import Essentials warp " + w);
                    } catch (InvalidWorldException ex) {
                        plugin.getLogger().info("Failed to import Essentials warp " + w);
                    }
                }
            }
        }
    }

    void write(ConfigurationSection data) {
        for(String s : warps.keySet()) {
            data.set(s, warps.get(s).toString());
        }
    }

    /**
     * Gets an active warp's location by name
     *
     * @param warpName The warp name
     * @return The TeleportLocation for the warp or null
     * @throws IllegalArgumentException - if warp name is null
     */
    public TeleportLocation get(String warpName) {
        Validate.notNull(warpName);
        return warps.get(warpName);
    }

    /**
     * Return the set of active warp names
     *
     * @return The set of warp names
     */
    public Set<String> get() {
        return  warps.keySet();
    }

    /**
     * Adds a new warp
     *
     * @param warpName The name of the warp
     * @param location The destination of the warp
     * @throws IllegalArgumentException - if warp name or location is null
     * @throws IllegalWarpNameException - if a warp with that name exists
     */
    public void add(String warpName, Location location) {
        Validate.notNull(warpName);
        Validate.notNull(location);
        if(warps.containsKey(warpName)) { throw new IllegalWarpNameException("The provided warp name already exists."); }

        warps.put(warpName, new TeleportLocation(location, true, true, true));
        // Mirror new warp to Essentials
        if(plugin.getServer().getPluginManager().isPluginEnabled("Essentials")) {
            Essentials essentials = (Essentials)plugin.getServer().getPluginManager().getPlugin("Essentials");
            try {
                essentials.getWarps().setWarp(warpName, location);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to mirror new warp + " + warpName + " in Essentials.");
            }
        }
    }

    /**
     * Gets if a warp exists with a given name
     *
     * @param warpName The name of the warp to check
     * @return true if a warp by that name exists
     * @throws IllegalArgumentException - if warp name is null
     */
    public boolean isWarp(String warpName) {
        Validate.notNull(warpName);
        return warps.containsKey(warpName);
    }

    /**
     * Removes a warp
     *
     * @param warpName The warp name to remove
     * @throws IllegalArgumentException - if warp name or location is null
     * @throws IllegalWarpNameException - if a warp with that name exists
     */
    public void remove(String warpName) {
        Validate.notNull(warpName);
        if(!warps.containsKey(warpName)) { throw new IllegalWarpNameException("The provided warp name does not exist."); }
        warps.remove(warpName);

        // Remove the mirror in Essentials
        if(plugin.getServer().getPluginManager().isPluginEnabled("Essentials")) {
            Essentials essentials = (Essentials)plugin.getServer().getPluginManager().getPlugin("Essentials");
            try {
                essentials.getWarps().removeWarp(warpName);
            } catch (Exception ex) {
                plugin.getLogger().warning("Removed warp " + warpName + " but failed to find mirroed warp in Essentials.");
            }
        }
    }
}
