package org.mineacademy.velocitycontrol.foundation.model;

public interface Rule {

    /**
     * Return the uniquely identifying expression that identifies this rule
     *
     * @return
     */
    String getUid();

    /**
     * Attempts to parse the given line from {@link RuleSetReader} containing an operator such as
     * "then warn Do not spam please." etc. for the given rule that is being created:
     * then the args are "then" "warn" "Do" "not" "spam" "please."
     *
     * You can use {@link Common#joinRange(int, String[])} to join the message together.
     *
     * @param rule
     * @param args
     *
     * @return true if the operator was parsed successfully
     */
    boolean onOperatorParse(String[] args);
}
