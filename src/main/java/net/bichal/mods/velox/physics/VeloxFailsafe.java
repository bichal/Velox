package net.bichal.mods.velox.physics;

import net.bichal.mods.velox.Velox;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * What happens when Velox itself fails, most likely because another mod changes the same vanilla code.
 * Every Velox hook that can throw reports here instead of letting the error reach the server tick: Velox
 * switches itself off for the rest of the session, hands every item back to the normal game, and writes the
 * reason to the log once. The server keeps running. A restart tries again.
 */
public final class VeloxFailsafe {

    private static final int MAX_SUSPECTS = 5;

    private static volatile boolean tripped;
    private static volatile String lastFailure = "";

    private VeloxFailsafe() {
    }

    /** Velox switched itself off after an error. */
    public static boolean tripped() {
        return tripped;
    }

    /** One line about the error that switched Velox off, or "" when it did not happen. */
    public static String lastFailure() {
        return lastFailure;
    }

    /** Runs a hook; an error in it switches Velox off instead of reaching the caller. */
    public static void run(String where, Runnable action) {
        if (tripped) {
            return;
        }
        try {
            action.run();
        } catch (RuntimeException | LinkageError error) {
            trip(where, error);
        }
    }

    /**
     * Switches Velox off. Only the first call does anything: after it, every later error is already
     * explained by the same log lines.
     */
    public static synchronized void trip(String where, Throwable error) {
        if (tripped) {
            return;
        }
        tripped = true;
        lastFailure = where + ": " + error;
        Velox.LOGGER.error("Velox: switched OFF after an error in {}. Items use the normal game's physics until the"
                + " server restarts. The server keeps running.", where);
        Set<String> suspects = suspects(error);
        if (suspects.isEmpty()) {
            Velox.LOGGER.error("Velox: no other mod shows up in the error, so this may be a bug in Velox or in the Minecraft version.");
        } else {
            Velox.LOGGER.error("Velox: code from these packages is part of the error, so one of them may clash with Velox: {}",
                    String.join(", ", suspects));
            Velox.LOGGER.error("Velox: if that mod's dropped items are the cause, list them in excludedItems in Velox's"
                    + " config; to keep Velox off, set enabled = false.");
        }
        Velox.LOGGER.error("Velox: the error was:", error);
        Velox.LOGGER.error("Velox: please report it, with this log, at https://github.com/bichal/Velox/issues");
        shutDownPhysics();
    }

    /** Closes the physics of every level: each item goes back to the normal game. */
    private static void shutDownPhysics() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            VeloxLevel holder = (VeloxLevel) level;
            ItemPhysicsWorld world = holder.velox$physics();
            if (world == null) {
                continue;
            }
            try {
                world.close();
            } catch (RuntimeException | LinkageError closeError) {
                Velox.LOGGER.debug("Velox: closing the physics of {} failed as well", McCompat.dimensionId(level), closeError);
            }
            holder.velox$setPhysics(null);
        }
    }

    /** Package roots of the code, other than the game and Velox, that appears in the error. */
    private static Set<String> suspects(Throwable error) {
        Set<String> found = new LinkedHashSet<>();
        Throwable current = error;
        for (int depth = 0; current != null && depth < 8 && found.size() < MAX_SUSPECTS; depth++) {
            for (StackTraceElement frame : current.getStackTrace()) {
                String className = frame.getClassName();
                if (!isInfrastructure(className)) {
                    found.add(packageRoot(className));
                    if (found.size() >= MAX_SUSPECTS) {
                        break;
                    }
                }
            }
            current = current.getCause();
        }
        return found;
    }

    private static boolean isInfrastructure(String className) {
        return className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("jdk.")
                || className.startsWith("sun.") || className.startsWith("net.minecraft.")
                || className.startsWith("com.mojang.") || className.startsWith("net.neoforged.")
                || className.startsWith("cpw.mods.") || className.startsWith("org.spongepowered.")
                || className.startsWith("com.llamalad7.") || className.startsWith("it.unimi.")
                || className.startsWith("org.slf4j.") || className.startsWith("org.apache.")
                || className.startsWith("com.google.") || className.startsWith("io.netty.")
                || className.startsWith("net.bichal.mods.velox.") || className.startsWith("net.bichal.mods.celeris.");
    }

    private static String packageRoot(String className) {
        String[] parts = className.split("\\.");
        int keep = Math.min(parts.length - 1, 3);
        if (keep < 1) {
            return className;
        }
        return String.join(".", java.util.Arrays.copyOf(parts, keep));
    }
}
