package dev.viruzia.combatreanimated;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CombatReanimated {
    public static final String MOD_ID = "combatreanimated";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private CombatReanimated() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
