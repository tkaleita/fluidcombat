package fuzs.fluidcombat.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import fuzs.fluidcombat.FluidCombat;
import fuzs.fluidcombat.config.ServerConfig;
import fuzs.fluidcombat.helper.BlockStanceHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class PlayerMixin extends LivingEntity {

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "hurt", at = @At(value = "RETURN", ordinal = 0), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;scalesWithDifficulty()Z")), cancellable = true)
    public void hurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        if (!FluidCombat.CONFIG.get(ServerConfig.class).weakAttacksKnockBackPlayers) return;
        if (amount == 0.0F && this.level().getDifficulty() != Difficulty.PEACEFUL) {
            callback.setReturnValue(super.hurt(source, amount));
        }

        
    }

    @Inject(method = "attack", at = @At("HEAD"))
    public void attack$0(Entity target, CallbackInfo callback, @Share("sprints_during_attack") LocalBooleanRef sprintsDuringAttack) {
        sprintsDuringAttack.set(this.isSprinting());
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V", ordinal = 0, shift = At.Shift.AFTER))
    public void attack$1(Entity target, CallbackInfo callback) {
        // allow landing critical hits when sprint jumping like before 1.9 and in combat test snapshots
        // the injection point is fine despite being inside a few conditions as the same conditions must apply for critical hits
        if (FluidCombat.CONFIG.get(ServerConfig.class).criticalHitsWhileSprinting) {
            // this disables sprinting, no need to call the dedicated method as it also updates the attribute modifier which is unnecessary since we reset the value anyway
            this.setSharedFlag(3, false);
        }
    }

    @Inject(method = "attack", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;walkDist:F"))
    public void attack$2(Entity target, CallbackInfo callback, @Share("sprints_during_attack") LocalBooleanRef sprintsDuringAttack) {
        // reset to original sprinting value for rest of attack method
        if (sprintsDuringAttack.get()) {
            this.setSharedFlag(3, true);
        }
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V", shift = At.Shift.AFTER))
    public void attack$3(Entity target, CallbackInfo callback, @Share("sprints_during_attack") LocalBooleanRef sprintsDuringAttack) {
        // don't disable sprinting when attacking a target
        // this is mainly nice to have since you always stop to swim when attacking creatures underwater
        if (FluidCombat.CONFIG.get(ServerConfig.class).sprintAttacks) {
            if (sprintsDuringAttack.get()) this.setSprinting(true);
        }
    }

    @ModifyVariable(method = "attack", at = @At("LOAD"), ordinal = 3, slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;sweepAttack()V")))
    public boolean attack$4(boolean triggerSweepAttack, Entity target) {
        return false;
        /*if (!CombatNouveau.CONFIG.get(ServerConfig.class).requireSweepingEdge) return triggerSweepAttack;
        return triggerSweepAttack && this.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) > 0.0F;*/
    }

    @Inject(method = "attack", at = @At("TAIL"))
    public void injectCustomSweep(Entity target, CallbackInfo callback) {
        /*if ((Object) this instanceof Player player) {
            SweepAttackHelper.initiateSweepAttack(player, target);
        }*/
    }

    @Inject(method = "sweepAttack", at = @At("HEAD"), cancellable = true)
    public void cancelSweepAttackParticle(CallbackInfo callback) {
        callback.cancel(); // cancel vanilla sweep attack particle from spawning
    }

    @Inject(method = "disableShield", at = @At("HEAD"), cancellable = true)
    private void onDisableShield(CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            BlockStanceHelper.disableAllGuardStances(player, 100);
        }
    }
}