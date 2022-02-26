package com.james090500.CoreFoundation.exception;

import lombok.Getter;

/**
 * Thrown when we check a regex
 * and the evaluation takes over the given limit
 */
@Getter
public final class RegexTimeoutException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The message that was being checked
	 */
	private final String checkedMessage;

	/**
	 * The execution limit in miliseconds
	 */
	private final long executionLimit;

	public RegexTimeoutException(CharSequence checkedMessage, long timeoutLimit) {
		this.checkedMessage = checkedMessage.toString();
		this.executionLimit = timeoutLimit;
	}
}