/*
 * Copyright (c) 2/16/14 1:49 PM Kevin Seiden. All rights reserved.
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

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

class EasterEgg {
    private final Plugin plugin = Bukkit.getPluginManager().getPlugin("RocketTeleport");
    private final int TIME = 40;

    void run(Player player, Location location) {

        new MessageTask(player, "<&1Villager Police&f> Stop!").runTask(plugin);
        new MessageTask(player, "<&6" + player.getName() + "&f> No.").runTaskLater(plugin, TIME);
        new PrepareRocketVillagerTask(player, location).runTaskLater(plugin, TIME * 2);
        new MessageTask(player, "<&7Villager Console&f> Beep. Beep. Beep. Beep. Missile inbound!").runTaskLater(plugin, TIME * 4);
        new MessageTask(player, "<&2Villager Reporter&f> WHAT?!").runTaskLater(plugin, TIME * 5);
        new MessageTask(player, "<&7Villager Console&f> Deploying flares!").runTaskLater(plugin, (int)(TIME * 5.5));
        new MessageTask(player, "<&8Villager Hellicopter&f> Deploying flares.").runTaskLater(plugin, TIME * 6);
        new FireworkTask(location.add(0D, 20D, 0D)).runTaskLater(plugin, TIME * 7);
        new MessageTask(player, "<&7Villager Console&f> Ahhh, we're going down!").runTaskLater(plugin, TIME * 8);
        new MessageTask(player, "<&8Villager Hellicopter&f> We're going down.").runTaskLater(plugin, (int)(TIME * 8.5));
        new RainVillagersTask(location).runTaskLater(plugin, TIME * 9);
        new MessageTask(player, "<&3Villager #9&f> ...and that was the last we saw of those villagers.").runTaskLater(plugin, TIME * 10);
    }

    private Location getDropLocation(Location landingArea) {
        final int RADIUS = 15;
        double x = (landingArea.getX() - RADIUS) + Math.random() * (RADIUS*2);
        double z = (landingArea.getZ() - RADIUS) + Math.random() * (RADIUS*2);
        Location loc = new Location(landingArea.getWorld(), x, 0D, z);
        Block landing = loc.getWorld().getHighestBlockAt(loc);
        return landing.getLocation().add(0, 30, 0);
    }

    private class MessageTask extends BukkitRunnable {
        final Player player;
        final String message;

        MessageTask(Player player, String message) {
            this.player = player;
            this.message = ChatColor.translateAlternateColorCodes('&', message);
        }

        @Override
        public void run() {
            player.sendMessage(message);
        }
    }

    private class PrepareRocketVillagerTask extends BukkitRunnable {
        Player player;
        Location location;
        PrepareRocketVillagerTask(Player player, Location location) {
            this.player = player;
            this.location = location;
        }

        @Override
        public void run() {
            new MessageTask(player, "<&1Villager Police&f> Aww, he said no.").runTask(plugin);
            Entity entity = location.getWorld().spawnEntity(location.getWorld().getHighestBlockAt(location).getLocation(), EntityType.VILLAGER);
            new RocketVillagerTask(entity).runTaskLater(plugin, TIME);
        }


    }

    private class RocketVillagerTask extends BukkitRunnable {
        final Entity entity;

        RocketVillagerTask(Entity entity) {
            this.entity = entity;
        }
        public void run() {
            entity.getWorld().playSound(entity.getLocation(), Sound.EXPLODE, 20, 0);
            entity.teleport(entity.getLocation().add(0D, 1D, 0D)); // Prevents player from getting "stuck" on pressure plate
            entity.setVelocity(new Vector(0D, 5D, 0D));
            new DespawnTask(entity).runTaskLater(plugin, TIME);
        }
    }

    private class RainVillagersTask extends BukkitRunnable {
        final Location location;
        RainVillagersTask(Location location) {
            this.location = location;
        }

        public void run() {
            for(int x = 0; x < 15; x++) {
                Entity entity = location.getWorld().spawnEntity(getDropLocation(location), EntityType.VILLAGER);
                new DespawnTask(entity).runTaskLater(plugin, TIME * 3);
            }
        }
    }

    private class DespawnTask extends BukkitRunnable {
        final Entity entity;

        DespawnTask(Entity entity) {
            this.entity = entity;
        }

        public void run() {
            entity.remove();
        }
    }

    private class FireworkTask extends BukkitRunnable {
        final Location location;

        FireworkTask(Location location) {
            this.location = location;
        }

        @Override
        public void run() {
            Firework firework = location.getWorld().spawn(location, Firework.class);
            FireworkMeta data = firework.getFireworkMeta();
            data.addEffects(FireworkEffect.builder().withColor(Color.ORANGE).with(FireworkEffect.Type.BALL_LARGE).build());
            data.setPower(1);
            firework.setFireworkMeta(data);
            firework.detonate();
        }
    }
}
