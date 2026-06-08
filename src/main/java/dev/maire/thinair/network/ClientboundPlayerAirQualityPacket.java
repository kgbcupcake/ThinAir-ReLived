package dev.maire.thinair.network;

import dev.maire.thinair.ThinAir;
import dev.maire.thinair.api.AirQualityLevel;
import dev.maire.thinair.client.ClientPlayerAirQualityCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundPlayerAirQualityPacket(int entityId, AirQualityLevel airQualityLevel)
        implements CustomPacketPayload {

    public static final ResourceLocation ID = ThinAir.id("player_air_quality");
    public static final CustomPacketPayload.Type<ClientboundPlayerAirQualityPacket> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, ClientboundPlayerAirQualityPacket> CODEC =
            StreamCodec.of(
                    ClientboundPlayerAirQualityPacket::encode,
                    ClientboundPlayerAirQualityPacket::decode
            );

    private static void encode(FriendlyByteBuf buf, ClientboundPlayerAirQualityPacket packet) {
        buf.writeVarInt(packet.entityId);
        buf.writeByte(packet.airQualityLevel.ordinal());
    }

    private static ClientboundPlayerAirQualityPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        AirQualityLevel level = AirQualityLevel.values()[buf.readByte()];
        return new ClientboundPlayerAirQualityPacket(entityId, level);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundPlayerAirQualityPacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                ClientPlayerAirQualityCache.put(packet.entityId, packet.airQualityLevel));
    }
}
