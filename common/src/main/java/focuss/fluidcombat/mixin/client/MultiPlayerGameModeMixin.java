package focuss.fluidcombat.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Inject(method = "useItem", at = @At("RETURN"), cancellable = true)
    private void fluidCombat$afterUseItem(Player player,
                                          InteractionHand hand,
                                          CallbackInfoReturnable<InteractionResult> cir) {

        if (hand != InteractionHand.MAIN_HAND) return;
        InteractionResult result = cir.getReturnValue();
        boolean didSomething = result != InteractionResult.PASS;
        boolean shouldOverride = false;
        // vanilla did nothing → safe to override
        if (!didSomething) {
            shouldOverride = true;
        }
        // vanilla did something → optionally override
        // (example: weapon right-clicks)
        if (didSomething) {
            // put your custom conditions here
            // example:
            // if (FluidCombatUtil.isWeapon(player.getMainHandItem()))
            //     shouldOverride = true;
        }

        if (shouldOverride) {
            // trigger your offhand logic here
            // fluidCombat$triggerSweep(player, EquipmentSlot.OFFHAND);

            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }


    @Inject(method = "useItemOn", at = @At("RETURN"), cancellable = true)
    private void fluidCombat$afterUseItemOn(LocalPlayer player,
                                            InteractionHand hand,
                                            BlockHitResult hit,
                                            CallbackInfoReturnable<InteractionResult> cir) {

        if (hand != InteractionHand.MAIN_HAND) return;
        InteractionResult result = cir.getReturnValue();
        boolean didSomething = result != InteractionResult.PASS;
        boolean shouldOverride = false;
        // nothing happened
        if (!didSomething) {
            shouldOverride = true;
        }
        // something happened (block interaction etc.)
        if (didSomething) {
            // optional override conditions
            // example:
            // if (FluidCombatUtil.isWeapon(player.getMainHandItem()))
            //     shouldOverride = true;
        }
        if (shouldOverride) {
            // your offhand attack
            // fluidCombat$triggerSweep(player, EquipmentSlot.OFFHAND);
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}