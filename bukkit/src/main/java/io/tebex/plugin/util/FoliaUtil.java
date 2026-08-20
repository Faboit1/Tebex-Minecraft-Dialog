package io.tebex.plugin.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FoliaUtil {
    private static final boolean FOLIA;

    static {
        boolean f;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            f = true;
        } catch (ClassNotFoundException e) {
            f = false;
        }
        FOLIA = f;
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static void runAsync(Plugin plugin, Runnable runnable) {
        if (FOLIA) {
            try {
                Object scheduler = getAsyncScheduler();
                Consumer<Object> consumer = task -> runnable.run();
                scheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class)
                        .invoke(scheduler, plugin, consumer);
                return;
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public static void runAsyncLater(Plugin plugin, Runnable runnable, long delayTicks) {
        if (FOLIA) {
            try {
                Object scheduler = getAsyncScheduler();
                Consumer<Object> consumer = task -> runnable.run();
                long delayMs = Math.max(1, delayTicks * 50);
                scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class)
                        .invoke(scheduler, plugin, consumer, delayMs, TimeUnit.MILLISECONDS);
                return;
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayTicks);
    }

    public static void runAsyncTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        if (FOLIA) {
            try {
                Object scheduler = getAsyncScheduler();
                Consumer<Object> consumer = task -> runnable.run();
                long delayMs = Math.max(1, delayTicks * 50);
                long periodMs = Math.max(1, periodTicks * 50);
                scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class)
                        .invoke(scheduler, plugin, consumer, delayMs, periodMs, TimeUnit.MILLISECONDS);
                return;
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
    }

    public static void runSync(Plugin plugin, Runnable runnable) {
        if (FOLIA) {
            try {
                Object scheduler = getGlobalRegionScheduler();
                Consumer<Object> consumer = task -> runnable.run();
                scheduler.getClass().getMethod("run", Plugin.class, Consumer.class)
                        .invoke(scheduler, plugin, consumer);
                return;
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static void runSyncLater(Plugin plugin, Runnable runnable, long delayTicks) {
        if (FOLIA) {
            try {
                Object scheduler = getGlobalRegionScheduler();
                Consumer<Object> consumer = task -> runnable.run();
                scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class)
                        .invoke(scheduler, plugin, consumer, Math.max(1, delayTicks));
                return;
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
    }

    public static void runForEntity(Plugin plugin, Entity entity, Runnable runnable) {
        if (FOLIA) {
            try {
                Object entityScheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                Consumer<Object> consumer = task -> runnable.run();
                entityScheduler.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class)
                        .invoke(entityScheduler, plugin, consumer, (Runnable) null);
                return;
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    private static Object getAsyncScheduler() throws Exception {
        Method m = Bukkit.getServer().getClass().getMethod("getAsyncScheduler");
        return m.invoke(Bukkit.getServer());
    }

    private static Object getGlobalRegionScheduler() throws Exception {
        Method m = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler");
        return m.invoke(Bukkit.getServer());
    }
}
