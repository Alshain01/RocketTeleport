package io.github.alshain01.rocketteleport;

import org.bukkit.ChatColor;

public enum Message {
    COMMAND_ERROR, CANCEL_ERROR, RADIUS_ERROR, ROCKET_ERROR, CREATION_ERROR,
    INSTRUCTION, LAND_INSTRUCTION,
    CANCEL_ROCKET,
    UPDATE_AVAILABLE, UPDATE_DOWNLOADED;

    public String get() {
        String message = RocketTeleport.message.getConfig().getString("Message." + this.toString());
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
