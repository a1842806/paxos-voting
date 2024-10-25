package council.voting.member;

import java.util.Random;

/**
 * Defines different response profiles for council members.
 * Includes methods to simulate delays and message dropping.
 */
public enum ResponseProfile {
    IMMEDIATE(10, 0.99),      // M1-like behavior: fast responses
    INTERMITTENT(5000, 0.7),  // M2-like behavior: slow, sometimes unresponsive
    UNRELIABLE(1000, 0.8),    // M3-like behavior: sometimes fails
    NORMAL(500, 0.95);        // M4-M9 behavior: regular response times

    private final int maxDelay;
    private final double reliability;
    private final Random random = new Random();

    ResponseProfile(int maxDelay, double reliability) {
        this.maxDelay = maxDelay;
        this.reliability = reliability;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public double getReliability() {
        return reliability;
    }

    /**
     * Returns a random delay based on the profile's maximum delay.
     * @return A random delay in milliseconds.
     */
    public long getRandomDelay() {
        return (long) (random.nextDouble() * maxDelay);
    }

    /**
     * Determines whether to drop a message based on the profile's reliability.
     * @return true if the message should be dropped, false otherwise.
     */
    public boolean shouldDropMessage() {
        return random.nextDouble() > reliability;
    }

    /**
     * Provides an individual message timeout, used when waiting for responses.
     * @return The timeout in milliseconds.
     */
    public long getIndividualMessageTimeout() {
        return maxDelay + 1000; // Add buffer to the max delay
    }
}