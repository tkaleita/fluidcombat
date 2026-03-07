package focuss.fluidcombat.mixin.client;

import focuss.fluidcombat.FluidCombat;
import focuss.fluidcombat.config.ClientConfig;
import focuss.fluidcombat.config.ServerConfig;
import focuss.fluidcombat.helper.GuardStanceHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "getAttackAnim", at = @At("TAIL"), cancellable = true)
    public void getAttackAnim(float tickDelta, CallbackInfoReturnable<Float> callback) {
        // alternative swing animation
        if (!FluidCombat.CONFIG.get(ClientConfig.class).alternateSwingAnimation) return;
        final float swingProgress = callback.getReturnValueF();
        if (swingProgress > 0.4F && swingProgress < 0.95F) {
            callback.setReturnValue(0.4F + 0.6F * (float) Math.pow((swingProgress - 0.4F) / 0.6F, 4.0));
        }
        callback.cancel();
    }

    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
    private void minSwingDuration(CallbackInfoReturnable<Integer> cir) {
        int duration = cir.getReturnValue();
        if (duration < 2) {
            cir.setReturnValue(2);
        }
    }

    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void fakeBlockingForGuardStance(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player) {
            if (GuardStanceHelper.isGuarding(player)) {
                cir.setReturnValue(true);
            }
        }
    }

    @ModifyArg(method = "knockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"), index = 0)
    private double onScaleKnockback(double originalStrength) {
        return originalStrength * FluidCombat.CONFIG.get(ServerConfig.class).entityKnockbackScale;
    }

    @ModifyConstant(method = "knockback", constant = @Constant(doubleValue = 0.4D))
    private double tweakVerticalKnockback(double original) {
        // return original * 0.5D; // for half lift
        return 0.25d;             // for zero lift
    }
}