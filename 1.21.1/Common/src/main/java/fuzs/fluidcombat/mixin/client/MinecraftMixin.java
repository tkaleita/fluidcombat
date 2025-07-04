package fuzs.fluidcombat.mixin.client;

import fuzs.fluidcombat.FluidCombat;
import fuzs.fluidcombat.client.handler.AutoAttackHandler;
import fuzs.fluidcombat.config.ServerConfig;
import fuzs.fluidcombat.helper.SweepAttackHelper;
import fuzs.fluidcombat.mixin.client.accessor.MultiPlayerGameModeAccessor;
import fuzs.fluidcombat.network.client.ServerboundSweepAttackMessage;
import fuzs.fluidcombat.network.client.ServerboundSwingArmMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Shadow
    public ClientLevel level;
    @Shadow
    public LocalPlayer player;
    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;

    @Inject(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;stopDestroyBlock()V", shift = At.Shift.AFTER))
    private void continueAttack(boolean attacking, CallbackInfo callback) {
        if (!FluidCombat.CONFIG.get(ServerConfig.class).holdAttackButton) return;
        // do not cancel stopDestroyBlock as in combat snapshots
        // also additional check for an item being used
        if (attacking && !this.player.isUsingItem()) {
            if (this.player.getAttackStrengthScale(0.5F) >= 1.0 && AutoAttackHandler.readyForAutoAttack()) {
                this.startAttack();
                AutoAttackHandler.resetAutoAttackDelay();
            }
        }
    }

    @Shadow
    private boolean startAttack() {
        throw new RuntimeException();
    }

    @Inject(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V", shift = At.Shift.BEFORE), cancellable = true)
    private void startAttack(CallbackInfoReturnable<Boolean> callback) {
        if (FluidCombat.CONFIG.get(ServerConfig.class).retainEnergyOnMiss) {
            // finish executing Minecraft::startAttack without calling a reset on the attack strength ticker
            this.player.swing(InteractionHand.MAIN_HAND, false);
            FluidCombat.NETWORK.sendToServer(new ServerboundSwingArmMessage(InteractionHand.MAIN_HAND));
            callback.setReturnValue(false);
        }
        if (FluidCombat.CONFIG.get(ServerConfig.class).airSweepAttack) {
            if (this.player.getAttackStrengthScale(0.5F) == 1.0F) {
                ((MultiPlayerGameModeAccessor) this.gameMode).combatnouveau$callEnsureHasSentCarriedItem();
                FluidCombat.NETWORK.sendToServer(new ServerboundSweepAttackMessage((this.player).isShiftKeyDown()));
                // possibly blocked by retainEnergyOnMiss option, we want it regardless in case of triggering a sweep attack
                this.player.resetAttackStrengthTicker();
            }
        }
        SweepAttackHelper.spawnSweepAttackEffects(player, level);
    }
}
