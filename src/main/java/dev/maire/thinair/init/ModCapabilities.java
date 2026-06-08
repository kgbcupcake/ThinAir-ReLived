package dev.maire.thinair.init;

import dev.maire.thinair.ThinAir;
import dev.maire.thinair.capability.AirBubblePositionsCapability;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class ModCapabilities {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ThinAir.MOD_ID);

    public static final Supplier<AttachmentType<AirBubblePositionsCapability>> AIR_BUBBLE_POSITIONS =
            ATTACHMENT_TYPES.register("air_bubble_positions", () ->
                    AttachmentType.serializable(AirBubblePositionsCapability::new).build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    @Nullable
    public static AirBubblePositionsCapability get(LevelChunk chunk) {
        return chunk.getData(AIR_BUBBLE_POSITIONS.get());
    }
}
