package org.mineacademy.velocitycontrol.foundation.exception;

import org.mineacademy.velocitycontrol.foundation.Debugger;

/**
 * Represents our core exception. All exceptions of this
 * kind are logged automatically to the error.log file
 */
public class VCException extends RuntimeException {

    /**
     * Create a new exception and logs it
     *
     * @param t
     */
    public VCException(Throwable t) {
        super(t);

        Debugger.saveError(t);
    }

    /**
     * Create a new exception and logs it
     *
     * @param message
     */
    public VCException(String message) {
        super(message);

        Debugger.saveError(this, message);
    }

    /**
     * Create a new exception and logs it
     *
     * @param message
     * @param t
     */
    public VCException(Throwable t, String message) {
        super(message, t);

        Debugger.saveError(t, message);
    }

    @Override
    public String getMessage() {
        return "Report / " + super.getMessage();
    }
}