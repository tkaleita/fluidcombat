package focuss.fluidcombat.platform;

import com.alcatrazescapee.notreepunching.EventHandler;
import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import focuss.fluidcombat.FluidCombat;
import focuss.fluidcombat.platform.services.IPlatformHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.randomcrits.init.RandomCritsModParticleTypes;
import net.randomcrits.init.RandomCritsModSounds;
import net.randomcrits.network.RandomCritsModVariables;
import org.jetbrains.annotations.Nullable;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public boolean canUseItemClient(Player player, ItemStack stack) {
        if (isModLoaded("justlevelingfork")) {
            AptitudeCapability cap = AptitudeCapability.get(player);
            if (cap == null) return true;
            return cap.canUseItemClient(stack);
        }
        return true;
    }

    @Override
    public boolean canUseItem(Player player, ItemStack stack) {
        if (isModLoaded("justlevelingfork")) {
            AptitudeCapability cap = AptitudeCapability.get(player);
            if (cap == null) return true;
            return cap.canUseItem(player, stack);
        }
        return true;
    }

    @Override
    public boolean canMineBlock(Player player, ItemStack stack, BlockState state, @Nullable BlockPos pos) {
        boolean drop = !state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state);
        if (isModLoaded("notreepunching")) {
            return EventHandler.modifyHarvestCheck(player, state, pos, drop);
        }
        return drop;
    }

    @Override
    public float modifyBreakSpeed(Player player, BlockState state, @Nullable BlockPos pos, float speed) {
        if (isModLoaded("notreepunching")) {
            return EventHandler.modifyBreakSpeed(player, state, pos, speed);
        }
        return speed;
    }

    @Override
    public boolean isCritModInstalled() {
        return (isModLoaded("random_crits"));
    }

    @Override
    public boolean isPlayerCritting(Player player) {
        if (isModLoaded("random_crits")) {
            FluidCombat.LOGGER.info(player.getCapability(RandomCritsModVariables.PLAYER_VARIABLES_CAPABILITY).toString());
            return player.getCapability(RandomCritsModVariables.PLAYER_VARIABLES_CAPABILITY)
                    .map(vars -> player.getRandom().nextDouble() < vars.CritChanceTest)
                    .orElse(false);
        }
        return false;
    }

    @Override
    public void playCritEffects(ServerLevel level, BlockPos pos) {
        RandomSource random = level.random;

        var particle = ParticleTypes.CRIT;
        var sound = SoundEvents.PLAYER_ATTACK_CRIT;
        var volume = 0.4f;

        if (isCritModInstalled()) {
            particle = RandomCritsModParticleTypes.RANDOM_CRITICAL_HIT.get();
            sound = RandomCritsModSounds.RANDOMCRIT.get();
            volume = 0.1f;
        }

        // play crit sound
        level.playSound(
                null,
                pos,
                sound,
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

            level.sendParticles(particle, px, py, pz, 1, vx, vy, vz, 0.0);
        }
    }

}