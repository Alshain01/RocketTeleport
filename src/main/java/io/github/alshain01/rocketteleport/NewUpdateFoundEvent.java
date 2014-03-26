package io.github.alshain01.rocketteleport;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

class NewUpdateFoundEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    public NewUpdateFoundEvent() {
        super(true);
    }

    @SuppressWarnings("unused") // API
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
