package net.crumb.lobbyParkour.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

public class SchedulerUtils {

    private static final boolean IS_FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        IS_FOLIA = folia;
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static void runTask(Plugin plugin, Runnable runnable) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runTask(Plugin plugin, Entity entity, Runnable runnable) {
        if (IS_FOLIA) {
            entity.getScheduler().execute(plugin, runnable, null, 0);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runTaskLater(Plugin plugin, Runnable runnable, long delayTicks) {
        long safeDelay = Math.max(1L, delayTicks);
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> runnable.run(), safeDelay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, safeDelay);
        }
    }

    public static void runTaskLater(Plugin plugin, Entity entity, Runnable runnable, long delayTicks) {
        long safeDelay = Math.max(1L, delayTicks);
        if (IS_FOLIA) {
            entity.getScheduler().runDelayed(plugin, task -> runnable.run(), () -> {}, safeDelay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, safeDelay);
        }
    }

    public static void runTask(Plugin plugin, Location location, Runnable runnable) {
        if (IS_FOLIA) {
            Bukkit.getRegionScheduler().execute(plugin, location, runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runTaskLater(Plugin plugin, Location location, Runnable runnable, long delayTicks) {
        long safeDelay = Math.max(1L, delayTicks);
        if (IS_FOLIA) {
            Bukkit.getRegionScheduler().runDelayed(plugin, location, task -> runnable.run(), safeDelay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, safeDelay);
        }
    }

    public static Task runTaskTimer(Plugin plugin, Consumer<Task> task, long delay, long period) {
        long safeDelay = Math.max(1L, delay);
        long safePeriod = Math.max(1L, period);
        if (IS_FOLIA) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, st -> task.accept(new Task() {
                @Override
                public void cancel() {
                    st.cancel();
                }

                @Override
                public boolean isCancelled() {
                    return st.isCancelled();
                }
            }), safeDelay, safePeriod);

            return new Task() {
                @Override
                public void cancel() {
                    scheduledTask.cancel();
                }

                @Override
                public boolean isCancelled() {
                    return scheduledTask.isCancelled();
                }
            };
        } else {
            final BukkitRunnable[] runnableHandle = new BukkitRunnable[1];
            runnableHandle[0] = new BukkitRunnable() {
                @Override
                public void run() {
                    task.accept(new Task() {
                        @Override
                        public void cancel() {
                            runnableHandle[0].cancel();
                        }

                        @Override
                        public boolean isCancelled() {
                            return runnableHandle[0].isCancelled();
                        }
                    });
                }
            };
            runnableHandle[0].runTaskTimer(plugin, safeDelay, safePeriod);

            return new Task() {
                @Override
                public void cancel() {
                    runnableHandle[0].cancel();
                }

                @Override
                public boolean isCancelled() {
                    return runnableHandle[0].isCancelled();
                }
            };
        }
    }

    public static void teleport(Entity entity, Location location) {
        if (IS_FOLIA) {
            entity.teleportAsync(location);
        } else {
            entity.teleport(location);
        }
    }

    public interface Task {
        void cancel();
        boolean isCancelled();
    }
}
