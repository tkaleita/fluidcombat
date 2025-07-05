package fuzs.fluidcombat.mixin.client;

import fuzs.fluidcombat.FluidCombat;
import fuzs.fluidcombat.client.handler.AutoAttackHandler;
import fuzs.fluidcombat.helper.SweepAttackHelper;
import fuzs.fluidcombat.mixin.client.accessor.MultiPlayerGameModeAccessor;
import fuzs.fluidcombat.network.client.ServerboundSweepAttackMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;

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

    @Inject(method = "continueAttack", at = @At(value = "HEAD"))//, target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;stopDestroyBlock()V", shift = At.Shift.BEFORE))
    private void continueAttack(boolean attacking, CallbackInfo callback) {
        // do not cancel stopDestroyBlock as in combat snapshots
        // also additional check for an item being used
        HitResult hit = Minecraft.getInstance().hitResult;
        if (attacking && !this.player.isUsingItem() && (hit == null || hit.getType() != HitResult.Type.BLOCK)) {
            if (AutoAttackHandler.readyForAutoAttack(this.player)) { // now takes LocalPlayer
                this.startAttack();
            }
        }   
    }

    @Shadow
    private boolean startAttack() {
        throw new RuntimeException();
    }

    @Inject(method = "startAttack", at = @At("RETURN"))//, target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V", shift = At.Shift.BEFORE), cancellable = true)
    private void startAttack(CallbackInfoReturnable<Boolean> callback) {

        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) return;

        ((MultiPlayerGameModeAccessor) this.gameMode).combatnouveau$callEnsureHasSentCarriedItem();
        FluidCombat.NETWORK.sendToServer(new ServerboundSweepAttackMessage((this.player).isShiftKeyDown()));
        // possibly blocked by retainEnergyOnMiss option, we want it regardless in case of triggering a sweep attack
        this.player.resetAttackStrengthTicker();
        SweepAttackHelper.spawnSweepAttackEffects(player, level);
    }
}
