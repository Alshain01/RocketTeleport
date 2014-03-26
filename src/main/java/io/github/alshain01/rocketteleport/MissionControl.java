package io.github.alshain01.rocketteleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class MissionControl implements Listener{
    final private Sound sound;
    final private float volume;
    final private float pitch;
    final private int delayTime;
    final private Map<UUID, BukkitTask> awaitingTeleport = new HashMap<UUID, BukkitTask>();

    MissionControl(ConfigurationSection sound, ConfigurationSection timers) {
        this.sound = Sound.valueOf(sound.getString("Type"));
        this.volume = (float)sound.getDouble("Volume");
        this.pitch = (float)sound.getDouble("Pitch");
        this.delayTime = timers.getInt("Delay");
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if(awaitingTeleport.containsKey(player.getUniqueId())) {
            e.getPlayer().sendMessage(Message.TELEPORT_CANCELLED.get());
            awaitingTeleport.get(player.getUniqueId()).cancel();
            if(awaitingTeleport.isEmpty()) {
                e.getHandlers().unregister(this);
            }
        }
    }

    void liftOff(Player player, Location destination, boolean delay) {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("RocketTeleport");

        BukkitTask task;
        if((int)player.getLocation().getY() < player.getWorld().getHighestBlockYAt(player.getLocation())) {
            task = new Teleport(player, destination, delay).runTaskLater(plugin, delayTime * 20);
        } else {
            task = new Launch(player, destination, delay).runTaskLater(plugin, delayTime * 20);
        }
        if(delay && delayTime > 0) {
            player.sendMessage(Message.TELEPORT_DELAY.get().replace("{Time}", String.valueOf(delayTime)));
            awaitingTeleport.put(player.getUniqueId(), task);
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    private class Launch extends BukkitRunnable {
        final Player player;
        final Location destination;
        final boolean advertise;

        Launch(Player player, Location destination, boolean advertise) {
            this.player = player;
            this.destination = destination;
            this.advertise = advertise;
        }

        @Override
        public void run() {
            if(advertise) {
                player.sendMessage(Message.TELEPORT_COMMENCING.get());
            }
            player.getWorld().playSound(player.getLocation(), sound, volume, pitch); // Fails silently if sound is null
            player.teleport(player.getLocation().add(0D,2D,0D)); // Prevents player from getting "stuck" on pressure plate
            player.setVelocity(new org.bukkit.util.Vector(0D, 10D, 0D));
            new Teleport(player, destination, false).runTaskLater(Bukkit.getServer().getPluginManager().getPlugin("RocketTeleport"), 40);
            awaitingTeleport.remove(player.getUniqueId());
        }
    }

    /*
     * Runnable class for handling delayed teleport
     */
    private class Teleport extends BukkitRunnable {
        final Player player;
        final Location destination;
        final boolean advertise;

        Teleport(Player player, Location destination, boolean advertise) {
            this.player = player;
            this.destination = destination;
            this.advertise = advertise;
        }

        @Override
        public void run() {
            if(advertise) {
                player.sendMessage(Message.TELEPORT_COMMENCING.get());
            }

            player.setVelocity(new Vector(0D, 0D, 0D));
            player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
            awaitingTeleport.remove(player.getUniqueId());
        }
    }}
