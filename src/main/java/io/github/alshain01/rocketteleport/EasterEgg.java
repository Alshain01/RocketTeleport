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

        player.sendMessage("<&1Villager Police&f> Stop!");
        new MessageTask(player, "<&6" + player.getName() + "&f> No.").runTaskLater(plugin, TIME);
        new MessageTask(player, "<&1Villager Police&f> Aww, he said no.").runTaskLater(plugin, TIME * 2);
        new RocketVillagerTask(location).runTaskLater(plugin, TIME * 3);
        new MessageTask(player, "<&7Villager Console&f> Beep. Beep. Beep. Beep. Missile inbound!").runTaskLater(plugin, TIME * 4);
        new MessageTask(player, "<&2Villager Reporter&f> WHAT?!").runTaskLater(plugin, TIME * 5);
        new MessageTask(player, "<&7Villager Console&f> Deploying flares!").runTaskLater(plugin, (int)(TIME * 5.5));
        new MessageTask(player, "<&8Villager Hellicopter&f> Deploying flares.").runTaskLater(plugin, TIME * 6);
        new FireworkTask(location.add(0D, 45D, 0D)).runTaskLater(plugin, TIME * 7);
        new MessageTask(player, "<&7Villager Console&f> Ahhh, we're going down!").runTaskLater(plugin, TIME * 8);
        new MessageTask(player, "<&8Villager Hellicopter&f> We're going down.").runTaskLater(plugin, TIME * (int)(TIME * 8.5));
        new RainVillagersTask(location).runTaskLater(plugin, TIME * 9);
        new MessageTask(player, "<&3Villager #9&f> ...and that was the last we saw'r of those villagers.").runTaskLater(plugin, TIME * 10);
    }

    private Location getDropLocation(Location landingArea) {
        final int RADIUS = 15;
        double x = (landingArea.getX() - RADIUS) + Math.random() * (RADIUS*2);
        double z = (landingArea.getZ() - RADIUS) + Math.random() * (RADIUS*2);
        Location loc = new Location(landingArea.getWorld(), x, 0D, z);
        Block landing = loc.getWorld().getHighestBlockAt(loc);
        return landing.getLocation().add(0, 25, 0);
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

    private class RocketVillagerTask extends BukkitRunnable {
        final Location location;

        RocketVillagerTask(Location location) {
            this.location = location;
        }
        public void run() {
            Entity entity = location.getWorld().spawnEntity(location, EntityType.VILLAGER);
            entity.getWorld().playSound(entity.getLocation(), Sound.EXPLODE, 20, 0);
            entity.teleport(entity.getLocation().add(0D, 1D, 0D)); // Prevents player from getting "stuck" on pressure plate
            entity.setVelocity(new Vector(0D, 10D, 0D));
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
