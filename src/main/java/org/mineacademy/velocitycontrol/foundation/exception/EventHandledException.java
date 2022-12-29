package org.mineacademy.velocitycontrol.foundation.exception;

import lombok.Getter;

/**
 * Represents a silent exception thrown then handling events,
 * this will only send the event player a message
 */
public final class EventHandledException extends CommandException {

    /**
     * Should we cancel this event?
     */
    @Getter
    private final boolean cancelled;

    /**
     * Create a new command exception with messages for the command sender
     * @param cancelled
     *
     * @param messages
     */
    public EventHandledException(boolean cancelled, String... messages) {
        super(messages);

        this.cancelled = cancelled;
    }
}