package net.bichal.mods.velox.physics;

import net.bichal.mods.velox.config.VeloxConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Items the server owner wants to keep on the normal game's physics ({@code excludedItems} in the config):
 * a way to deal with a mod's items without turning Velox off. An entry is an item id
 * ({@code create:chromatic_compound}) or {@code modid:*} for every item of a mod. The list is read again
 * when the config text changes; an item is checked when it joins the world.
 */
final class VeloxExclusions {

    private static String parsedFrom = "";
    private static Set<String> ids = Set.of();
    private static Set<String> namespaces = Set.of();

    private VeloxExclusions() {
    }

    static boolean excluded(ItemEntity entity) {
        String raw;
        try {
            raw = VeloxConfig.EXCLUDED_ITEMS.get();
        } catch (IllegalStateException configNotLoaded) {
            return false;
        }
        if (raw.isBlank()) {
            return false;
        }
        if (!raw.equals(parsedFrom)) {
            parse(raw);
        }
        String id = BuiltInRegistries.ITEM.getKey(entity.getItem().getItem()).toString();
        if (ids.contains(id)) {
            return true;
        }
        int colon = id.indexOf(':');
        return colon > 0 && namespaces.contains(id.substring(0, colon));
    }

    private static void parse(String raw) {
        Set<String> exact = new HashSet<>();
        Set<String> mods = new HashSet<>();
        for (String part : raw.split(",")) {
            String entry = part.trim().toLowerCase(java.util.Locale.ROOT);
            if (entry.isEmpty()) {
                continue;
            }
            if (entry.endsWith(":*")) {
                mods.add(entry.substring(0, entry.length() - 2));
            } else {
                exact.add(entry.indexOf(':') < 0 ? "minecraft:" + entry : entry);
            }
        }
        ids = exact;
        namespaces = mods;
        parsedFrom = raw;
    }
}
