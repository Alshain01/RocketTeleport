package io.github.alshain01.rocketteleport;

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
    }

    WarpCommand(RocketTeleport plugin, ConfigurationSection data) {
        this.plugin = plugin;
        for(String s : data.getKeys(false)) {
            this.warps.put(s, new WarpLocation(data.getString(s)));
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

        WarpLocation(Location location) {
            coords[0] = location.getBlockX() + 0.5;
            coords[1] = location.getBlockY() + 1;
            coords[2] = location.getBlockZ() + 0.5;
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
        if (args.length < 1) {
            if(!sender.hasPermission("rocketteleport.warp")) {
                sender.sendMessage(Message.COMMAND_ERROR.get());
                return true;
            }

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
            return true;
        }

        /*
         * Handle the Deletion of a warp
         */
        if(args[0].equalsIgnoreCase("delete")) {
            if(!sender.hasPermission("rocketteleport.warp.delete")) {
                sender.sendMessage(Message.COMMAND_ERROR.get());
                return true;
            }

            if(args.length < 2) {
                sender.sendMessage("/warp delete <name>");
                return true;
            }

            if(warps.remove(args[1]) != null) {
                sender.sendMessage(Message.WARP_REMOVED.get().replace("{Warp}", args[1]));
            } else {
                sender.sendMessage(Message.INVALID_WARP_ERROR.get().replace("{Warp}", args[1]));
            }
        } else {
            // All the remaining commands require a player
            if(!(sender instanceof Player)) {
                sender.sendMessage("/warp <delete> <name>");
                return true;
            }

            if(args[0].equalsIgnoreCase("set")) {
                if(!sender.hasPermission("rocketteleport.warp.set")) {
                    sender.sendMessage(Message.COMMAND_ERROR.get());
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage("/warp set <name>");
                    return true;
                }

                warps.put(args[1], new WarpLocation(((Player) sender).getLocation()));
                sender.sendMessage(Message.WARP_CREATED.get());
            } else {
                if(!sender.hasPermission("rocketteleport.warp")) {
                    sender.sendMessage(Message.COMMAND_ERROR.get());
                    return true;
                }

                // Warp to destination
                if(warps.containsKey(args[0])) {
                    plugin.missionControl.liftOff((Player)sender, warps.get(args[0]).getLocation());
                    return true;
                }
            }
            sender.sendMessage("/warp <name>");
        }
        return true;
    }
}
