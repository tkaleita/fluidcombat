package focuss.fluidcombat.network.client;

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

                RandomSource random = level.random;

                // play crit sound
                level.playSound(
                        null,
                        pos,
                        SoundEvents.PLAYER_ATTACK_CRIT,
                        SoundSource.PLAYERS,
                        0.4F,
                        0.9F + random.nextFloat() * 0.2F
                );

                // spawn crit particles around the block
                for (int i = 0; i < 15; i++) {
                    double px = pos.getX() + 0.5 + (random.nextDouble() - 0.5);
                    double py = pos.getY() + 0.5 + (random.nextDouble() - 0.5);
                    double pz = pos.getZ() + 0.5 + (random.nextDouble() - 0.5);

                    double vx = (random.nextDouble() - 0.5) * 0.3;
                    double vy = random.nextDouble() * 0.3;
                    double vz = (random.nextDouble() - 0.5) * 0.3;

                    level.sendParticles(
                            ParticleTypes.CRIT,
                            px, py, pz,
                            1,
                            vx, vy, vz,
                            0.0
                    );
                }
            }
        };
    }
}