package fuzs.fluidcombat.mixin;

import fuzs.fluidcombat.FluidCombat;
import fuzs.fluidcombat.config.ServerConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class EntityMixin {

    @Inject(method = "getPickRadius", at = @At("HEAD"), cancellable = true)
    public void getPickRadius(CallbackInfoReturnable<Float> callback) {
        if (!FluidCombat.CONFIG.get(ServerConfig.class).inflateHitboxes) return;
        if (LivingEntity.class.isInstance(this)) {
            callback.setReturnValue(0.1F);
        }
    }
}
