package net.bichal.mods.velox.physics;

import com.mojang.brigadier.CommandDispatcher;
import net.bichal.mods.celeris.physics.CelerisPhysics;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Locale;

/**
 * {@code /velox status}: whether Velox is working and how many items it handles in each dimension. For
 * server owners, and for bug reports. It only reads Velox's own counters.
 */
final class VeloxCommands {

    private VeloxCommands() {
    }

    static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("velox")
                .requires(VeloxCommands::allowed)
                .then(Commands.literal("status").executes(context -> status(context.getSource()))));
    }

    /**
     * Operators (permission level 2). Minecraft reworked permissions in 1.21.11, so the method is looked up by
     * name: where it is gone, only the console (a source that is not an entity) may use the command.
     */
    private static boolean allowed(CommandSourceStack source) {
        try {
            return (Boolean) CommandSourceStack.class.getMethod("hasPermission", int.class).invoke(source, 2);
        } catch (NoSuchMethodException newerPermissions) {
            return source.getEntity() == null;
        } catch (ReflectiveOperationException | RuntimeException error) {
            return false;
        }
    }

    private static int status(CommandSourceStack source) {
        reply(source, "Velox: " + headline());
        int shown = 0;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            ItemPhysicsWorld world = ((VeloxLevel) level).velox$physics();
            if (world == null) {
                continue;
            }
            shown++;
            reply(source, String.format(Locale.ROOT, "  %s: %d of %d items handled, %d took the batch result last tick, "
                            + "%d left to the normal game by the engine, step %.2f ms",
                    McCompat.dimensionId(level), world.size(), world.capacity(), world.appliedCount(),
                    world.lastDeferred(), world.lastStepMillis()));
        }
        if (shown == 0 && !VeloxFailsafe.tripped()) {
            reply(source, "  no dimension has dropped items to handle yet");
        }
        return 1;
    }

    private static String headline() {
        if (VeloxFailsafe.tripped()) {
            return "SWITCHED OFF after an error (" + VeloxFailsafe.lastFailure() + "). Items use the normal game's "
                    + "physics until the server restarts. See the log lines that start with Velox.";
        }
        if (!VeloxEvents.engineAvailable()) {
            return "not running: it is turned off in the config, or the Celeris engine is unavailable (see the log).";
        }
        return "running on the Celeris " + CelerisPhysics.engineName() + " kernel.";
    }

    private static void reply(CommandSourceStack source, String line) {
        source.sendSuccess(() -> Component.literal(line), false);
    }
}
