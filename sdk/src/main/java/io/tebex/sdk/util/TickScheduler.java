package io.tebex.sdk.util;

import java.util.*;
import java.util.concurrent.*;

/**
 * This is a simple scheduled task executor that allows tasks to run based on the game tick rate. All tasks using the tick
 * scheduler are executed on the main thread. Since Tebex allows only seconds-based command delays, sub-second precision is not implemented.
 *
 *  The tick() function must be called each server tick to check if tasks are available to run.
 *
 *  This is used to schedule due commands for players that will execute on the main thread while also allowing us to
 *  support delaying commands in sequence.
 */
public class TickScheduler {
    /** The game tick speed */
    private static final int TICKS_PER_SECOND = 20;

    /** Current game ticks */
    private static long _ticks = 0;

    /** Set of tasks with order maintained by scheduled tick time */
    private static final ConcurrentSkipListSet<TickTask> scheduledRunnables = new ConcurrentSkipListSet<>();

    /** Reserved empty list constant */
    private static final List<Runnable> NO_TASKS = new ArrayList<>();

    /**
     * Schedule a runnable to run at the next tick cycle.
     * @param r The function to run.
     */
    public static void scheduleNow(Runnable r) {
        scheduleLater(r, 0, TimeUnit.SECONDS);
    }

    /**
     * Schedules a runnable task to be executed after a specified delay.
     *
     * @param r The Runnable task to be scheduled.
     * @param initialDelay The initial delay before executing the task.
     * @param unit The time unit for the initial delay.
     */
    public static void scheduleLater(Runnable r, long initialDelay, TimeUnit unit) {
        if (initialDelay < 0) {
            throw new IllegalArgumentException("Scheduled delay cannot be negative");
        }

        long initialDelayTicks = unit.toSeconds(initialDelay) * TICKS_PER_SECOND;
        long runAt = _ticks + initialDelayTicks;

        // Create a new TickTask and add to scheduledRunnables
        TickTask task = new TickTask(runAt, r);
        scheduledRunnables.add(task);
    }

    /**
     * @return A list of runnables that are ready to be executed. They will be removed from the scheduled runnable map.
     */
    public static List<Runnable> tick() {
        _ticks++;
        if (scheduledRunnables.isEmpty()) {
            return NO_TASKS;
        }

        List<Runnable> dueTasks = new ArrayList<>();
        Iterator<TickTask> iterator = scheduledRunnables.iterator();
        while (iterator.hasNext()) {
            TickTask task = iterator.next();
            if (task.scheduledTick <= _ticks) {
                dueTasks.add(task.runnable); // move the runnable to due tasks
                iterator.remove(); // consume the runnable from our set
            } else {
                break;
            }
        }
        return dueTasks;
    }

    static class TickTask implements Comparable<TickTask> {
        private final long scheduledTick;
        private final Runnable runnable;

        public TickTask(long scheduledTick, Runnable runnable) {
            this.scheduledTick = scheduledTick;
            this.runnable = runnable;
        }

        // Required for ordering in the ConcurrentSkipListSet
        @Override
        public int compareTo(TickTask o) {
            int tickCompare = Long.compare(this.scheduledTick, o.scheduledTick);
            return tickCompare != 0 ? tickCompare : this.runnable.hashCode() - o.runnable.hashCode();
        }
    }
}