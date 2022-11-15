package us.dot.its.jpo.conflictmonitor.monitor.algorithms;

import java.util.concurrent.CompletableFuture;


/**
 * General interface for an algorithm that can be configured, started, and stopped.
 * 
 * @param TParameters Type of configuration parameters the algorithm requires.
 */
public interface Algorithm<TParameters> {

    /**
     * This should be called before {@link start}
     * 
     * @param parameters The configuration parameters used by the algorithm.
     */
    void setParameters(TParameters parameters);

     /**
     * Starts running the algorithm to process messages
     * <p>
     * The algorithm should run in the background so that this method does not block the caller.
     * 
     */
    void start();

    /**
     * Stops running the algorithm.
     * <p>
     * This can be called if the algorithm's configuration parameters need to be changed
     * and the algorithm restarted.
     * <p>
     * This method should run synchronously so that the algorithm is stopped when it returns. It may block the
     * caller, but should not block indefinitely.
     * 
     */
    void stop();
}
