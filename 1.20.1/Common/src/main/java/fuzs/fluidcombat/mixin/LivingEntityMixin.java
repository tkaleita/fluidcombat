package fuzs.fluidcombat.mixin;

import fuzs.fluidcombat.FluidCombat;
import fuzs.fluidcombat.config.ServerConfig;
import fuzs.fluidcombat.helper.GuardStanceHelper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "blockedByShield", at = @At("HEAD"), cancellable = true)
    protected void blockedByShield(LivingEntity target, CallbackInfo callback) {
        if (!(target instanceof Player player)) return;

        ItemStack activeItem = player.getUseItem();
        boolean isGuardStance = GuardStanceHelper.isGuarding(player);
        boolean isActualShield = activeItem.getUseAnimation() == UseAnim.BLOCK;

        if (isGuardStance) {
            // 👉 Custom knockback or block effects for guard stance
            this.knockback(0.25, target.getX() - this.getX(), target.getZ() - this.getZ()); // lighter stagger
            GuardStanceHelper.disableGuardStance(player, 60, true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.75F, 0.9F);
            callback.cancel();
        } else if (isActualShield) {
            // 🛡 Vanilla-like knockback with config scaling
            if (FluidCombat.CONFIG.get(ServerConfig.class).shieldKnockback == ServerConfig.ShieldKnockback.NONE) return;

            double knockBackStrength;
            if (FluidCombat.CONFIG.get(ServerConfig.class).shieldKnockback == ServerConfig.ShieldKnockback.VARIABLE) {
                int variableShieldKnockbackDelay = FluidCombat.CONFIG.get(ServerConfig.class).variableShieldKnockbackDelay;
                if (!FluidCombat.CONFIG.get(ServerConfig.class).removeShieldDelay) variableShieldKnockbackDelay += 5;
                knockBackStrength = (activeItem.getUseDuration(player) - player.getUseItemRemainingTicks()) / (double) variableShieldKnockbackDelay;
                knockBackStrength = 1.0 - Mth.clamp(knockBackStrength, 0.0, 1.0);
                knockBackStrength += 0.5;
            } else {
                knockBackStrength = 0.5;
            }

            this.knockback(knockBackStrength, target.getX() - this.getX(), target.getZ() - this.getZ());
            callback.cancel();
        }
    }

    @Shadow
    public abstract void knockback(double p_147241_, double p_147242_, double p_147243_);

    @Inject(method = "isDamageSourceBlocked", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;getSourcePosition()Lnet/minecraft/world/phys/Vec3;", shift = At.Shift.BEFORE), cancellable = true)
    public void isDamageSourceBlocked(DamageSource damageSource, CallbackInfoReturnable<Boolean> callback) {
        Vec3 sourcePosition = damageSource.getSourcePosition();
        if (sourcePosition == null || damageSource.is(DamageTypeTags.IS_PROJECTILE)) return;

        Vec3 viewVector = this.getViewVector(1.0F);
        Vec3 toSource = sourcePosition.vectorTo(this.position()).normalize();
        Vec3 horizontalVector = new Vec3(toSource.x, 0.0, toSource.z);

        double protectionArc = -Math.cos(FluidCombat.CONFIG.get(ServerConfig.class).shieldProtectionArc * Math.PI * 0.5 / 180.0);
        boolean isInFrontArc = horizontalVector.dot(viewVector) < protectionArc;

        if ((Object) this instanceof Player player && (player.isBlocking() || GuardStanceHelper.canUseGuardStance(player))) {
            callback.setReturnValue(isInFrontArc);
        }
    }

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void afterHurt(DamageSource source, float amount, CallbackInfo ci) {
        this.invulnerableTime = 10; // halves invuln time so things can combo better
    }
}