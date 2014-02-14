package io.github.alshain01.rocketteleport;

import org.bukkit.permissions.Permissible;

enum PluginCommandType {
    SOFT(1, "soft"), HARD(1, "hard"), RANDOM(2, "random <radius>"),
    LAND(1, "land"), CANCEL(1, "cancel"), JASON(1, "jason"),
    RELOAD(1, "reload"), SAVE(1, "save");


    private final int totalArgs;
    private final String help;

    PluginCommandType(int minArgs, String help) {
        totalArgs = minArgs;
        this.help = help;
    }

    /*public String getMessage() {
        return Message.valueOf(this.toString()).get();
    }*/

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
