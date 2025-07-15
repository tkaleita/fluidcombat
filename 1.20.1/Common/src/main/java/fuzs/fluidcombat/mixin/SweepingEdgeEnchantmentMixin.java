package fuzs.fluidcombat.mixin;

import net.minecraft.world.item.enchantment.SweepingEdgeEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SweepingEdgeEnchantment.class)
abstract class SweepingEdgeEnchantmentMixin {

    @Inject(method = "getSweepingDamageRatio", at = @At("TAIL"), cancellable = true)
    private static void getSweepingDamageRatio(int level, CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue(callback.getReturnValueF() * 0.5F);
    }
}