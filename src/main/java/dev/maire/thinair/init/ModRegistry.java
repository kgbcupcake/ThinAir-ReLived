package dev.maire.thinair.init;

import dev.maire.thinair.ThinAir;
import dev.maire.thinair.advancements.criterion.BreatheAirTrigger;
import dev.maire.thinair.advancements.criterion.SignalifyTorchTrigger;
import dev.maire.thinair.advancements.criterion.UsedSoulfireTrigger;
import dev.maire.thinair.world.item.AirBladderItem;
import dev.maire.thinair.world.item.SoulfireBottleItem;
import dev.maire.thinair.world.level.block.SafetyLanternBlock;
import dev.maire.thinair.world.level.block.SignalTorchBlock;
import dev.maire.thinair.world.level.block.WallSignalTorchBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.advancements.CriterionTrigger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRegistry {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThinAir.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ThinAir.MOD_ID);
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, ThinAir.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThinAir.MOD_ID);

    public static final DeferredBlock<SignalTorchBlock> SIGNAL_TORCH_BLOCK =
            BLOCKS.register("signal_torch", () -> new SignalTorchBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TORCH)));
    public static final DeferredBlock<WallSignalTorchBlock> WALL_SIGNAL_TORCH_BLOCK =
            BLOCKS.register("wall_signal_torch", () -> new WallSignalTorchBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WALL_TORCH)
                            .dropsLike(SIGNAL_TORCH_BLOCK.get())
            ));
    public static final DeferredBlock<SafetyLanternBlock> SAFETY_LANTERN_BLOCK =
            BLOCKS.register("safety_lantern", () -> new SafetyLanternBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
                            .lightLevel(state -> state.getValue(SafetyLanternBlock.AIR_QUALITY).getLightLevel())
            ));

    public static final DeferredItem<AirBladderItem> AIR_BLADDER_ITEM =
            ITEMS.register("air_bladder", () -> new AirBladderItem(new Item.Properties().durability(327)));
    public static final DeferredItem<AirBladderItem> REINFORCED_AIR_BLADDER_ITEM =
            ITEMS.register("reinforced_air_bladder", () -> new AirBladderItem(new Item.Properties().durability(1962)));
    public static final DeferredItem<SoulfireBottleItem> SOULFIRE_BOTTLE_ITEM =
            ITEMS.register("soulfire_bottle", () -> new SoulfireBottleItem(new Item.Properties()));
    public static final DeferredItem<BlockItem> SAFETY_LANTERN_ITEM =
            ITEMS.register("safety_lantern", () -> new BlockItem(SAFETY_LANTERN_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<Item> RESPIRATOR_ITEM =
            ITEMS.register("respirator", () -> new Item(new Item.Properties().durability(77)));

    public static final DeferredHolder<CriterionTrigger<?>, BreatheAirTrigger> BREATHE_AIR_TRIGGER =
            TRIGGERS.register("breathe_air", BreatheAirTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, SignalifyTorchTrigger> SIGNALIFY_TORCH_TRIGGER =
            TRIGGERS.register("signalify_torch", SignalifyTorchTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, UsedSoulfireTrigger> USED_SOULFIRE_TRIGGER =
            TRIGGERS.register("used_soulfire", UsedSoulfireTrigger::new);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> THIN_AIR_TAB =
            CREATIVE_MODE_TABS.register("thinair", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.thinair.main"))
                    .icon(() -> new ItemStack(AIR_BLADDER_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(RESPIRATOR_ITEM.get());
                        output.accept(AIR_BLADDER_ITEM.get());
                        output.accept(REINFORCED_AIR_BLADDER_ITEM.get());
                        output.accept(SOULFIRE_BOTTLE_ITEM.get());
                        output.accept(SAFETY_LANTERN_ITEM.get());
                    })
                    .build());

    public static final TagKey<Item> AIR_REFILLER_ITEM_TAG =
            TagKey.create(Registries.ITEM, ThinAir.id("air_refiller"));
    public static final TagKey<EntityType<?>> AIR_QUALITY_SENSITIVE_ENTITY_TYPE_TAG =
            TagKey.create(Registries.ENTITY_TYPE, ThinAir.id("air_quality_sensitive"));

    public static final ResourceLocation SOULFIRE_BOTTLE_BURIED_LOOT_TABLE = ThinAir.id("chest/inject/soulfire_bottle_buried");
    public static final ResourceLocation SOULFIRE_BOTTLE_SHIPWRECK_LOOT_TABLE = ThinAir.id("chest/inject/soulfire_bottle_shipwreck");
    public static final ResourceLocation SOULFIRE_BOTTLE_BIG_RUIN_LOOT_TABLE = ThinAir.id("chest/inject/soulfire_bottle_big_ruin");
    public static final ResourceLocation SOULFIRE_BOTTLE_SMALL_RUIN_LOOT_TABLE = ThinAir.id("chest/inject/soulfire_bottle_small_ruin");
    public static final ResourceLocation SAFETY_LANTERN_DUNGEON_LOOT_TABLE = ThinAir.id("chest/inject/safety_lantern_dungeon");
    public static final ResourceLocation SAFETY_LANTERN_MINESHAFT_LOOT_TABLE = ThinAir.id("chest/inject/safety_lantern_mineshaft");
    public static final ResourceLocation SAFETY_LANTERN_STRONGHOLD_LOOT_TABLE = ThinAir.id("chest/inject/safety_lantern_stronghold");

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        TRIGGERS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
