package io.github.alshain01.rocketteleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MissionControl {
    private Sound sound;
    private float volume;
    private float pitch;

    MissionControl(ConfigurationSection sound) {
        this.sound = Sound.valueOf(sound.getString("Type"));
        this.volume = (float)sound.getDouble("Volume");
        this.pitch = (float)sound.getDouble("Pitch");
    }

    void liftOff(Player player, Location destination) {
        if((int)player.getLocation().getY() < player.getWorld().getHighestBlockYAt(player.getLocation())) {
            player.teleport(destination);
            return;
        }

        player.getWorld().playSound(player.getLocation(), sound, volume, pitch); // Fails silently if sound is null
        player.teleport(player.getLocation().add(0D,2D,0D)); // Prevents player from getting "stuck" on pressure plate
        player.setVelocity(new org.bukkit.util.Vector(0D, 10D, 0D));
        new Teleport(player, destination).runTaskLater(Bukkit.getServer().getPluginManager().getPlugin("RocketTeleport"), 40);
    }

    /*
     * Runnable class for handling delayed teleport
     */
    private class Teleport extends BukkitRunnable {
        final Player player;
        final Location destination;

        Teleport(Player player, Location destination) {
            this.player = player;
            this.destination = destination;
        }

        @Override
        public void run() {
            player.setVelocity(new Vector(0D, 0D, 0D));
            player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
    }}
