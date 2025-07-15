package fuzs.fluidcombat.mixin.client;

import fuzs.fluidcombat.helper.GuardStanceHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "getAttackAnim", at = @At("TAIL"), cancellable = true)
    public void getAttackAnim(float tickDelta, CallbackInfoReturnable<Float> callback) {
        // alternative swing animation, fully disabled for now
        /*final float swingProgress = callback.getReturnValueF();
        if (swingProgress > 0.4F && swingProgress < 0.95F) {
            callback.setReturnValue(0.4F + 0.6F * (float) Math.pow((swingProgress - 0.4F) / 0.6F, 4.0));
        }*/
    }

    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void fakeBlockingForGuardStance(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player) {
            if (GuardStanceHelper.isGuarding(player)) {
                cir.setReturnValue(true);
            }
        }
    }
}