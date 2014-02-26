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

import org.bukkit.ChatColor;

public enum Message {
    COMMAND_ERROR, CANCEL_ERROR, RADIUS_ERROR, ROCKET_ERROR,
    CREATION_ERROR, LOCATION_ERROR, INVALID_WARP_ERROR,
    INSTRUCTION, LAND_INSTRUCTION, COMMAND_INSTRUCTION, TRIGGER_DESTROYED,
    WARP_REMOVED, WARP_CREATED, WARP_LIST,
    CANCEL_ROCKET, ROCKET_CREATED,
    UPDATE_AVAILABLE, UPDATE_DOWNLOADED;

    public String get() {
        String message = RocketTeleport.message.getConfig().getString("Message." + this.toString());
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
