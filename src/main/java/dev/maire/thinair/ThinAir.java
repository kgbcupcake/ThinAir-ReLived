package dev.maire.thinair;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThinAir {
    public static final String MOD_ID = "thinair";
    public static final String MOD_NAME = "Thin Air";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
