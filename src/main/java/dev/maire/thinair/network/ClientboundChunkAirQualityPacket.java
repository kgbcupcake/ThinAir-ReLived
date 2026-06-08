package dev.maire.thinair.network;

import dev.maire.thinair.ThinAir;
import dev.maire.thinair.api.AirQualityLevel;
import dev.maire.thinair.capability.AirBubblePositionsCapability;
import dev.maire.thinair.init.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record ClientboundChunkAirQualityPacket(
        ChunkPos chunkPos,
        Map<BlockPos, AirQualityLevel> airBubblePositions,
        Mode mode
) implements CustomPacketPayload {

    public static final ResourceLocation ID = ThinAir.id("chunk_air_quality");
    public static final CustomPacketPayload.Type<ClientboundChunkAirQualityPacket> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, ClientboundChunkAirQualityPacket> CODEC =
            StreamCodec.of(
                    ClientboundChunkAirQualityPacket::encode,
                    ClientboundChunkAirQualityPacket::decode
            );

    private static void encode(FriendlyByteBuf buf, ClientboundChunkAirQualityPacket packet) {
        buf.writeLong(packet.chunkPos.toLong());
        buf.writeByte(packet.mode.ordinal());
        buf.writeInt(packet.airBubblePositions.size());
        for (Map.Entry<BlockPos, AirQualityLevel> entry : packet.airBubblePositions.entrySet()) {
            buf.writeBlockPos(entry.getKey());
            buf.writeByte(entry.getValue().ordinal());
        }
    }

    private static ClientboundChunkAirQualityPacket decode(FriendlyByteBuf buf) {
        ChunkPos chunkPos = new ChunkPos(buf.readLong());
        Mode mode = Mode.values()[buf.readByte()];
        int size = buf.readInt();
        Map<BlockPos, AirQualityLevel> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(buf.readBlockPos(), AirQualityLevel.values()[buf.readByte()]);
        }
        return new ClientboundChunkAirQualityPacket(chunkPos, map, mode);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundChunkAirQualityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if (!level.getChunkSource().hasChunk(packet.chunkPos.x, packet.chunkPos.z)) {
                return;
            }
            LevelChunk chunk = level.getChunkSource().getChunkNow(packet.chunkPos.x, packet.chunkPos.z);
            if (chunk == null) {
                return;
            }

            AirBubblePositionsCapability capability = ModCapabilities.get(chunk);
            if (capability == null) {
                return;
            }

            Map<BlockPos, AirQualityLevel> airBubblePositions = capability.getAirBubblePositions();
            switch (packet.mode) {
                case REPLACE -> {
                    airBubblePositions.clear();
                    airBubblePositions.putAll(packet.airBubblePositions);
                }
                case ADD -> airBubblePositions.putAll(packet.airBubblePositions);
                case REMOVE -> packet.airBubblePositions.keySet().forEach(airBubblePositions::remove);
            }
        });
    }

    public enum Mode {
        REPLACE,
        REMOVE,
        ADD
    }
}
