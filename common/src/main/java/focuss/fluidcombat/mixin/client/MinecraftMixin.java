package focuss.fluidcombat.mixin.client;

import focuss.fluidcombat.FluidCombat;
import focuss.fluidcombat.client.handler.AutoAttackHandler;
import focuss.fluidcombat.helper.SweepAttackHelper;
import focuss.fluidcombat.mixin.client.accessor.MultiPlayerGameModeAccessor;
import focuss.fluidcombat.network.client.ServerboundSweepAttackMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

    @Inject(method = "continueAttack", at = @At(value = "HEAD"), cancellable = true)//, target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;stopDestroyBlock()V", shift = At.Shift.BEFORE))
    private void continueAttack(boolean attacking, CallbackInfo callback) {
        FluidCombat.LOGGER.info("continueAttack");

        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            // we are trying to mine a block!
            if (attacking && !this.player.isUsingItem() && AutoAttackHandler.readyForAutoAttack(this.player)) {
                fluidCombat$triggerSweep();
            }
            callback.cancel();
            return;
        }

        if (attacking && !this.player.isUsingItem() && (hit == null || hit.getType() != HitResult.Type.BLOCK)) {
            if (AutoAttackHandler.readyForAutoAttack(this.player)) {
                this.startAttack();
            }
        }
    }

    @Shadow
    private boolean startAttack() {
        throw new RuntimeException();
    }

    @Unique
    boolean fluidCombat$wasCharged = false;

    @Inject(method = "startAttack", at = @At(value = "HEAD"), cancellable = true)
    private void startAttackHead(CallbackInfoReturnable<Boolean> cir) {
        FluidCombat.LOGGER.info("startAttackHead");

        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            // we are trying to mine a block! cancel!!
            cir.setReturnValue(false);
            return;
        }

        fluidCombat$wasCharged = player.getAttackStrengthScale(0.5F) >= 1.0F;
        if (!fluidCombat$wasCharged) {
            // block the vanilla attack altogether
            cir.setReturnValue(false);
        }
    }

    /// because the sweep resets attack cooldown, this NEEDS to be injected at return. to let minecraft handle the normal attack!
    @Inject(method = "startAttack", at = @At(value = "RETURN"))//, target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V", shift = At.Shift.BEFORE), cancellable = true)
    private void startAttack(CallbackInfoReturnable<Boolean> callback) {
        FluidCombat.LOGGER.info("startAttackReturn");
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) return;

        if (!fluidCombat$wasCharged) return;
        fluidCombat$triggerSweep();
    }

    @Unique
    private void fluidCombat$triggerSweep() {
        ((MultiPlayerGameModeAccessor) this.gameMode).fluidcombat$callEnsureHasSentCarriedItem();
        FluidCombat.NETWORK.sendToServer(new ServerboundSweepAttackMessage((this.player).isShiftKeyDown()));
        // possibly blocked by retainEnergyOnMiss option, we want it regardless in case of triggering a sweep attack
        this.player.resetAttackStrengthTicker();
        SweepAttackHelper.spawnSweepAttackEffects(player, level);
        player.swing(InteractionHand.MAIN_HAND, true); // force full swing animation
    }
}