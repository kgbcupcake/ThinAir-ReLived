package dev.maire.thinair.network;

import dev.maire.thinair.ThinAir;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ThinAirNetwork {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ThinAir.MOD_ID).versioned("1");

        registrar.playToClient(
                ClientboundChunkAirQualityPacket.TYPE,
                ClientboundChunkAirQualityPacket.CODEC,
                ClientboundChunkAirQualityPacket::handle
        );

        registrar.playToClient(
                ClientboundPlayerAirQualityPacket.TYPE,
                ClientboundPlayerAirQualityPacket.CODEC,
                ClientboundPlayerAirQualityPacket::handle
        );
    }
}
