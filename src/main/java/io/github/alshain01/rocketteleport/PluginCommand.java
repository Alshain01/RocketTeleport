/*
 * Copyright (c) 2/16/14 1:50 PM Kevin Seiden. All rights reserved.
 *
 *  This works is licensed under the Creative Commons Attribution-NonCommercial 3.0
 *
 *  You are Free to:
 *     to Share: to copy, distribute and transmit the work
 *     to Remix: to adapt the work
 *
 *  Under the following conditions:
 *     Attribution: You must attribute the work in the manner specified by the author (but not in any way that suggests that they endorse you or your use of the work).
 *     Non-commercial: You may not use this work for commercial purposes.
 *
 *  With the understanding that:
 *     Waiver: Any of the above conditions can be waived if you get permission from the copyright holder.
 *     Public Domain: Where the work or any of its elements is in the public domain under applicable law, that status is in no way affected by the license.
 *     Other Rights: In no way are any of the following rights affected by the license:
 *         Your fair dealing or fair use rights, or other applicable copyright exceptions and limitations;
 *         The author's moral rights;
 *         Rights other persons may have either in the work itself or in how the work is used, such as publicity or privacy rights.
 *
 *  Notice: For any reuse or distribution, you must make clear to others the license terms of this work. The best way to do this is with a link to this web page.
 *  http://creativecommons.org/licenses/by-nc/3.0/
 */

package io.github.alshain01.rocketteleport;

import io.github.alshain01.rocketteleport.Rocket.RocketType;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.util.UUID;

class PluginCommand implements CommandExecutor{
    enum PluginCommandType {
        SOFT(1, "soft", false), HARD(1, "hard", false), RANDOM(2, "random <radius>", false),
        LAND(1, "land", false), CANCEL(1, "cancel", false), VILLAGER14(1, "villager14", true),
        RELOAD(1, "reload", true), SAVE(1, "save", true);

        private final int totalArgs;
        private final String help;
        private final boolean hidden;

        PluginCommandType(int minArgs, String help, boolean hidden) {
            this.hidden = hidden;
            totalArgs = minArgs;
            this.help = help;
        }

        public boolean isHidden() {
            return hidden;
        }

        public String getHelp() {
            return "/rocketteleport " + help;
        }

        public int getTotalArgs() {
            return totalArgs;
        }

        public boolean hasPermission(Permissible sender) {
            return sender.hasPermission("rocketteleport." + this.toString().toLowerCase());
        }
    }

    private final RocketTeleport plugin;

    PluginCommand(RocketTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(getHelp(sender));
            return true;
        }

        final PluginCommandType action = getAction(args[0].toUpperCase(), sender);
        if(action == null) { return true; }

        // Check that there are enough arguments
        if(args.length < action.getTotalArgs()) {
            sender.sendMessage(action.getHelp());
            return true;
        }

        // Console actions
        switch(action) {
            case RELOAD:
                plugin.reload();
                sender.sendMessage(ChatColor.GREEN + "" + ChatColor.ITALIC + "RocketTeleport Reloaded");
                return true;
            case SAVE:
                plugin.writeData();
                sender.sendMessage(ChatColor.GREEN + "" + ChatColor.ITALIC + "RocketTeleport Data Saved");
                return true;
            default:
                break;
        }

        // All of the remaining commands require interaction with ingame blocks
        if(!(sender instanceof Player)) {
            sender.sendMessage(getHelp(sender));
            return true;
        }

        final Player player = (Player) sender;
        final UUID pID = player.getUniqueId();

        // Check the permissions
        if(!action.hasPermission(player)) {
            player.sendMessage(Message.COMMAND_ERROR.get());
            return true;
        }

        // Is the player attempting to create a cannon while one is already in the queue?
        switch (action) {
            case SOFT:
            case HARD:
            case RANDOM:
            case VILLAGER14:
                if(plugin.rocketQueue.containsKey(pID)) {
                    sender.sendMessage(Message.CREATION_ERROR.get());
                    return true;
                }
            default:
                break;
        }

        // Perform the command
        switch (action) {
            case CANCEL:
                if(plugin.rocketQueue.containsKey(pID)) {
                    plugin.commandQueue.remove(pID);
                    plugin.rocketQueue.remove(pID);
                    sender.sendMessage(Message.CANCEL_ROCKET.get());
                    return true;
                }
                sender.sendMessage(Message.CANCEL_ERROR.get());
                return true;
            case LAND:
                if (!plugin.rocketQueue.containsKey(pID)) {
                    sender.sendMessage(Message.ROCKET_ERROR.get());
                    return true;
                }

                plugin.commandQueue.put(pID, action);
                sender.sendMessage(Message.LAND_INSTRUCTION.get());
                return true;
            case VILLAGER14:
                plugin.commandQueue.put(pID, action);
                plugin.rocketQueue.put(pID, new Rocket(RocketType.ELEMENT));
                sender.sendMessage(ChatColor.GOLD +
                        "Left click the button or plate to use as an " + ChatColor.BLUE +
                        "Element Animation" + ChatColor.GOLD + " rocket trigger.");
                return true;
            case SOFT:
            case HARD:
                plugin.rocketQueue.put(pID, new Rocket(RocketType.valueOf(action.toString())));
                break;
            case RANDOM:
                double radius;
                try {
                    radius = Double.valueOf(args[1]);
                } catch(NumberFormatException ex) {
                    sender.sendMessage(Message.RADIUS_ERROR.get());
                    return true;
                }

                plugin.rocketQueue.put(pID, new Rocket(radius));
                break;
            default:
                getHelp(player);
                return true;
        }

        plugin.commandQueue.put(pID, action);
        sender.sendMessage(Message.INSTRUCTION.get().replace("{Type}", action.toString().toLowerCase()));
        return true;
    }

    private String getHelp(Permissible sender) {
        if(sender instanceof ConsoleCommandSender) {
            return "/rocketteleport <save | reload>";
        }

        StringBuilder helpText = new StringBuilder("/rocketteleport <");
        boolean first = true;
        for(PluginCommandType a : PluginCommandType.values()) {
            if(a.isHidden()) { continue; }
            if(a.hasPermission(sender)) {
                if(!first) { helpText.append(" | "); }
                helpText.append(a.toString().toLowerCase());
                first = false;
            }
        }
        return helpText.append(">").toString();
    }

    private PluginCommandType getAction(String action, CommandSender sender) {
        try {
            return PluginCommandType.valueOf(action.toUpperCase());
        } catch(IllegalArgumentException e) {
            sender.sendMessage(getHelp(sender));
            return null;
        }
    }
}
