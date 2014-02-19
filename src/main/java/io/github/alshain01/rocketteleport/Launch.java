package io.github.alshain01.rocketteleport;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

class Launch {
    final Player player;
    final Location destination;

    Launch(Player player, Location destination) {
        this.player = player;
        this.destination = destination;
    }

    void liftOff() {
        if((int)player.getLocation().getY() < player.getWorld().getHighestBlockYAt(player.getLocation())) {
            player.teleport(destination);
            return;
        }

        player.getWorld().playSound(player.getLocation(), Sound.EXPLODE, 20, 0);
        player.teleport(player.getLocation().add(0D,1D,0D)); // Prevents player from getting "stuck" on pressure plate
        player.setVelocity(new Vector(0D, 10D, 0D));
        new Teleport().runTaskLater(Bukkit.getServer().getPluginManager().getPlugin("RocketTeleport"), 40);
    }

    /*
     * Runnable class for handling delayed teleport
     */
    private class Teleport extends BukkitRunnable {
        @Override
        public void run() {
            player.setVelocity(new Vector(0D, 0D, 0D));
            player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
    }
}
