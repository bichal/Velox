package net.bichal.mods.velox;

import net.bichal.mods.velox.config.VeloxConfig;
import net.bichal.mods.velox.parity.ParityHarness;
import net.bichal.mods.velox.physics.VeloxEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Velox: hands item entity physics to Celeris's batch engine. Velox itself
 * does no simulation; it only connects vanilla entities and terrain to
 * {@code net.bichal.mods.celeris.physics} and applies the results inside the
 * vanilla tick, so every other part of {@code ItemEntity.tick} (pickup delay,
 * merging, age, despawn) keeps running unchanged.
 */
@Mod(Velox.MOD_ID)
public final class Velox {

    public static final String MOD_ID = "velox";
    public static final Logger LOGGER = LoggerFactory.getLogger("Velox");
    /**
     * Opt-in ({@code -Dvelox.nativeKernel=true}) for Celeris's native kernel. Off by default: in the CI
     * benchmarks the Java, JNI and FFM kernels give the same server tick, so loading native code buys
     * nothing measurable. It is a system property because the config file is not loaded yet when this
     * constructor runs, and the engine is chosen before it is.
     */
    public static final String NATIVE_KERNEL_PROPERTY = "velox.nativeKernel";

    public Velox(IEventBus modBus, ModContainer container) {
        container.registerConfig(commonConfigType(), VeloxConfig.SPEC);
        if (Boolean.getBoolean(NATIVE_KERNEL_PROPERTY)) {
            requestNativeKernel();
        }
        VeloxEvents.register();
        if (ParityHarness.ENABLED) {
            ParityHarness.register();
        }
    }

    /**
     * Celeris runs its Java kernel unless a mod asks for the native one. Velox asks only when
     * {@link #NATIVE_KERNEL_PROPERTY} is set. This has to run before Celeris chooses its engine
     * (the first batch), so it is the first thing the mod does.
     * The player keeps the last word with {@code -Dceleris.physics.engine=java}. Looked up by name
     * so Velox compiles and runs against any Celeris: one from before the opt-in has no such method
     * and already prefers the native kernel.
     */
    private static void requestNativeKernel() {
        try {
            Class.forName("net.bichal.mods.celeris.physics.CelerisPhysics")
                    .getMethod("allowNative", String.class).invoke(null, MOD_ID);
        } catch (ReflectiveOperationException | LinkageError olderCeleris) {
            LOGGER.debug("Celeris has no allowNative: its engine choice is not opt-in");
        }
    }

    /**
     * COMMON (loaded on each side, not synced); from NeoForge 26.3 the types are
     * CLIENT, SYNCED and LOCAL, where LOCAL is what COMMON was. Looked up by name
     * so one jar runs on both.
     */
    private static ModConfig.Type commonConfigType() {
        for (ModConfig.Type type : ModConfig.Type.values()) {
            if (type.name().equals("COMMON")) {
                return type;
            }
        }
        return ModConfig.Type.valueOf("LOCAL");
    }
}
