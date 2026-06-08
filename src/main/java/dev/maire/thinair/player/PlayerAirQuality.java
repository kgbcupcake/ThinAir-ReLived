package dev.maire.thinair.player;

import dev.maire.thinair.ThinAirMod;
import dev.maire.thinair.api.AirQualityLevel;
import dev.maire.thinair.init.ModCapabilities;
import dev.maire.thinair.network.ClientboundPlayerAirQualityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlayerAirQuality {

    private PlayerAirQuality() {
    }

    public static AirQualityLevel get(Player player) {
        return player.getData(ModCapabilities.PLAYER_AIR_QUALITY.get());
    }

    public static void setIfChanged(ServerPlayer player, AirQualityLevel airQualityLevel) {
        if (get(player) == airQualityLevel) {
            return;
        }
        player.setData(ModCapabilities.PLAYER_AIR_QUALITY.get(), airQualityLevel);
        syncToClients(player, airQualityLevel);
    }

    public static void syncToClients(ServerPlayer player, AirQualityLevel airQualityLevel) {
        ClientboundPlayerAirQualityPacket packet =
                new ClientboundPlayerAirQualityPacket(player.getId(), airQualityLevel);
        ThinAirMod.sendToPlayer(player, packet);
        PacketDistributor.sendToPlayersTrackingEntity(player, packet);
    }
}
