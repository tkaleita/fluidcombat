package focuss.fluidcombat.mixin.client;

import focuss.fluidcombat.FluidCombat;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "swing", at = @At("HEAD"))
    private void debugSwing(InteractionHand hand, CallbackInfo ci) {
        FluidCombat.LOGGER.info("Swing called for: {}", hand);
        new Exception("Swing stacktrace").printStackTrace();
    }
}