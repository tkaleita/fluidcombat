package focuss.fluidcombat.platform.services;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {

        return isDevelopmentEnvironment() ? "development" : "production";
    }

    boolean canUseItemClient(Player player, ItemStack stack);

    boolean canUseItem(Player player, ItemStack stack);

    boolean canMineBlock(Player player, ItemStack stack, BlockState state, @Nullable BlockPos pos);

    float modifyBreakSpeed(Player player, BlockState state, @Nullable BlockPos pos, float speed);

    boolean isPlayerCritting(Player player);

    void playCritEffects(ServerLevel level, BlockPos pos);

    boolean isCritModInstalled();

}