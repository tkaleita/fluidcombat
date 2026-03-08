package focuss.fluidcombat.network.client;

import focuss.fluidcombat.FluidCombat;
import focuss.fluidcombat.platform.Services;
import fuzs.puzzleslib.api.network.v3.ServerMessageListener;
import fuzs.puzzleslib.api.network.v3.ServerboundMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public record ServerboundBreakBlockMessage(EquipmentSlot slot, int x, int y, int z)
        implements ServerboundMessage<ServerboundBreakBlockMessage> {

    @Override
    public ServerMessageListener<ServerboundBreakBlockMessage> getHandler() {
        return new ServerMessageListener<>() {
            @Override
            public void handle(ServerboundBreakBlockMessage message,
                               MinecraftServer server,
                               ServerGamePacketListenerImpl handler,
                               ServerPlayer player,
                               ServerLevel level) {

                if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) return;

                BlockPos pos = new BlockPos(message.x(), message.y(), message.z());
                if (!level.isLoaded(pos)) return;

                BlockState state = level.getBlockState(pos);
                BlockEntity blockEntity = level.getBlockEntity(pos);

                if (!player.isCreative() && state.getDestroySpeed(level, pos) < 0) return;

                // block breaking effects
                level.levelEvent(2001, pos, Block.getId(state));

                ItemStack stack = player.getItemBySlot(message.slot());
                var drop = Services.PLATFORM.canMineBlock(player, stack, state, pos);
                FluidCombat.LOGGER.info("canMineBlock: {}", drop);
                if (drop) {
                    state.getBlock().playerDestroy(level, player, pos, state, blockEntity, stack);
                }
                level.removeBlock(pos, false);

                // give tools damage!
                stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(message.slot()));
            }
        };
    }
}