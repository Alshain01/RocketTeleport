package io.github.alshain01.rocketteleport;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class WarpCommand implements CommandExecutor{
    private final Warp warpController;
    private final MissionControl missionControl;

    WarpCommand(Warp warpController, MissionControl missionControl) {
        this.warpController = warpController;
        this.missionControl = missionControl;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(isCommandPermitted(sender, command.getName())) {
            if(!command.getName().equals("warp") && args.length < 1) { return false; }

            if (command.getName().equalsIgnoreCase("warp")) {
                warp(sender, args);
                return true;
            } else if (command.getName().equalsIgnoreCase("delwarp")) {
                delWarp(sender, args);
                return true;
            } else if (command.getName().equalsIgnoreCase("setwarp")) {
                if (isPlayerSender(sender)) {
                    setWarp(sender, args);
                    return true;
                }
            }
        } else {
            return true;
        }
        return false;
    }

    private void warp(CommandSender sender, String[] args) {
        if (args.length < 1) {
            // List all warps
            boolean first = true;
            StringBuilder warpNames = new StringBuilder();

            for(String s : warpController.get()) {
                if(!first)
                    warpNames.append(", ");
                else
                    first = false;

                warpNames.append(s);
            }
            sender.sendMessage(Message.WARP_LIST.get().replace("{Warps}", warpNames.toString()));
        } else {
            // Warp to destination
            if(isPlayerSender(sender)) {
                if(warpController.isWarp(args[0])) {
                    sender.sendMessage(Message.WARP_ACTION.get().replace("{Warp}", args[0]));
                    missionControl.liftOff((Player)sender, warpController.get(args[0]).getLocation(), true, true);
                } else {
                    sender.sendMessage(Message.INVALID_WARP_ERROR.get().replace("{Warp}", args[0]));
                }
            }
        }
    }

    private void delWarp(CommandSender sender, String[] args) {
        if(!warpController.isWarp(args[0])) {
            sender.sendMessage(Message.INVALID_WARP_ERROR.get().replace("{Warp}", args[0]));
            return;
        }

        // Remove the warp
        warpController.remove(args[0]);
        sender.sendMessage(Message.WARP_REMOVED.get().replace("{Warp}", args[0]));
    }

    private void setWarp(CommandSender sender, String[] args) {
        // Create new warp
        if(warpController.isWarp(args[0])) {
            sender.sendMessage(Message.WARP_EXISTS_ERROR.get().replace("{Warp}", args[0]));
            return;
        }

        sender.sendMessage(Message.WARP_CREATED.get().replace("{Warp}", args[0]));
        warpController.add(args[0], ((Player) sender).getLocation());

    }

    private boolean isCommandPermitted(CommandSender sender, String command) {
        if(sender.hasPermission("rocketteleport." + command.toLowerCase())) {
            return true;
        }
        sender.sendMessage(Message.COMMAND_ERROR.get());
        return false;
    }

    private boolean isPlayerSender(CommandSender sender) {
        if(sender instanceof Player) {
            return true;
        }
        sender.sendMessage(Message.CONSOLE_ERROR.get());
        return false;
    }
}
