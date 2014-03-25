package io.github.alshain01.rocketteleport;

import com.earth2me.essentials.Essentials;
import net.ess3.api.InvalidWorldException;
import com.earth2me.essentials.commands.WarpNotFoundException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

class WarpCommand implements CommandExecutor {
    final private Map<String, WarpLocation> warps = new HashMap<String, WarpLocation>();
    final private RocketTeleport plugin;

    WarpCommand(RocketTeleport plugin) {
        this.plugin = plugin;
        importEssentials();
    }

    WarpCommand(RocketTeleport plugin, ConfigurationSection data) {
        this.plugin = plugin;
        for(String s : data.getKeys(false)) {
            this.warps.put(s, new WarpLocation(data.getString(s)));
        }

        importEssentials();
    }

    void importEssentials() {
        if(plugin.getServer().getPluginManager().isPluginEnabled("Essentials")) {
            Essentials essentials = (Essentials)plugin.getServer().getPluginManager().getPlugin("Essentials");
            for(String w : essentials.getWarps().getList()) {
                if(!warps.containsKey(w)) {
                    try {
                        warps.put(w, new WarpLocation(essentials.getWarps().getWarp(w), false));
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

    private class WarpLocation {
        private final String world;
        private final double coords[] = new double[3];

        WarpLocation(Location location, boolean adjustY) {
            // Adjust coordinates to center block.
            // Use absolute value to produce 1 or -1 in order to
            // add 0.5 if the coord is positive, subtract if the coord is negative
            coords[0] = location.getBlockX() + (location.getBlockX() / Math.abs(location.getBlockX())) * 0.5;
            coords[1] = location.getBlockY() + (adjustY ? 1 : 0);
            coords[2] = location.getBlockZ() + (location.getBlockX() / Math.abs(location.getBlockX())) * 0.5;
            world = location.getWorld().getName();
        }

        WarpLocation(String location) {
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("warp")) {
            if(!sender.hasPermission("rocketteleport.warp")) {
                sender.sendMessage(Message.COMMAND_ERROR.get());
                return true;
            }

            if (args.length < 1) {
                StringBuilder warpNames = new StringBuilder();
                boolean first = true;
                for(String s : warps.keySet()) {
                    if(!first) {
                        warpNames.append(", ");
                    } else {
                        first = false;
                    }
                    warpNames.append(s);
                }
                warpNames.append(".");

                sender.sendMessage(Message.WARP_LIST.get().replace("{Warps}", warpNames.toString()));
            } else {
                if(!(sender instanceof Player)) {
                    sender.sendMessage(Message.CONSOLE_ERROR.get());
                } else {
                    // Warp to destination
                    if(warps.containsKey(args[0])) {
                        sender.sendMessage(Message.WARP_ACTION.get().replace("{Warp}", args[0]));
                        plugin.missionControl.liftOff((Player)sender, warps.get(args[0]).getLocation());
                    } else {
                        sender.sendMessage(Message.INVALID_WARP_ERROR.get().replace("{Warp}", args[0]));
                    }
                }
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("delwarp")) {
            if(!sender.hasPermission("rocketteleport.delwarp")) {
                sender.sendMessage(Message.COMMAND_ERROR.get());
                return true;
            }

            if(args.length < 1) { return false; }
            if(warps.remove(args[0]) != null) {
                sender.sendMessage(Message.WARP_REMOVED.get().replace("{Warp}", args[0]));
            } else {
                sender.sendMessage(Message.INVALID_WARP_ERROR.get().replace("{Warp}", args[0]));
            }
            return true;
        } else if (command.getName().equalsIgnoreCase("setwarp")) {
            // All the remaining commands require a player
            if(!(sender instanceof Player)) {
                sender.sendMessage(Message.CONSOLE_ERROR.get());
                return true;
            }

            if (args.length < 1) { return false; }
            if(!sender.hasPermission("rocketteleport.setwarp")) {
                sender.sendMessage(Message.COMMAND_ERROR.get());
                return true;
            }

            warps.put(args[0], new WarpLocation(((Player) sender).getLocation(), true));
            sender.sendMessage(Message.WARP_CREATED.get());
            return true;
        }
        return false;
    }
}
