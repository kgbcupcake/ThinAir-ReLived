package dev.maire.thinair.client.config;

import dev.maire.thinair.config.ThinAirConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ThinAirConfigScreen {

    private ThinAirConfigScreen() {}

    public static Screen create(Screen parent) {
        ThinAirConfig config = ThinAirConfig.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.thinair.title"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(
                Component.translatable("config.thinair.category.general"));

        general.addEntry(eb.startBooleanToggle(
                        Component.translatable("config.thinair.enableSignalTorches"),
                        config.enableSignalTorches())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.thinair.enableSignalTorches.desc"))
                .setSaveConsumer(config::setEnableSignalTorches)
                .build());

        general.addEntry(eb.startIntSlider(
                        Component.translatable("config.thinair.drownedChoking"),
                        config.drownedChoking(), 0, 72000)
                .setDefaultValue(100)
                .setTooltip(Component.translatable("config.thinair.drownedChoking.desc"))
                .setSaveConsumer(config::setDrownedChoking)
                .build());

        ConfigCategory ranges = builder.getOrCreateCategory(
                Component.translatable("config.thinair.category.ranges"));

        ranges.addEntry(eb.startDoubleField(
                        Component.translatable("config.thinair.yellowAirProviderRadius"),
                        config.yellowAirProviderRadius())
                .setDefaultValue(6.0)
                .setMin(1.0).setMax(32.0)
                .setTooltip(Component.translatable("config.thinair.yellowAirProviderRadius.desc"))
                .setSaveConsumer(config::setYellowAirProviderRadius)
                .build());

        ranges.addEntry(eb.startDoubleField(
                        Component.translatable("config.thinair.blueAirProviderRadius"),
                        config.blueAirProviderRadius())
                .setDefaultValue(6.0)
                .setMin(1.0).setMax(32.0)
                .setTooltip(Component.translatable("config.thinair.blueAirProviderRadius.desc"))
                .setSaveConsumer(config::setBlueAirProviderRadius)
                .build());

        ranges.addEntry(eb.startDoubleField(
                        Component.translatable("config.thinair.redAirProviderRadius"),
                        config.redAirProviderRadius())
                .setDefaultValue(3.0)
                .setMin(1.0).setMax(32.0)
                .setTooltip(Component.translatable("config.thinair.redAirProviderRadius.desc"))
                .setSaveConsumer(config::setRedAirProviderRadius)
                .build());

        ranges.addEntry(eb.startDoubleField(
                        Component.translatable("config.thinair.greenAirProviderRadius"),
                        config.greenAirProviderRadius())
                .setDefaultValue(9.0)
                .setMin(1.0).setMax(32.0)
                .setTooltip(Component.translatable("config.thinair.greenAirProviderRadius.desc"))
                .setSaveConsumer(config::setGreenAirProviderRadius)
                .build());

        builder.setSavingRunnable(ThinAirConfig::saveNow);
        builder.setAlwaysShowTabs(true);
        return builder.build();
    }
}
