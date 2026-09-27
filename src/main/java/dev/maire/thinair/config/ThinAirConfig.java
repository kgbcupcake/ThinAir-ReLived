package dev.maire.thinair.config;

import dev.maire.thinair.ThinAir;
import dev.maire.thinair.api.AirQualityLevel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ThinAirConfig {
    public static final ModConfigSpec SPEC;
    private static ThinAirConfig INSTANCE;
    private static ModConfig boundConfig;
    private static Map<ResourceLocation, AirQualityEntry> dimensionEntries = null;
    private static Map<ResourceLocation, AirQualityEntry> biomeEntries = null;

    private ModConfigSpec.ConfigValue<List<? extends String>> dimensions;
    private ModConfigSpec.ConfigValue<List<? extends String>> biomes;
    private ModConfigSpec.BooleanValue enableSignalTorches;
    private ModConfigSpec.BooleanValue affectAllMobs;
    private ModConfigSpec.IntValue drownedChoking;
    private ModConfigSpec.DoubleValue blueAirProviderRadius;
    private ModConfigSpec.DoubleValue redAirProviderRadius;
    private ModConfigSpec.DoubleValue yellowAirProviderRadius;
    private ModConfigSpec.DoubleValue orangeAirProviderRadius;
    private ModConfigSpec.DoubleValue greenAirProviderRadius;

    static {
        Pair<ThinAirConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ThinAirConfig::new);
        SPEC = specPair.getRight();
    }

    public ThinAirConfig(ModConfigSpec.Builder builder) {
        INSTANCE = this;

        dimensions = builder.comment("Air qualities at different heights in different dimensions.",
                        "The syntax is the dimension's resource location, the \"default\" air level in that dimension,",
                        "then any number of height:airlevel pairs separated by commas.",
                        "The air will have that quality starting at that height and above (until the next entry).",
                        "The entries must be in ascending order of height.",
                        "If a dimension doesn't have an entry here, it'll be assumed to be green everywhere."
                )
                .defineList("dimensions",
                        Arrays.asList("minecraft:overworld=yellow,0:green,128:yellow",
                                "minecraft:the_nether=yellow",
                                "minecraft:the_end=red"
                        ),
                        o -> o instanceof String s && parseAirQualityLine(s, "dimension") != null
                );

        biomes = builder.comment("Air qualities at different heights in specific biomes.",
                        "Use the biome's resource location, the default air level, and optional height:airlevel pairs.",
                        "Entries use the same syntax as dimensions. A biome entry overrides the dimension profile."
                )
                .defineList("biomes", List.<String>of(),
                        o -> o instanceof String s && parseAirQualityLine(s, "biome") != null);

        enableSignalTorches = builder.comment(
                        "Whether to allow right-clicking torches to make them spray particle effects")
                .define("enableSignalTorches", true);

        affectAllMobs = builder.comment(
                        "Whether all non-player living entities are affected by air quality, in addition to entities in the air_quality_sensitive entity type tag.")
                .define("affectAllMobs", false);

        drownedChoking = builder.comment("How much air a Drowned attack removes. Set to 0 to disable this feature.")
                .defineInRange("drownedChoking", 100, 0, 72000);

        builder.push("Ranges");
        yellowAirProviderRadius = builder.comment(
                        "The radius in which all blocks defined in the yellow air providers tag project a bubble of air around them.")
                .defineInRange("yellowAirProviderRadius", 6.0, 1.0, 32.0);
        orangeAirProviderRadius = builder.comment(
                        "The radius in which all blocks defined in the orange air providers tag project a bubble of air around them.")
                .defineInRange("orangeAirProviderRadius", 6.0, 1.0, 32.0);
        blueAirProviderRadius = builder.comment(
                        "The radius in which all blocks defined in the blue air providers tag (usually soul fire related blocks) project a bubble of air around them.")
                .defineInRange("blueAirProviderRadius", 6.0, 1.0, 32.0);
        redAirProviderRadius = builder.comment(
                        "The radius in which all blocks defined in the red air providers tag (usually lava related blocks) project a bubble of air around them.")
                .defineInRange("redAirProviderRadius", 3.0, 1.0, 32.0);
        greenAirProviderRadius = builder.comment(
                        "The radius in which all blocks defined in the green air providers tag (usually various portal blocks) project a bubble of air around them.")
                .defineInRange("greenAirProviderRadius", 9.0, 1.0, 32.0);
        builder.pop();
    }

    public static ThinAirConfig get() {
        return INSTANCE;
    }

    public static void bind(ModConfig config) {
        if (config.getSpec() == SPEC) {
            boundConfig = config;
        }
    }

    public static void saveNow() {
        if (boundConfig != null) {
            var loadedConfig = boundConfig.getLoadedConfig();
            if (loadedConfig != null) {
                loadedConfig.save();
            }
        }
    }

    public boolean enableSignalTorches() {
        return enableSignalTorches.get();
    }

    public void setEnableSignalTorches(boolean value) {
        enableSignalTorches.set(value);
    }

    public boolean affectAllMobs() {
        return affectAllMobs.get();
    }

    public void setAffectAllMobs(boolean value) {
        affectAllMobs.set(value);
    }

    public int drownedChoking() {
        return drownedChoking.get();
    }

    public void setDrownedChoking(int value) {
        drownedChoking.set(value);
    }

    public double yellowAirProviderRadius() {
        return yellowAirProviderRadius.get();
    }

    public void setYellowAirProviderRadius(double value) {
        yellowAirProviderRadius.set(value);
    }

    public double orangeAirProviderRadius() {
        return orangeAirProviderRadius.get();
    }

    public void setOrangeAirProviderRadius(double value) {
        orangeAirProviderRadius.set(value);
    }

    public double blueAirProviderRadius() {
        return blueAirProviderRadius.get();
    }

    public void setBlueAirProviderRadius(double value) {
        blueAirProviderRadius.set(value);
    }

    public double redAirProviderRadius() {
        return redAirProviderRadius.get();
    }

    public void setRedAirProviderRadius(double value) {
        redAirProviderRadius.set(value);
    }

    public double greenAirProviderRadius() {
        return greenAirProviderRadius.get();
    }

    public void setGreenAirProviderRadius(double value) {
        greenAirProviderRadius.set(value);
    }

    public static void invalidateCache() {
        dimensionEntries = null;
        biomeEntries = null;
    }

    @Nullable
    private static Pair<ResourceLocation, AirQualityEntry> parseAirQualityLine(String line, String profileType) {
        var profileValues = line.split("=");
        if (profileValues.length != 2) {
            ThinAir.LOGGER.warn("Couldn't parse {} air quality entry {}: couldn't split across `=` into 2 parts",
                    profileType, line);
            return null;
        }

        var profileKey = ResourceLocation.tryParse(profileValues[0]);
        if (profileKey == null) {
            ThinAir.LOGGER.warn("Couldn't parse {} air quality entry {}: {} isn't a valid resource location",
                    profileType, line, profileValues[0]
            );
            return null;
        }

        var heightAndRest = profileValues[1].split(",", 2);
        AirQualityLevel baseQuality;
        try {
            baseQuality = AirQualityLevel.valueOf(heightAndRest[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            ThinAir.LOGGER.warn("Couldn't parse {} air quality entry {}: {} isn't a valid base air quality",
                    profileType, line, heightAndRest[0]
            );
            return null;
        }

        var heights = new ArrayList<Pair<Integer, AirQualityLevel>>();
        if (heightAndRest.length == 2) {
            var heightPairStrs = heightAndRest[1].split(",");
            Integer prevHeight = null;
            for (var heightPairStr : heightPairStrs) {
                var pairStr = heightPairStr.split(":");
                if (pairStr.length != 2) {
                    ThinAir.LOGGER.warn("Couldn't parse {} air quality entry {}: couldn't use {} as a height entry",
                            profileType, line, heightPairStr
                    );
                    return null;
                }

                int height;
                try {
                    height = Integer.parseInt(pairStr[0]);
                } catch (NumberFormatException e) {
                    ThinAir.LOGGER.warn("Couldn't parse {} air quality entry {}: {} isn't a valid height",
                            profileType, line, pairStr[0]);
                    return null;
                }
                if (prevHeight != null && height <= prevHeight) {
                    ThinAir.LOGGER.warn("Couldn't parse {} air quality entry {}: height entries must be in ascending order",
                            profileType, line);
                    return null;
                }
                prevHeight = height;

                AirQualityLevel quality;
                try {
                    quality = AirQualityLevel.valueOf(pairStr[1].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    ThinAir.LOGGER.warn("Couldn't parse {} air quality entry {}: {} isn't a valid air quality",
                            profileType, line, pairStr[1]
                    );
                    return null;
                }

                heights.add(Pair.of(height, quality));
            }
        }

        return Pair.of(profileKey, new AirQualityEntry(baseQuality, heights));
    }

    public static AirQualityLevel getAirQualityAtLevelByDimension(ResourceLocation dimension, int y) {
        if (dimensionEntries == null) {
            var lines = INSTANCE.dimensions.get();
            dimensionEntries = new HashMap<>(lines.size());
            for (var line : INSTANCE.dimensions.get()) {
                var entry = parseAirQualityLine(line, "dimension");
                if (entry == null) {
                    ThinAir.LOGGER.warn("Somehow managed to get a bad dimension config past the validator?!");
                    continue;
                }
                dimensionEntries.put(entry.getLeft(), entry.getRight());
            }
        }

        return getAirQualityAtLevel(dimensionEntries.get(dimension), y, AirQualityLevel.GREEN);
    }

    @Nullable
    public static AirQualityLevel getAirQualityAtLevelByBiome(ResourceLocation biome, int y) {
        if (biomeEntries == null) {
            var lines = INSTANCE.biomes.get();
            biomeEntries = new HashMap<>(lines.size());
            for (var line : lines) {
                var entry = parseAirQualityLine(line, "biome");
                if (entry == null) {
                    ThinAir.LOGGER.warn("Somehow managed to get a bad biome config past the validator?!");
                    continue;
                }
                biomeEntries.put(entry.getLeft(), entry.getRight());
            }
        }
        return biomeEntries.containsKey(biome)
                ? getAirQualityAtLevel(biomeEntries.get(biome), y, null)
                : null;
    }

    @Nullable
    private static AirQualityLevel getAirQualityAtLevel(
            @Nullable AirQualityEntry entry, int y, @Nullable AirQualityLevel fallback) {
        if (entry == null) {
            return fallback;
        }
        List<Pair<Integer, AirQualityLevel>> heights = entry.heights;
        for (int i = heights.size() - 1; i >= 0; i--) {
            Pair<Integer, AirQualityLevel> heightPair = heights.get(i);
            if (y >= heightPair.getLeft()) {
                return heightPair.getRight();
            }
        }
        return entry.baseQuality;
    }

    private record AirQualityEntry(AirQualityLevel baseQuality, List<Pair<Integer, AirQualityLevel>> heights) {

    }
}
