package focuss.fluidcombat.network.client;

import focuss.fluidcombat.FluidCombat;
import focuss.fluidcombat.platform.Services;
import fuzs.puzzleslib.api.network.v3.ServerMessageListener;
import fuzs.puzzleslib.api.network.v3.ServerboundMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;

public record ServerboundBlockCritEffectsMessage(int x, int y, int z) implements ServerboundMessage<ServerboundBlockCritEffectsMessage> {
    @Override
    public ServerMessageListener<ServerboundBlockCritEffectsMessage> getHandler() {
        return new ServerMessageListener<>() {
            @Override
            public void handle(ServerboundBlockCritEffectsMessage message,
                               MinecraftServer server,
                               ServerGamePacketListenerImpl handler,
                               ServerPlayer player,
                               ServerLevel level) {

                if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) return;

                BlockPos pos = new BlockPos(message.x(), message.y(), message.z());
                if (!level.isLoaded(pos)) return;

                Services.PLATFORM.playCritEffects(level, pos);
            }
        };
    }
}