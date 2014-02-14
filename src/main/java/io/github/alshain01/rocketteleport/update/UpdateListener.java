package io.github.alshain01.rocketteleport.update;

import io.github.alshain01.rocketteleport.Message;
import io.github.alshain01.rocketteleport.update.Updater.UpdateResult;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateListener implements Listener {
    private final UpdateScheduler scheduler;

    public UpdateListener(UpdateScheduler scheduler) {
        this.scheduler = scheduler;
    }

    // Update listener
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerJoin(PlayerJoinEvent e) {
        if(scheduler == null) { return; }
        if (e.getPlayer().hasPermission("rocketteleport.notifyupdate")) {
            if(scheduler.getResult() == UpdateResult.UPDATE_AVAILABLE) {
                e.getPlayer().sendMessage(Message.UPDATE_AVAILABLE.get());
            } else if(scheduler.getResult() == UpdateResult.SUCCESS) {
                e.getPlayer().sendMessage(Message.UPDATE_DOWNLOADED.get());
            }
        }
    }
}
